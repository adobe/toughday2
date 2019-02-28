package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Class responsible for encapsulating all the information required for describing the state of a Driver component.
 */
public class DriverState {
    // Documentation says that each service has the following DNS A record: my-svc.my-namespace.svc.cluster.local
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    private final String hostname;
    private static final String SVC_NAME = "driver";
    private final Queue<String> agents = new ConcurrentLinkedQueue<>();
    private final int id;
    private int masterId = -1;
    private int nrDrivers;
    private Configuration driverConfig;
    private Role currentState;
    private final ReadWriteLock masterIdLock = new ReentrantReadWriteLock();

    public enum Role {
        CANDIDATE,
        MASTER
    }

    /**
     * Returns the URI to be used for identifying a driver component using its id.
     * @param id : the unique identifier associated with the driver component.
     */
    public String getPathForId(int id) {
        return SVC_NAME + "-" + id + "." + SVC_NAME + "." +
                this.driverConfig.getDistributedConfig().getClusterNamespace() + ".svc.cluster.local";
    }

    /**
     * Returns the lock used for synchronised access to the id of the driver component considered to be the Master.
     */
    public ReadWriteLock getMasterIdLock() {
        return this.masterIdLock;
    }

    /**
     * Constructor.
     * @param hostname : hostname used to access the driver service inside the cluster.
     * @param driverConfig : TD configuration used for deploying the driver.
     */
    public DriverState(String hostname, Configuration driverConfig) {
        this.driverConfig = driverConfig;
        this.hostname = hostname;

        String name_pattern = "driver-[0-9]+\\." + SVC_NAME + "\\." + this.driverConfig.getDistributedConfig().getClusterNamespace() +
                "\\.svc\\.cluster\\.local";

        if (!Pattern.matches(name_pattern, hostname)) {
            throw new IllegalStateException("Driver's name should respect the following format: driver-<id>.<svc_name>.<namespace>.svc.cluster.local");
        }

        String podName = hostname.substring(0, hostname.indexOf("." + SVC_NAME + "." +
                this.driverConfig.getDistributedConfig().getClusterNamespace()));

        this.id = Integer.parseInt(podName.substring(podName.lastIndexOf("-") + 1));
        this.nrDrivers = Integer.parseInt(System.getenv("NR_DRIVERS"));
        this.currentState = Role.CANDIDATE;
    }

    /**
     * Getter for the hostname.
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Getter for the id associated with the driver.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Getter for the id of the driver considered to be the current master.
     */
    public int getMasterId() {
        return this.masterId;
    }

    /**
     * Getter for the number of driver components deployed in the cluster.
     */
    public int getNrDrivers() {
        return this.nrDrivers;
    }

    /**
     * Returns the list of active agents running in the cluster.
     */
    public Queue<String> getRegisteredAgents() {
        return this.agents;
    }

    /**
     * Adds the agent to the list of agents running in the cluster.
     * @param agentIdentifier : ipAddress used to uniquely identify the agent inside the cluster.
     */
    public void registerAgent(String agentIdentifier) {
        if (!this.agents.contains(agentIdentifier)) {
            this.agents.add(agentIdentifier);
        }
    }

    /**
     * Removes an agent from the list of active agents running in the cluster.
     * @param agentIdentifier : ipAddress that uniquely identifies the agent to be removed.
     */
    public void removeAgent(String agentIdentifier) {
        this.agents.remove(agentIdentifier);
    }

    /**
     * Getter for the TD configuration of the driver component.
     */
    public Configuration getDriverConfig() {
        return this.driverConfig;
    }

    /**
     * Getter for the current status of the driver component. (Master or Candidate)
     */
    public Role getCurrentState() {
        return this.currentState;
    }

    /**
     * Setter the current status of the driver component.
     */
    public void setCurrentState(Role role) {
        this.currentState = role;
    }

    /**
     * Setter for the id of the driver running as master in the cluster.
     */
    public void setMasterId(int id) {
        this.masterId = id;
    }

    /**
     * Method used for updating the state of the cluster when information is received from another drivers. This method
     * is called usually when a new driver joins the cluster and needs to know the current state of the execution (for
     * example how many active agents are registered in the cluster)
     * @param updates : information received from other drivers running in the cluster
     * @param driverInstance : the Driver instance which received the updates.
     */
    public void updateDriverState(DriverUpdateInfo updates, Driver driverInstance) {
        // excludes candidates which are not allowed to become master
        updates.getInvalidCandidates().forEach(driverInstance.getMasterElection()::markCandidateAsInvalid);

        // add all agents
        updates.getRegisteredAgents().forEach(driverInstance.getDriverState()::registerAgent);
        LOG.info("After processing update instructions, registered agents is " +
                driverInstance.getDriverState().getRegisteredAgents().toString());
        LOG.info("After processing update instructions, invalid candidates are: " +
                updates.getInvalidCandidates().toString());

        // build TD configuration to be executed distributed
        if (!updates.getYamlConfig().isEmpty() && driverInstance.getConfiguration() == null) {
            LOG.info("Building configuration received from the other drivers running in the cluster.");
            try {
                Configuration configuration = new Configuration(updates.getYamlConfig());
                driverInstance.setConfiguration(configuration);
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IOException |
                    IllegalAccessException e) {
                /* if driver can't build the configuration executed distributed it will not be able to become the master
                 * and to coordinate the entire execution
                 */
                LOG.warn("Exception occurred when building ToughDay configuration to be executed distributed. Driver" +
                        " will leave the cluster");
                System.exit(-1);
            }

            // delete phases that were previously executed
            List<Phase> phases = driverInstance.getConfiguration().getPhases();
            List<Phase> previouslyExecutedPhases = new ArrayList<>();
            for (Phase phase : phases) {
                if (phase.getName().equals(updates.getCurrentPhaseName())) {
                    break;
                }

                previouslyExecutedPhases.add(phase);
            }
            LOG.info("phases that will be removed " + previouslyExecutedPhases.toString());
            phases.removeAll(previouslyExecutedPhases);

            // set current phase to be monitored
            driverInstance.getDistributedPhaseMonitor().setPhase(phases.get(0));
            LOG.info("Current phase being executed is " + driverInstance.getDistributedPhaseMonitor().getPhase().getName());
        }
    }
}