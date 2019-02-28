package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.GenerateYamlConfiguration;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverUpdateInfo;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.MasterElection;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.HTTP_REQUEST_RETRIES;

/**
 * Base implementation for the RequestProcessor interface. Implements the common actions that should be taken by both
 * driver components (master and candidates) when receiving certain types of HTTP requests.
 */
public abstract class AbstractRequestProcessor implements RequestProcessor {
    protected final HttpUtils httpUtils = new HttpUtils();
    protected static final Logger LOG = LogManager.getLogger(Engine.class);
    protected Driver driverInstance;

    /**
     * Constructor.
     * @param driverInstance : the driver Instance which will receive the http requests to be processed.
     */
    public AbstractRequestProcessor(Driver driverInstance) {
        this.driverInstance = driverInstance;
    }

    /**
     * Returns a list containing the URIs that should be used for forwarding a request to all the other drivers running
     * in the cluster.
     * @param driverInstance : the driver instance that must forward the request.
     */
    protected List<String> getDriverPathsForRedirectingRequests(Driver driverInstance) {
        return IntStream.rangeClosed(0, driverInstance.getDriverState().getNrDrivers() - 1).boxed()
                .filter(id -> id != driverInstance.getDriverState().getId()) // exclude current driver
                .map(id -> driverInstance.getDriverState().getPathForId(id))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public String processRegisterRequest(Request request, Driver driverInstance) {
        String agentIp = request.body();
        DriverState driverState = this.driverInstance.getDriverState();

        if (request.queryParams("forward").equals("true")) {
            /* register new agents to all the drivers running in the cluster */
            for (int i = 0; i < driverState.getNrDrivers(); i++) {
                /* skip current driver */
                if (i == driverState.getId()) {
                    continue;
                }

                LOG.info(this.driverInstance.getDriverState().getHostname() + ": sending agent register request for agent " + agentIp + "" +
                        "to driver " + this.driverInstance.getDriverState().getPathForId(i));
                HttpResponse regResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, agentIp,
                        Driver.getAgentRegisterPath(driverState.getPathForId(i), HttpUtils.SPARK_PORT, false), HTTP_REQUEST_RETRIES);
                if (regResponse == null) {
                    // the assumption is that the new driver will receive the full list of active agents after being restarted
                    LOG.info("Driver " + driverState.getHostname() + "failed to send register request for agent " + agentIp +
                            "to driver " + driverState.getPathForId(i));
                }
            }
        }

        return "";
    }

    @Override
    public String processUpdatesRequest(Request request, Driver driverInstance) throws JsonProcessingException {
        LOG.info("Driver has requested updates about the state of the cluster.");
        String currentPhaseName = "";
        String yamlConfig = "";
        DriverState driverState = this.driverInstance.getDriverState();

        /* send configuration received to be executed in distributed mode and the phase being executed at this moment,
         * if applicable.
         */
        if (driverInstance.getConfiguration() != null) {
            GenerateYamlConfiguration generateYaml =
                    new GenerateYamlConfiguration(driverInstance.getConfiguration().getConfigParams(), new HashMap<>());
            yamlConfig = generateYaml.createYamlStringRepresentation();
            if (driverInstance.getDistributedPhaseMonitor().isPhaseExecuting()) {
                currentPhaseName = this.driverInstance.getDistributedPhaseMonitor().getPhase().getName();
            }
        }

        // build information to send to the driver that recently joined the cluster
        DriverUpdateInfo driverUpdateInfo = new DriverUpdateInfo(driverState.getId(),
                driverState.getCurrentState(), this.driverInstance.getMasterElection().getInvalidCandidates(),
                driverState.getRegisteredAgents(), yamlConfig, currentPhaseName);

        ObjectMapper objectMapper = new ObjectMapper();
        String yamlUpdateInfo = objectMapper.writeValueAsString(driverUpdateInfo);
        LOG.info("Create YAML update info: " + yamlUpdateInfo);

        // set response
        return yamlUpdateInfo;
    }

    @Override
    public String processMasterElectionRequest(Request request, Driver driverInstance) {
        int failedDriverId = Integer.parseInt(request.body());
        MasterElection masterElection = this.driverInstance.getMasterElection();
        // check if this news was already processed
        if (masterElection.isCandidateInvalid(failedDriverId)) {
            return "";
        }

        LOG.info("Driver was informed that the current master (id: " + failedDriverId + ") died");
        masterElection.markCandidateAsInvalid(failedDriverId);

        // pick a new leader
        masterElection.electMaster(driverInstance);
        LOG.info("New master was elected: " + this.driverInstance.getDriverState().getMasterId());

        return "";
    }

    @Override
    public String processPhaseCompletionAnnouncement(Request request) {
        String agentIp = request.body();
        DriverState driverState = this.driverInstance.getDriverState();

        LOG.info("Agent " + agentIp + " finished executing the current phase.");
        this.driverInstance.getDistributedPhaseMonitor().addAgentWhichCompletedTheCurrentPhase(agentIp);

        /* if this is the first driver receiving this type of request, forward it to all the other drivers running in
         * the cluster.
         */
        if (request.queryParams("forward").equals("true")) {
            for (int i = 0; i < driverState.getNrDrivers(); i++) {
                /* skip current driver and inactive drivers */
                if (i == driverState.getId()) {
                    continue;
                }

                LOG.info(driverState.getHostname() + ": sending agent announcement for phase completion " +
                        agentIp + "" + "to driver " + driverState.getPathForId(i));
                HttpResponse response = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, agentIp,
                        Driver.getPhaseFinishedByAgentPath(driverState.getPathForId(i), HttpUtils.SPARK_PORT, false),
                        HTTP_REQUEST_RETRIES);

                if (response == null) {
                    // the assumption is that the new driver will receive the full list of active agents after being restarted
                    LOG.info("Driver " + driverState.getHostname() + "failed to send announcement for phase "
                            + "of agent " + agentIp + "to driver " + driverState.getPathForId(i));
                }

            }

        }

        // TODO: update current phase for stand-by drivers if the phase was successfully finished
        return "";
    }


    @Override
    public String processExecutionRequest(Request request, Driver driverInstance) throws Exception {
        String yamlConfiguration = request.body();
        LOG.info("Received execution request for TD configuration:\n");
        LOG.info(yamlConfiguration);

        // save TD configuration which must be executed in distributed mode
        driverInstance.setConfiguration(new Configuration(yamlConfiguration));

        // send TD configuration to all the other drivers running in the cluster
        if (request.queryParams("forward").equals("true")) {
            List<String> forwardPaths = this.getDriverPathsForRedirectingRequests(driverInstance);
            forwardPaths.forEach(forwardPath -> {
                LOG.info("Forwarding execution request to driver " + forwardPath);
                HttpResponse driverResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, request.body(),
                        Driver.getExecutionPath(forwardPath, HttpUtils.SPARK_PORT, false), HttpUtils.HTTP_REQUEST_RETRIES);
                if (driverResponse == null) {
                    /* the assumption is that the driver will fail to respond to heartbeat request and will receive this
                     * information after being restarted.
                     */
                    LOG.warn("Unable to forward execution request to " + forwardPath);
                }
            });
        }

        return "";
    }
}
