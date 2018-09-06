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
package com.adobe.qe.toughday.tests;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.benchmark.Benchmark;
import com.adobe.qe.toughday.api.core.benchmark.Proxy;
import com.adobe.qe.toughday.api.core.benchmark.ResultInfo;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;

import java.util.Collections;

public class WorkerProxy extends Worker implements Proxy<Worker> {
    private final String info;
    private AbstractTest test;
    private Worker target;
    private Benchmark benchmark;
    
    public WorkerProxy(String info) {
        this.info = info;
    }

    @Override
    public long doWork(long millis) throws Throwable {
        ResultInfo<Long, Object> result = benchmark().computeTestResult(test, (TestResult<Object> testResult) -> {
            return super.doWork(millis);
        });

        TestResult currentTestResult = result.getTestResult();
        currentTestResult.withData(Collections.singletonMap("sleep", millis));
        benchmark().getRunMap().record(currentTestResult);

        if(result.getThrowable() != null) { throw result.getThrowable(); }
        return result.getReturnValue();
    }

    /* ----------------- Proxy Interface implementation --------------------------- */
    @Override
    public void setTest(AbstractTest test) { this.test = test; }

    @Override
    public void setTarget(Worker target) { this.target = target; }

    @Override
    public void setBenchmark(Benchmark benchmark) { this.benchmark = benchmark; }

    @Override
    public Benchmark benchmark() { return benchmark; }
    /* --------------------------------------------------------------------------- */
}
