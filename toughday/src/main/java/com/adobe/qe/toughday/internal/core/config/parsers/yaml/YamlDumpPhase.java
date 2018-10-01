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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlDumpPhase {
    private String name;
    private Boolean measurable;
    private String useconfig;
    private String duration;
    private List<YamlDumpAction> tests = new ArrayList<>();
    private List<YamlDumpAction> metrics = new ArrayList<>();
    private List<YamlDumpAction> publishers = new ArrayList<>();
    private Map<String, Object> runmode;
    private Map<String, Object> publishmode;

    public YamlDumpPhase(Map<String, Object> properties, Map<String, Object> runmode, Map<String, Object> publishmode) {
        this.name = properties.containsKey("name") ? properties.get("name").toString() : null;
        this.measurable = properties.containsKey("measurable") ? (Boolean)(properties.get("measurable")) : null;
        this.useconfig = properties.containsKey("useconfig") ? properties.get("useconfig").toString() : null;
        this.duration = properties.containsKey("duration") ? properties.get("duration").toString() : null;
        this.runmode = runmode;
        this.publishmode = publishmode;
    }

    public String getName() {
        return name != null? name : "";
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

    public String getUseconfig() {

        return useconfig != null ? useconfig : "";
    }

    public void setUseconfig(String useconfig) {
        this.useconfig = useconfig;
    }

    public List<YamlDumpAction> getTests() {
        return tests;
    }

    public void setTests(List<YamlDumpAction> tests) {
        this.tests = tests;
    }

    public Map<String, Object> getRunmode() {
        return runmode;
    }

    public void setRunmode(Map<String, Object> runmode) {
        this.runmode = runmode;
    }

    public Map<String, Object> getPublishmode() {
        return publishmode;
    }

    public void setPublishmode(Map<String, Object> publishmode) {
        this.publishmode = publishmode;
    }

    public List<YamlDumpAction> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<YamlDumpAction> metrics) {
        this.metrics = metrics;
    }

    public List<YamlDumpAction> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<YamlDumpAction> publishers) {
        this.publishers = publishers;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
