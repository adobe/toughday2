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
package com.adobe.qe.toughday.internal.core.engine;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.internal.core.RunMapImpl;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class PublishMode {
    protected Engine engine;
    protected RunMapImpl globalRunMap;

    public PublishMode() {
        this.globalRunMap = new RunMapImpl();
    }

    @Deprecated
    public RunMapImpl getGlobalRunMap() {
        return globalRunMap;
    }

    public RunMapImpl getRunMap() {
        return globalRunMap;
    }

    public Map<AbstractTest, Long> aggregateAndReinitialize(RunMap runMap) {
        return globalRunMap.aggregateAndReinitialize(runMap);
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public abstract void publishIntermediateResults(Map<String, List<MetricResult>> results);

    public abstract void publishFinalResults(Map<String, List<MetricResult>> results);

    public abstract void publish(Collection<TestResult> testResults);
}
