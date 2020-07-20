/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.internal.core.config;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.metrics.Name;
import com.adobe.qe.toughday.metrics.Timestamp;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Class for global arguments.
 */
public class GlobalArgs implements com.adobe.qe.toughday.api.core.config.GlobalArgs {
    private static final Logger LOGGER = LogManager.getLogger(GlobalArgs.class);

    public static final String DEFAULT_DURATION = "1d";
    public static final String DEFAULT_USER = "admin";
    public static final String DEFAULT_PASSWORD = "admin";
    public static final String DEFAULT_PORT_STRING = "4502";
    public static final String DEFAULT_PROTOCOL = "http";
    public static final int DEFAULT_PORT = Integer.parseInt(DEFAULT_PORT_STRING);
    public static final String DEFAULT_TIMEOUT_STRING = "180"; // 3 minutes
    public static final long DEFAULT_TIMEOUT = 3 * 60 * 1000l; // 5 minutes
    public static final String DEFAULT_LOG_LEVEL_STRING = "INFO";
    public static final Level DEFAULT_LOG_LEVEL = Level.valueOf(DEFAULT_LOG_LEVEL_STRING);
    public static final String DEFAULT_DRY_RUN = "false";
    public static final String DEFAULT_SAVE_CONFIG = "true";
    public static final String DEFAULT_LOG_PATH = ".";
    private String host;
    private int port;
    private String user;
    private String password;
    private long duration;
    private Map<String, Publisher> publishers;
    private Map<String, Metric> metrics;
    private long timeout;
    private String protocol;
    private String authMethod;
    private boolean installSampleContent = true;
    private String contextPath;
    private Level logLevel = DEFAULT_LOG_LEVEL;
    private boolean dryRun = Boolean.parseBoolean(DEFAULT_DRY_RUN);
    private boolean saveConfig = Boolean.parseBoolean(DEFAULT_SAVE_CONFIG);
    private boolean showSteps = false;
    private boolean hostValidationEnabled = true;
    private String logPath;

    /**
     * Constructor
     */
    public GlobalArgs() {
        this.publishers = new HashMap<>();
        this.metrics = new LinkedHashMap<>();
        this.port = DEFAULT_PORT;
        this.user = DEFAULT_USER;
        this.password = DEFAULT_PASSWORD;
        this.duration = parseDurationToSeconds(DEFAULT_DURATION);
        this.timeout = DEFAULT_TIMEOUT;
        this.protocol = DEFAULT_PROTOCOL;
    }

    // Global config args

    private static long unitToSeconds(char unit) {
        long factor = 1;
        // there are no breaks after case, so unitToSeconds('d') will return 1 * 24 * 60 * 60 * 1
        switch (unit) {
            case 'd':
                factor *= 24;
            case 'h':
                factor *= 60;
            case 'm':
                factor *= 60;
            case 's':
                factor *= 1;
                break;
            default:
                throw new IllegalArgumentException("Unknown duration unit: " + unit);
        }
        return factor;
    }

    /**
     * Parses a duration specified as string and converts it to seconds.
     *
     * @param duration a duration in d(ays), h(ours), m(inutes), s(econds). Ex. 1d12h30m30s
     * @return number of seconds for the respective duration.
     */
    public static long parseDurationToSeconds(String duration) {
        long finalDuration = 0l;
        long intermDuration = 0l;

        if (duration.matches("^[0-9]+$")) {
            throw new IllegalArgumentException("Time unit is not specified");
        }

        for (char c : duration.toCharArray()) {
            if (Character.isDigit(c)) {
                intermDuration = intermDuration * 10 + (long) (c - '0');
            } else {
                finalDuration += intermDuration * unitToSeconds(c);
                intermDuration = 0;
            }
            // everything else, like whitespaces is ignored
        }
        return finalDuration;
    }

    @Deprecated
    public void updatePublisherName(String oldName, String newName) {
        publishers.put(newName, publishers.remove(oldName));
    }

    @Deprecated
    public void updateMetricName(String oldName, String newName) {
        metrics.put(newName, metrics.remove(oldName));
    }

    @Deprecated
    public void addMetric(Metric metric) {
        if (metrics.containsKey(metric.getName())) {
            LOGGER.warn("A metric with this name was already added. Only the last one is taken into consideration.");
        }
        metrics.put(metric.getName(), metric);
    }

    @Deprecated
    public void addPublisher(Publisher publisher) {
        if (publishers.containsKey(publisher.getName())) {
            throw new IllegalStateException("There is already a publisher named \"" + publisher.getName() + "\"." +
                    "Please provide a different name using the \"name\" property.");
        }
        publishers.put(publisher.getName(), publisher);
    }

    @Deprecated
    public Publisher getPublisher(String publisherName) {
        if (!publishers.containsKey(publisherName)) {
            throw new IllegalStateException("Could not find a publisher with the name \"" + publisherName + "\" to configure it.");
        }
        return publishers.get(publisherName);
    }

    @Deprecated
    public Metric getMetric(String metricName) {
        if (!metrics.containsKey(metricName)) {
            throw new IllegalStateException("Could not find a metric with the name \"" + metricName + "\" to configure it.");
        }
        return metrics.get(metricName);
    }

    @Deprecated
    public boolean containsPublisher(String publisherName) {
        return publishers.containsKey(publisherName);
    }

    @Deprecated
    public boolean containsMetric(String metricName) { return metrics.containsKey(metricName); }

    @Deprecated
    public void removePublisher(String publisherName) {
        Publisher publisher = publishers.remove(publisherName);
        if (publisher == null) {
            throw new IllegalStateException("Could not exclude publisher \"" + publisherName + "\", because there was no publisher configured with that name");
        }
    }

    @Deprecated
    public void removeMetric(String metricName) {
        Metric metric = metrics.remove(metricName);
        if (metric == null) {
            throw new IllegalStateException("Could not exclude metric \"" + metricName + "\", because there was no metric configured with that name");
        }
    }

    @ConfigArgGet
    public long getDuration() {
        return duration;
    }

    @ConfigArgSet(required = false, desc = "How long the tests will run. Can be expressed in s(econds), m(inutes), h(ours), and/or d(ays). Example: 1h30m.", defaultValue = DEFAULT_DURATION, order = 6)
    public void setDuration(String durationString) {
        this.duration = parseDurationToSeconds(durationString);
    }

    /**
     * Returns a list with all the metrics that are going to be published.
     * @return
     */
    @Deprecated
    public Collection<Metric> getMetrics() {

        Collection<Metric> requiredMetrics = new ArrayList<>();

        //add mandatory metrics
        requiredMetrics.add(new Name());
        requiredMetrics.add(new Timestamp());

        requiredMetrics.addAll(metrics.values());
        return requiredMetrics;
    }

    @Deprecated
    public Collection<Publisher> getPublishers() {
        return publishers.values();
    }

    // Adders and getters

    @ConfigArgGet
    public long getTimeout() {
        return timeout / 1000;
    }

    public long getTimeoutInSeconds() {
        return this.timeout;
    }

    @ConfigArgSet(required = false, desc = "How long a test will run before it will be interrupted and marked as failed. Expressed in seconds",
            defaultValue = DEFAULT_TIMEOUT_STRING, order = 7)
    public void setTimeout(String timeout) {
        this.timeout = Integer.parseInt(timeout) * 1000;
    }

    @ConfigArgGet
    public String getHost() {
        return host;
    }

    @ConfigArgSet(required = true, desc = "The host name/ip which will be targeted", order = 1)
    public void setHost(String host) {
        this.host = host;
    }

    @ConfigArgGet
    public int getPort() {
        return port;
    }

    @ConfigArgSet(required = false, desc = "The port of the host", defaultValue = DEFAULT_PORT_STRING, order = 2)
    public void setPort(String port) {
        this.port = Integer.parseInt(port);
    }

    @ConfigArgGet
    public String getUser() {
        return user;
    }

    @ConfigArgSet(required = false, desc = "User name for the instance", defaultValue = DEFAULT_USER, order = 3)
    public void setUser(String user) {
        this.user = user;
    }

    @ConfigArgGet
    public String getPassword() {
        return password;
    }

    @ConfigArgSet(required = false, desc = "Password for the given user", defaultValue = DEFAULT_PASSWORD, order = 4)
    public void setPassword(String password) {
        this.password = password;
    }

    @ConfigArgGet
    public String getProtocol() {
        return protocol;
    }

    @ConfigArgSet(required = false, desc = "Authentication Method", defaultValue = "basic")
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    @ConfigArgGet
    public String getAuthMethod() {
        return this.authMethod;
    }

    @ConfigArgSet(required = false, desc = "What type of protocol to use for the host", defaultValue = DEFAULT_PROTOCOL)
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @ConfigArgGet
    public boolean getInstallSampleContent() {
        return installSampleContent;
    }

    @ConfigArgSet(required = false, desc = "Install ToughDay 2 Sample Content.", defaultValue = "true")
    public void setInstallSampleContent(String installSampleContent) {
        this.installSampleContent = Boolean.valueOf(installSampleContent);
    }

    @ConfigArgSet(required = false, desc = "Enable/Disable Host Validation", defaultValue = "true")
    public void setHostValidationEnabled(String hostValidationEnabled) {
        this.hostValidationEnabled = Boolean.valueOf(hostValidationEnabled);
    }

    @ConfigArgGet
    public String getContextPath() {
        return this.contextPath;
    }

    @ConfigArgSet(required = false, desc = "Context path.")
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @ConfigArgGet
    public Level getLogLevel() {
        return logLevel;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_LOG_LEVEL_STRING, desc = "Log level for ToughDay Engine")
    public void setLogLevel(String logLevel) {
        this.logLevel = Level.valueOf(logLevel);
    }

    @ConfigArgGet
    public boolean getDryRun() {
        return dryRun;
    }

    @ConfigArgSet(required = false, defaultValue = "false", desc = "If true, prints the resulting configuration and does not run any tests.")
    public void setDryRun(String dryRun) {
        this.dryRun = Boolean.valueOf(dryRun);
    }

    @ConfigArgSet(required = false, defaultValue = "true", desc = "If true, saves the current configuration into a yaml configuration file.")
    public void setSaveConfig(String saveConfig) {
        this.saveConfig = Boolean.valueOf(saveConfig);
    }

    @ConfigArgGet
    public boolean getSaveConfig() {
        return saveConfig;
    }

    @ConfigArgSet(required = false, defaultValue = "false", desc = "Show test steps in the aggregated publish. (They are always shown in the detailed publish)")
    public void setShowSteps(String showTestSteps) {
        this.showSteps = Boolean.parseBoolean(showTestSteps);
    }

    @ConfigArgGet
    public boolean getShowSteps() {
        return this.showSteps;
    }

    @ConfigArgGet
    public boolean getHostValidationEnabled() {
        return this.hostValidationEnabled;
    }

    @ConfigArgGet
    public String getLogPath() {
        return logPath;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_LOG_PATH, desc = "The path where the logs folder will be created.")
    public void setLogPath(String logPath) {
        if (!logPath.equals("/") && logPath.endsWith("/")) {
            logPath = logPath.substring(0, logPath.length() - 1);
        }

        if (logPath.startsWith("~")) {
            logPath = System.getProperty("user.home") + logPath.substring(1);
        }

        System.setProperty("logFileName", logPath);

        this.logPath = logPath;
    }
}
