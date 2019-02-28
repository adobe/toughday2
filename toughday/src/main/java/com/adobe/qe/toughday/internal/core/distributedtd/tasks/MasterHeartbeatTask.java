package com.adobe.qe.toughday.internal.core.distributedtd.tasks;

import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for implementing the heartbeat protocol for monitoring the state of the Master running in the cluster.
 */
public class MasterHeartbeatTask implements Runnable {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    private final Driver driver;
    private final HttpUtils httpUtils = new HttpUtils();

    /**
     * Constructor.
     * @param driver : the Driver instance which will execute this task periodically.
     */
    public MasterHeartbeatTask(Driver driver) {
        this.driver = driver;
    }

    private void announceDriversThatMasterDied() {
        LOG.info("Starting to announce drivers that the current master just died.");

        for (int i = 0; i < this.driver.getDriverState().getNrDrivers(); i++) {
            // skip current driver and the former master(which just died)
            if (i == this.driver.getDriverState().getId() || i == this.driver.getDriverState().getMasterId()) {
                continue;
            }

            String hostname = this.driver.getDriverState().getPathForId(i);
            HttpResponse driverResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD,
                    String.valueOf(this.driver.getDriverState().getMasterId()), Driver.getMasterElectionPath(hostname, HttpUtils.SPARK_PORT),
                    HttpUtils.HTTP_REQUEST_RETRIES);

            if (driverResponse == null) {
                /* the assumption is that the driver failed to respond to heartbeat request and he will receive the
                 * updates after rejoining the cluster.
                 */
                LOG.warn("Failed to announce driver " + hostname + " that the master failed.");
            }

            LOG.info("Successfully announced driver " + hostname + " that the current master died.");
        }

    }

    /* Function responsible for determining if the number of tests left to executed by the agents must be updated. This
     * operation is required whenever the work is rebalanced by the master.
     */
    private void checkIfCountsPerTestMustBeUpdated(Map<String, Map<String, Double>> masterData) {
        Map<String, Map<String, Long>> executions = driver.getDistributedPhaseMonitor().getExecutions();

        executions.forEach((testName, execPerAgent) -> {
            execPerAgent.forEach((agentIp, counts) -> {
                // check if one agent is no longer running TD tests
                if (!masterData.get(testName).containsKey(agentIp)) {
                    LOG.info("Counts must be updated because one agent is no longer running TD tests.");
                    this.driver.getDistributedPhaseMonitor().updateCountPerTest();
                    this.driver.getDistributedPhaseMonitor().resetExecutions();
                    return;
                }

                // check if work was rebalanced and executions must be reset
                if (masterData.get(testName).get(agentIp).longValue() < counts) {
                    LOG.info("Work was rebalanced so counts must be updated");
                    this.driver.getDistributedPhaseMonitor().updateCountPerTest();
                    this.driver.getDistributedPhaseMonitor().resetExecutions();
                    return;
                }
            });
        });

    }

    @Override
    public void run() {
        DriverState driverState = this.driver.getDriverState();
        Gson gson = new Gson();

        LOG.info(driverState.getHostname() + ": sending heartbeat message to master: " +
                Driver.getHeartbeatPath(driverState.getPathForId(driverState.getMasterId())));

        /* acquire read lock to prevent modification of the current master when receiving information from a different
        * driver while the current one is informing others to trigger the master election process.
        * */

        this.driver.getDriverState().getMasterIdLock().readLock().lock();
        HttpResponse driverResponse = this.httpUtils.sendHttpRequest(HttpUtils.GET_METHOD, "",
                Driver.getHeartbeatPath(driverState.getPathForId(driverState.getMasterId())),
                HttpUtils.HTTP_REQUEST_RETRIES);

        if (driverResponse != null) {
            this.driver.getDriverState().getMasterIdLock().readLock().unlock();

            /* process the information received from the master regarding the number of tests that were executed by the
             * agents running TD.
             */

            try {
                String jsonExecutions = EntityUtils.toString(driverResponse.getEntity());
                Map<String, Map<String, Double>> executions = gson.fromJson(jsonExecutions, Map.class);

                LOG.info("Received execution state from master " + executions.toString());
                checkIfCountsPerTestMustBeUpdated(executions);

                executions.forEach((testName, executionsPerAgent) -> {
                    Map<String, Long> execPerAgent = new HashMap<>();
                    executionsPerAgent.forEach((agentIp, counts) -> execPerAgent.put(agentIp, counts.longValue()));
                    this.driver.getDistributedPhaseMonitor().getExecutions().put(testName, execPerAgent);
                });

                LOG.info("Successfully updated executions/agent");

            } catch (IOException e) {
                LOG.info("Could not process the updates from the driver regarding the number of executions/test. " +
                        "Count property will no longer be respected if the current master dies.");
            }
            return;
        }

        LOG.info(driverState.getHostname() + ": master failed to respond to heartbeat message. Master " +
                "election process will be triggered soon.");
        /* mark candidate as inactive */
        this.driver.getMasterElection().markCandidateAsInvalid(driverState.getMasterId());

        /* announce all drivers that the current master died */
        announceDriversThatMasterDied();

        this.driver.getDriverState().getMasterIdLock().readLock().unlock();
        /* elect a new master */
        this.driver.getMasterElection().electMaster(driver);
    }
}