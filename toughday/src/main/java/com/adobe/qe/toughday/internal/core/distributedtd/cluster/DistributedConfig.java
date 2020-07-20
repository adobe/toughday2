package com.adobe.qe.toughday.internal.core.distributedtd.cluster;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;

/**
 * Contains all the configurable arguments when running TD distributed.
 */
public class DistributedConfig {
    private static final String DEFAULT_HEARTBEAT_INTERVAL = "5s";
    private static final String DEFAULT_REDISTRIBUTION_WAIT_TIME = "3s";

    private boolean agent = false;
    private boolean driver = false;
    private String driverIp = null;
    private String heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private String redistributionWaitTime = DEFAULT_REDISTRIBUTION_WAIT_TIME;

    @ConfigArgSet(required = false, desc = "The public ip address of the cluster. The driver" +
            " service must be accessible at this address. This property is required when running in distributed mode.")
    public void setDriverIp(String driverIp) {
        this.driverIp = driverIp;
    }

    @ConfigArgGet
    public String getDriverIp() {
        return this.driverIp;
    }

    @ConfigArgGet
    public boolean getAgent() { return this.agent; }

    @ConfigArgSet(required = false, defaultValue = "false", desc = "If true, TD runs as a cluster agent, waiting to receive" +
            " a task from the driver.")
    public void setAgent(String agent) {
        this.agent = Boolean.parseBoolean(agent);
    }

    @ConfigArgGet
    public boolean getDriver() {
        return this.driver;
    }

    @ConfigArgSet(required = false, defaultValue = "false", desc = "If true, TD runs as a driver in the cluster," +
            " distributing the work between the agents.")
    public void setDriver(String driver) {
        this.driver = Boolean.parseBoolean(driver);
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_HEARTBEAT_INTERVAL, desc = "Period of time for sending " +
            "heartbeat messages to the agents in the cluster.")
    public void setHeartbeatInterval(String heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @ConfigArgGet
    public String getHeartbeatInterval() {
        return this.heartbeatInterval;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_REDISTRIBUTION_WAIT_TIME, desc = "The minimum amount of time " +
            " to wait before scheduling work redistribution if required.")
    public void setRedistributionWaitTime(String redistributionWaitTime) {
        this.redistributionWaitTime = redistributionWaitTime;
    }

    @ConfigArgGet
    public String getRedistributionWaitTime() {
        return this.redistributionWaitTime;
    }

    public long getRedistributionWaitTimeInSeconds() {
        return GlobalArgs.parseDurationToSeconds(this.redistributionWaitTime);
    }

    public long getHeartbeatIntervalInSeconds() {
        return GlobalArgs.parseDurationToSeconds(this.heartbeatInterval);
    }

    public void merge(DistributedConfig other) {
        this.setHeartbeatInterval(other.getHeartbeatInterval());
        this.setRedistributionWaitTime(other.redistributionWaitTime);
    }
}
