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
package com.adobe.qe.toughday.internal.core.engine;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.metrics.Name;
import com.adobe.qe.toughday.metrics.Timestamp;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Phase implements Cloneable {
    private static final String DEFAULT_MEASURABILITY = "true";

    private String name;
    private Boolean measurable;
    private String useconfig;
    private Long duration;

    private TestSuite testSuite;
    private RunMode runMode;
    private PublishMode publishMode;
    private Map<String, Publisher> publishers = new HashMap<>();
    private Map<String, Metric> metrics = new LinkedHashMap<>();
    private Map<AbstractTest, AtomicLong> counts = new HashMap<>();

    /**
     * Creates shallow copy.
     * @throws CloneNotSupportedException if the object to be cloned does not implement the Cloneable interface.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @ConfigArgGet
    public String getName() {
        return name;
    }

    @ConfigArgSet(required = false, desc = "The name of the phase.")
    public void setName(String name) {
        this.name = name;
    }

    @ConfigArgGet
    public Boolean getMeasurable() {
        return measurable;
    }

    @ConfigArgSet(required = false, desc = "Option to specify whether the metrics of this phase will be taken into consideration",
        defaultValue = DEFAULT_MEASURABILITY)
    public void setMeasurable(String measurabile) {
        this.measurable = Boolean.valueOf(measurabile);
    }

    @ConfigArgGet
    public String getUseconfig() {
        return useconfig;
    }

    @ConfigArgSet(required = false, desc = "The name of the phase from which to import the configuration.")
    public void setUseconfig(String useconfig) {
        this.useconfig = useconfig;
    }

    @ConfigArgGet
    public String getDuration() {
        return duration == null ? null : String.valueOf(duration) + 's';
    }

    @ConfigArgSet(required = false, desc = "The duration of the current phase.")
    public void setDuration(String duration) {
        if (duration != null) {
            this.duration = GlobalArgs.parseDurationToSeconds(duration);
        }
    }


    public Map<AbstractTest, AtomicLong> getCounts() {
        return counts;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public void setTestSuite(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public RunMode getRunMode() {
        return runMode;
    }

    public void setRunMode(RunMode runMode) {
        this.runMode = runMode;
    }

    public PublishMode getPublishMode() {
        return publishMode;
    }

    public void setPublishMode(PublishMode publishMode) {
        this.publishMode = publishMode;
    }

    public Collection<Publisher> getPublishers() {
        return publishers.values();
    }

    public void setPublishers(Map<String, Publisher> publishers) {
        this.publishers = publishers;
    }

    public Collection<Metric> getMetrics() {

        List<Metric> requiredMetrics = new ArrayList<>();

        //add mandatory metrics
        requiredMetrics.add(new Name());
        requiredMetrics.add(new Timestamp());

        requiredMetrics.addAll(metrics.values());

        return requiredMetrics;
    }

    public void setMetrics(Map<String, Metric> metrics) {
        this.metrics = metrics;
    }
}
