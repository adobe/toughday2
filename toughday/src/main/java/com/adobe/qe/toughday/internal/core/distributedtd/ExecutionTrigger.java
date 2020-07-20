package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.GenerateYamlConfiguration;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Driver;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;


/**
 * Class responsible for sending a request to the driver component running in the cluster. This
 * request will trigger the distributed execution of TD.
 * */
public class ExecutionTrigger {

    protected static final Logger LOG = LogManager.getLogger(ExecutionTrigger.class);
    private static final String DEFAULT_CLUSTER_PORT = "80";

    private final Configuration configuration;
    private final String executionPath;

    public ExecutionTrigger(Configuration configuration) {
        this.configuration = configuration;
        // sanity check
        if (configuration.getDistributedConfig().getDriverIp() == null || configuration.getDistributedConfig().getDriverIp().isEmpty()) {
            throw new IllegalStateException("The public ip address at which the driver's service is accessible " +
                    " is required when running TD in distributed mode.");
        }

        this.executionPath = HttpUtils.URL_PREFIX + configuration.getDistributedConfig().getDriverIp() + ":" + DEFAULT_CLUSTER_PORT
                + Driver.EXECUTION_PATH;
    }

    /**
     * Method used for triggering the execution of TD in distributed mode.
     */
    public void triggerDistributedExecution() {
        GenerateYamlConfiguration generateYaml = new GenerateYamlConfiguration(this.configuration.getConfigParams(), new HashMap<>());
        String yamlConfig = generateYaml.createYamlStringRepresentation();
        HttpUtils httpUtils = new HttpUtils();

        HttpResponse response = httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, yamlConfig, executionPath, 3);
        if (response == null) {
            LOG.warn("TD execution request could not be sent to driver. Make sure that driver is up" +
                    " and ready to process requests.");
        }
    }
}
