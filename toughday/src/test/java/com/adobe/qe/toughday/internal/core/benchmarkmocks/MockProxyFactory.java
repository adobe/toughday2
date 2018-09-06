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

public class MockProxyFactory implements ProxyFactory<MockWorker> {
    private boolean invoked;

    @Override
    public MockWorker createProxy(MockWorker target, AbstractTest test, Benchmark benchmark) {
        invoked = true;
        MockProxy mockProxy = new MockProxyFromFactory();
        mockProxy.setBenchmark(benchmark);
        mockProxy.setTarget(target);
        mockProxy.setTest(test);
        return mockProxy;
    }

    public boolean isInvoked() { return this.invoked; }
}
