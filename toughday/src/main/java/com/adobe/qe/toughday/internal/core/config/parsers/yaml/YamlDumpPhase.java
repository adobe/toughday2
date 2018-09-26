package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlDumpPhase {
    private String name;
    private Boolean measurable;
    private String useconfig;
    private List<YamlDumpAction> tests = new ArrayList<>();
    private Map<String, Object> runmode;
    private Map<String, Object> publishmode;

    public YamlDumpPhase(Map<String, Object> properties, Map<String, Object> runmode, Map<String, Object> publishmode) {
        this.name = properties.containsKey("name") ? properties.get("name").toString() : null;
        this.measurable = properties.containsKey("measurable") ? (Boolean)(properties.get("measurable")) : null;
        this.useconfig = properties.containsKey("useconfig") ? properties.get("useconfig").toString() : null;
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
}
