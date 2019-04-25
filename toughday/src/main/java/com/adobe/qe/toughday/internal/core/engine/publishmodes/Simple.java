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
package com.adobe.qe.toughday.internal.core.engine.publishmodes;

import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import com.adobe.qe.toughday.internal.core.engine.PublishMode;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@Description(desc = "Results are aggregated and published for the whole run.")
public class Simple extends PublishMode {

    @Override
    public void publishIntermediateResults(Map<String, List<MetricResult>> results) {
        try {
            engine.getCurrentPhaseLock().readLock().lock();
            for(Publisher publisher : engine.getCurrentPhase().getPublishers()) {
                publisher.publishAggregatedIntermediate(results);
            }
        } finally {
            engine.getCurrentPhaseLock().readLock().unlock();
        }
    }

    @Override
    public void publish(Collection<TestResult> testResults) {
        try {
            engine.getCurrentPhaseLock().readLock().lock();
            for(Publisher publisher : engine.getCurrentPhase().getPublishers()) {
                publisher.publishRaw(testResults);
            }
        } finally {
            engine.getCurrentPhaseLock().readLock().unlock();
        }
    }

    @Override
    public void publishFinalResults(Map<String, List<MetricResult>> results) {
        try {
            engine.getCurrentPhaseLock().readLock().lock();
            for (Publisher publisher : engine.getCurrentPhase().getPublishers()) {
                publisher.publishAggregatedFinal(results);
            }
        } finally {
            engine.getCurrentPhaseLock().readLock().unlock();
        }
    }
}
