package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes.RunModeBalancer;
import com.adobe.qe.toughday.internal.core.distributedtd.splitters.runmodes.RunModeSplitter;

import java.util.concurrent.ExecutorService;

public class DummyRunMode implements RunMode {
    private RunModeBalancer<DummyRunMode> dummyRunModeBalancer = new DummyRunModeBalancer();
    private String property1 = "prop1";
    private String property2 = "prop2";
    private String property3 = "prop3";

    @ConfigArgSet(required = false, defaultValue = "p1")
    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    @ConfigArgSet(required = false, defaultValue = "p2")
    public void setProperty2(String property2) {
        this.property2 = property2;
    }

    @ConfigArgSet(required = false, defaultValue = "p3")
    public void setProperty3(String property3) {
        this.property3 = property3;
    }

    @ConfigArgGet(redistribute = true)
    public String getProperty1() {
        return this.property1;
    }

    @ConfigArgGet(redistribute = true)
    public String getProperty2() {
        return this.property2;
    }

    @ConfigArgGet
    public String getProperty3() {
        return this.property3;
    }

    @Override
    public void runTests(Engine engine) throws Exception {

    }

    @Override
    public void finishExecutionAndAwait() {

    }

    @Override
    public ExecutorService getExecutorService() {
        return null;
    }

    @Override
    public RunContext getRunContext() {
        return null;
    }

    @Override
    public <T extends RunMode> RunModeSplitter<T> getRunModeSplitter() {
        return null;
    }

    @Override
    public RunModeBalancer<DummyRunMode> getRunModeBalancer() {
       return this.dummyRunModeBalancer;
    }
}
