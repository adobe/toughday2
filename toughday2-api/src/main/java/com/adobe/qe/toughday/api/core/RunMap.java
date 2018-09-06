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
package com.adobe.qe.toughday.api.core;

import com.adobe.qe.toughday.api.core.benchmark.TestResult;

import java.text.SimpleDateFormat;

public interface RunMap {
   public static final SimpleDateFormat TIME_STAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

   void record(TestResult testResult);

    /**
     * Get test statistics for a certain test.
     * @param test
     * @return
     */
    TestStatistics getRecord(AbstractTest test);

    /**
     * Test statistics
     */
     interface TestStatistics {

        /**
         * Get the test reference for this run
         * @return
         */
        AbstractTest getTest();

        /**
         * Get the timestamp of the last execution of this test.
         */
        String getTimestamp();

        /**
         * Get total number of seconds that all the threads have spent in the test
         * @return
         */
        double getTotalDuration();

        /**
         * Get total numbers of run for this test
         * @return
         */
        long getTotalRuns();

        /**
         * Get the real throughput
         * Calculated as follows: <code>number of total runs / total time duration, including setup and wait times </code>
         * @return
         */
        double getRealThroughput();

        /* TODO figure out how to compute this when the number of threads is not fixed
         * Get the execution throughput.
         * Calculated as follows: <code>number of parallel users / average duration</code>
         * @return

        double getExecutionThroughput();*/

        /**
         * Get the lowest duration of test execution.
         * @return
         */
        long getMinDuration();

        /**
         * Get the highest duration of test execution.
         * @return
         */
        long getMaxDuration();

        /**
         * Get the average duration of all test executions.
         Formula: Sum (request time) / Runs
         * @return
         */
        double getAverageDuration();

        /**
         * Get the median duration of all test executions.
         * @return
         */
        long getMedianDuration();

        /**
         * Get total numbers of fail runs for this test.
         * @return
         */
        long getFailRuns();

        /**
         * Get total numbers of skipped runs for this test.
         * @return
         */
        long getSkippedRuns();


        long getValueAtPercentile(double percentile);

        /**
         * Get the standard deviation of the results of this test.
         * @return
         */
        double getStandardDeviation();
    }
}
