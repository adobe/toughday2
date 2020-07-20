package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver;

import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class responsible for implementing the master election process to be executed whenever a new Master must be elected
 * in the cluster.
 */
public class MasterElection {
    private int nrDrivers;
    private Queue<Integer> candidates;
    protected static final Logger LOG = LogManager.getLogger(Engine.class);
    private static MasterElection instance = null;

    private MasterElection(int nrDrivers) {
        this.nrDrivers = nrDrivers;
        this.candidates = IntStream.rangeClosed(0, nrDrivers - 1).boxed()
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
    }

    /**
     * Returns a Singleton instance of this class. This method is not thread safe and it should not be called from
     * multiple threads.
     * @param nrDrivers : number of driver components deployed in the cluster.
     */
    public static MasterElection getInstance(int nrDrivers) {
        if (instance == null) {
            instance = new MasterElection(nrDrivers);
        }

        return instance;
    }

    /**
     * Mark the given candidate as unable to become the new master, in case the current one fails.
     * @param candidateId : id used to identify the driver component which should no be taken into consideration when
     *                    electing a new master.
     */
    public void markCandidateAsInvalid(int candidateId) {
        this.candidates.remove(candidateId);
    }

    /**
     * Return true if the given candidate should not be chosen as the new Master; false otherwise.
     */
    public boolean isCandidateInvalid(int candidateId) {
        return !this.candidates.contains(candidateId);
    }

    /**
     * Return a list with all the ids of the candidates that should not be taken into consideration when electing a new
     * master in the cluster.
     */
    public Queue<Integer> getInvalidCandidates() {
        Queue<Integer> invalidCandidates = IntStream.rangeClosed(0, nrDrivers - 1).boxed()
                .collect(Collectors.toCollection(LinkedList::new));
        invalidCandidates.removeAll(this.candidates);

        return invalidCandidates;
    }

    private void before() {
        // restore all valid candidates in case there is no option;
        if (candidates.isEmpty()) {
            resetInvalidCandidates();
        }
    }

    /**
     * Resets the list of candidates considered invalid. After this method is called, all the drivers will be considered
     * eligible to be elected as the new master.
     */
    public void resetInvalidCandidates() {
        LOG.info("Resetting list of candidates to be considered for master election");
        this.candidates = IntStream.rangeClosed(0, nrDrivers - 1).boxed().collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
    }

    /**
     * Method used for electing a new master when a Driver component is joining the cluster.
     * @param newDriver : driver instance which joined the cluster recently and which is currently not aware if there is
     *                  already a driver which plays the role of the Master.
     */
    public void electMasterWhenDriverJoinsTheCluster(Driver newDriver) {
        // check if there is already a master running in the cluster
        collectUpdatesFromAllDrivers(newDriver);
        DriverState driverState = newDriver.getDriverState();

        // if no master was detected, trigger the master election process
        driverState.getMasterIdLock().readLock().lock();
        if (driverState.getMasterId() == -1) {
            driverState.getMasterIdLock().readLock().unlock();
            electMaster(newDriver);
            return;
        }

        driverState.getMasterIdLock().readLock().unlock();
        after(newDriver);
    }

    /**
     * Method used for electing a new Master whenever the previous one fails to respond to heartbeat messages.
     * @param driver : the driver instance electing a new master.
     */
    public void electMaster(Driver driver) {
        before();

        // set new master
        LOG.info("Electing new master " + candidates.stream().min(Integer::compareTo).get());

        driver.getDriverState().getMasterIdLock().writeLock().lock();
        driver.getDriverState().setMasterId(candidates.stream().min(Integer::compareTo).get());
        driver.getDriverState().getMasterIdLock().writeLock().unlock();

        after(driver);
    }

    private List<String> getIdleAgents(Driver driver) {
        HttpUtils httpUtils = new HttpUtils();
        List<String> idleAgents = new ArrayList<>();

        driver.getDriverState().getRegisteredAgents()
            .forEach(agentIp -> {
                HttpResponse agentResponse = httpUtils.sendHttpRequest(HttpUtils.GET_METHOD, "",
                        Agent.getGetStatusPath(agentIp), HttpUtils.HTTP_REQUEST_RETRIES);
                if (agentResponse != null) {
                    try {
                        String status = EntityUtils.toString(agentResponse.getEntity());
                        if (status.equals(Agent.Status.IDLE.toString())) {
                            LOG.info("Agent " + agentIp + " is idle.");
                            idleAgents.add(agentIp);
                        } else if (status.equals(Agent.Status.PHASE_COMPLETED.toString())) {
                            LOG.info(("Agent " + agentIp + " finished executing the current phase."));
                            driver.getDistributedPhaseMonitor().registerAgentRunningTD(agentIp);
                            driver.getDistributedPhaseMonitor().addAgentWhichCompletedTheCurrentPhase(agentIp);
                        } else {
                            LOG.info("Agent " + agentIp + " is running TD.");
                            driver.getDistributedPhaseMonitor().registerAgentRunningTD(agentIp);
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not check status of agent. Current phase might not be executed with the "
                                + "desired configuration.");
                    }
                }
            });

        return idleAgents;
    }

    private void after(Driver driver) {
        // cancel heartbeat task for the diver elected as the new master
        driver.getDriverState().getMasterIdLock().readLock().lock();

        // running as master
        if (driver.getDriverState().getMasterId() == driver.getDriverState().getId()) {
            LOG.info("Running as MASTER");

            driver.getDriverState().setCurrentState(DriverState.Role.MASTER);
            LOG.info("Stopping master heartbeat periodic task since this driver was elected as master.");
            driver.cancelMasterHeartBeatTask();

            /* check if work redistribution is required */
            List<String> idleAgents = getIdleAgents(driver);

            /* the assumption is that all the registered agents are currently executing TD test or they've completed
             * the execution of the current phase
             */
            if (driver.getConfiguration() != null && !idleAgents.isEmpty()) {
                LOG.info("New master must redistribute the work between the agents because of idle agents " +
                        idleAgents.toString());
                idleAgents.forEach(idleAgent ->
                        driver.getTaskBalancer().scheduleWorkRedistributionProcess(driver, idleAgent, true));
            }

            // schedule heartbeat task for periodically checking agents
            LOG.info("Scheduling heartbeat task for monitoring agents...");
            driver.scheduleHeartbeatTask();

            // resume execution
            driver.getExecutorService().submit(driver::resumeExecution);
        } else {
            LOG.info("Running as CANDIDATE");

            // running as candidate
            driver.getDriverState().setCurrentState(DriverState.Role.CANDIDATE);

            // schedule heartbeat task to periodically monitor the agents
            driver.scheduleMasterHeartbeatTask();
        }

        driver.getDriverState().getMasterIdLock().readLock().unlock();
    }

    private void processUpdatesFromDriver(Driver currentDriver, String yamlUpdates) {
        LOG.info("Current driver " + currentDriver.getDriverState().getId() + " is processing updates:\n" + yamlUpdates);
        ObjectMapper objectMapper = new ObjectMapper();
        DriverUpdateInfo updates = null;

        try {
            updates = objectMapper.readValue(yamlUpdates, DriverUpdateInfo.class);
        } catch (IOException e) {
            LOG.info("Unable to process updates about the cluster state. Driver will restart now...");
            System.exit(-1);
        }

        currentDriver.getDriverState().updateDriverState(updates, currentDriver)
        ;

        // set master if updates were received from the current master running in the cluster
        if (updates.getRole() == DriverState.Role.MASTER) {
            LOG.info("Received instructions that " + currentDriver.getDriverState().getPathForId(updates.getDriverId()) + "is the " +
                    "current master running in the cluster.");
            currentDriver.getDriverState().getMasterIdLock().writeLock().lock();
            currentDriver.getDriverState().setMasterId(updates.getDriverId());
            currentDriver.getDriverState().getMasterIdLock().writeLock().unlock();
        }
    }

    private void collectUpdatesFromAllDrivers(Driver currentDriver) {
        HttpUtils httpUtils = new HttpUtils();
        List<Integer> ids = IntStream.rangeClosed(0, nrDrivers - 1).boxed().collect(Collectors.toList());

        // send request to all drivers, except the current one
        List<String> paths = ids.stream()
                .filter(id -> id != currentDriver.getDriverState().getId())
                .map(id -> currentDriver.getDriverState().getPathForId(id))
                .map(Driver::getAskForUpdatesPath)
                .collect(Collectors.toList());

        for (String URI : paths) {
            HttpResponse driverResponse = httpUtils.sendHttpRequest(HttpUtils.GET_METHOD, "", URI,
                    HttpUtils.HTTP_REQUEST_RETRIES);
            if (driverResponse != null) {
                try {
                    String yamlUpdates = EntityUtils.toString(driverResponse.getEntity());
                    LOG.info("Received updates: " + yamlUpdates);

                    processUpdatesFromDriver(currentDriver, yamlUpdates);

                    // if updates were received from the master -> finish process
                    if (currentDriver.getDriverState().getMasterId() != -1) {
                        break;
                    }
                } catch (IOException e) {
                    LOG.info("Unable to process updates about the cluster state. Driver will restart now...");
                    System.exit(-1);
                }
            }
        }
    }

}
