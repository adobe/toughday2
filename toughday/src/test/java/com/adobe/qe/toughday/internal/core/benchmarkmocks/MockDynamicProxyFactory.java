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
import com.adobe.qe.toughday.api.core.benchmark.ProxyFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockDynamicProxyFactory implements ProxyFactory<MockWorker> {
    @Override
    public MockWorker createProxy(MockWorker target, AbstractTest test, Benchmark benchmark) {
        MockWorker spy = Mockito.spy(target);

        MockProxy proxy = new MockProxy();
        proxy.setTarget(target);
        proxy.setTest(test);
        proxy.setBenchmark(benchmark);

        try {
            Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    Object[] callArgs = invocationOnMock.getArguments();
                    Long[] args = new Long[invocationOnMock.getArguments().length];
                    for(int i = 0; i < callArgs.length; i++) {
                       args[i] =(Long) callArgs[i];
                    }
                    return proxy.methodWithReturnValue(args);
                }
            }).when(spy).methodWithReturnValue(Mockito.any());
        } catch (Throwable throwable) {
            spy = target;
        }

        return spy;
    }
}
