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
import com.adobe.qe.toughday.api.core.benchmark.ProxyFactory;
import com.adobe.qe.toughday.api.core.benchmark.ResultInfo;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;

public class WorkerHierarchyProxyFactory implements ProxyFactory<Worker> {
    @Override
    public Worker createProxy(Worker target, AbstractTest test, Benchmark benchmark) {
        Worker proxy = Mockito.spy(target);
        try {
            Mockito.doAnswer(new Answer<Long>() {
                @Override
                public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                    long millis = invocationOnMock.getArgument(0);
                    ResultInfo<Long, Object> result = benchmark.computeTestResult(test, (TestResult<Object> testResult) -> {
                        return (Long) invocationOnMock.callRealMethod();
                    });

                    TestResult currentTestResult = result.getTestResult();
                    currentTestResult.withData(Collections.singletonMap("sleep", millis));
                    benchmark.getRunMap().record(currentTestResult);

                    if(result.getThrowable() != null) { throw result.getThrowable(); }
                    return result.getReturnValue();
                }
            }).when(proxy).doWork(Mockito.anyLong());
        } catch (Throwable throwable) {
            test.logger().warn("Could not create proxy", throwable);
            return target;
        }

        return proxy;
    }
}
