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
package com.adobe.qe.toughday.publishers;

import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MyPublisher extends Publisher {
    private static final String RAW_FORMAT = "";

    @Override
    protected void doPublishAggregatedIntermediate(Map<String, List<MetricResult>> results) {
        doPublishAggregated(results);
    }


    @Override
    protected void doPublishAggregatedFinal(Map<String, List<MetricResult>> results) {
        doPublishAggregated(results);
    }

    private void doPublishAggregated(Map<String, List<MetricResult>> results) {
        for (String testName : results.keySet()) {
            List<MetricResult> testResultInfos = results.get(testName);
            for (MetricResult resultInfo : testResultInfos) {
                /*myExportAggregated(resultInfo.getName(),
                        resultInfo.getValue(),
                        resultInfo.getFormat(),
                        resultInfo.getUnitOfMeasure());*/
            }
        }
    }

    @Override
    protected void doPublishRaw(Collection<TestResult> testResults) {
        for (TestResult testResult : testResults) {
            Object data = testResult.getData();
            /*myExportRaw(String.format(RAW_FORMAT,
                    testResult.getTestFullName(),
                    testResult.getStatus().toString(),
                    testResult.getThreadId(),
                    testResult.getFormattedStartTimestamp(),
                    testResult.getFormattedEndTimestamp(),
                    testResult.getDuration(),
                    testResult.getData()));*/
        }
    }


    @Override
    public void finish() {
        // Clean up
    }
}
