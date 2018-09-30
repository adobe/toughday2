package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import java.util.List;
import java.util.Map;

public class YamlParsePhase {
    private String name;
    private Boolean measurable;
    private String useconfig;
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
}
