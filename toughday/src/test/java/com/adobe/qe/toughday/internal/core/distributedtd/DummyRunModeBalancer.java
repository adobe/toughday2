package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes.AbstractRunModeBalancer;

public class DummyRunModeBalancer extends AbstractRunModeBalancer<DummyRunMode> {
    @Override
    public void before(RedistributionInstructions redistributionInstructions, DummyRunMode runMode) {

    }

    @Override
    public void after(RedistributionInstructions redistributionInstructions, DummyRunMode runMode) {

    }
}
