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

import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.engine.Engine;

import java.util.List;
import java.util.Map;

@Description(desc = "Results are aggregated and published on intervals, rather than the whole execution. (Use --interval to specify the length of the aggregation interval).")
public class Intervals extends Simple {
    private static final String DEFAULT_INTERVAL = "5s";

    private String interval = "5s";
    private long delta;
    private long currentDelta = 0;

    public Intervals() {
        delta = computeDelta(GlobalArgs.parseDurationToSeconds(interval));
    }

    private final long computeDelta(long interval) {
        return interval * 1000 / Engine.RESULT_AGGREATION_DELAY - 1;
    }


    @ConfigArgSet(required = false, defaultValue = DEFAULT_INTERVAL, desc = "Set the publishing interval. Can be expressed in s(econds), m(inutes), h(ours). Example: 1m30s.")
    public void setInterval(String interval) {
        this.interval = interval;
        this.delta = computeDelta(GlobalArgs.parseDurationToSeconds(interval));
    }


    @ConfigArgGet
    public String getInterval() { return this.interval; }

    @Override
    public void publishIntermediateResults(Map<String, List<MetricResult>> results) {
        if (currentDelta < delta) {
            currentDelta++;
            return;
        }
        super.publishIntermediateResults(results);

        this.globalRunMap.reinitialize();
        this.currentDelta = 0;
    }
}
