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
package com.adobe.qe.toughday.internal.core.benchmarkmocks;


import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.benchmark.Benchmark;
import com.adobe.qe.toughday.api.core.benchmark.Proxy;
import com.adobe.qe.toughday.api.core.benchmark.ResultInfo;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import org.junit.Assert;

import java.util.Map;

public class MockProxy extends MockWorker implements Proxy<MockWorker> {
    private AbstractTest test;
    private MockWorker target;
    private Benchmark benchmark;

    @Override
    public void setTest(AbstractTest test) {
        this.test = test;
    }

    @Override
    public void setTarget(MockWorker target) {
        this.target = target;
    }

    @Override
    public void setBenchmark(Benchmark benchmark) {
        this.benchmark = benchmark;
    }

    @Override
    public Benchmark benchmark() {
        return this.benchmark;
    }


    public Object methodWithReturnValue(Long... args) throws Throwable {
        ResultInfo<Object, Map> resultInfo =  benchmark().computeTestResult(test, (TestResult<Map> result) -> {
            return target.methodWithReturnValue(args);
        });

        benchmark().getRunMap().record(resultInfo.getTestResult());
        if(resultInfo.getThrowable() != null) throw resultInfo.getThrowable();
        return resultInfo.getReturnValue();
    }
}
