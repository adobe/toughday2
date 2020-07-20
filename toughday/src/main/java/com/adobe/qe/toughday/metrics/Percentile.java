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
package com.adobe.qe.toughday.metrics;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.RunMap;

@Description(desc = "Percentile.")
public class Percentile extends Metric {
    private double value;

    @ConfigArgSet(required = true, desc = "The value at which percentile will be calculated.")
    public Percentile setValue(String value) {
        this.value = Double.valueOf(value.substring(0,value.length() - 1));
        if (this.name.equals(getClass().getSimpleName())) {
            this.name = value;
        }
        return this;
    }

    @ConfigArgGet
    public String getValue() {
        return String.valueOf(this.value) + 'p';
    }

    @Override
    public Object getValue(RunMap.TestStatistics testStatistics) {
        return testStatistics.getValueAtPercentile(value);
    }

    @Override
    public String getFormat() {
        return "%d";
    }

    @Override
    public String getUnitOfMeasure() {
        return "ms";
    }
}
