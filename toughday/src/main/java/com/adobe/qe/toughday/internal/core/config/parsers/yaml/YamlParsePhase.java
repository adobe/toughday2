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
package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import java.util.List;
import java.util.Map;

public class YamlParsePhase {
    private String name;
    private Boolean measurable;
    private String useconfig;
    private String duration;
    private List<YamlParseAction> tests;
    private List<YamlParseAction> metrics;
    private List<YamlParseAction> publishers;
    private Map<String, Object> runmode;
    private Map<String, Object> publishmode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getMeasurable() {
        return measurable;
    }

    public void setMeasurable(Boolean measurable) {
        this.measurable = measurable;
    }

    public List<YamlParseAction> getTests() {
        return tests;
    }

    public void setTests(List<YamlParseAction> tests) {
        this.tests = tests;
    }

    public Map<String, Object> getRunmode() {
        return runmode;
    }

    public void setRunmode(Map<String, Object> runmode) {
        this.runmode = runmode;
    }

    public String getUseconfig() {
        return useconfig;
    }

    public void setUseconfig(String useconfig) {
        this.useconfig = useconfig;
    }

    public Map<String, Object> getPublishmode() {
        return publishmode;
    }

    public void setPublishmode(Map<String, Object> publishmode) {
        this.publishmode = publishmode;
    }

    public List<YamlParseAction> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<YamlParseAction> metrics) {
        this.metrics = metrics;
    }

    public List<YamlParseAction> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<YamlParseAction> publishers) {
        this.publishers = publishers;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
