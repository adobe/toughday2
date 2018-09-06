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
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.api.core.RunMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * Base class for all metrics. Classes extending this class, if not abstract, will be shown in help.
 */
public abstract class Metric {

    protected String name;
    protected int decimals;

    private static final int DEFAULT_DECIMALS = 6;

    public static final List<Metric> basicMetrics = Collections.unmodifiableList(
            new ArrayList<Metric>() {{
                add(new Passed());
                add(new Failed());
                add(new Skipped());
            }});

    public static final List<Metric> defaultMetrics = Collections.unmodifiableList(
            new ArrayList<Metric>() {{
                addAll(basicMetrics);
                add(new Average());
                add(new Median());
                add(new StdDev());
                add(new Percentile().setValue("90p"));
                add(new Percentile().setValue("99p"));
                add(new Percentile().setValue("99.9p"));
                add(new Min());
                add(new Max());
                add(new RealTP());
            }});


    public Metric() {
        this.name = getClass().getSimpleName();
        this.decimals = DEFAULT_DECIMALS;
    }

    /**
     * Returns all the information that publishers need in order to print this metric.
     * @return
     * @param testStatistics
     */

    public MetricResult getResult(RunMap.TestStatistics testStatistics) {
        return new MetricResultImpl<>(this.getName(), this.getValue(testStatistics), this.getFormat(), this.getUnitOfMeasure());
    }

    public abstract Object getValue(RunMap.TestStatistics testStatistics);

    @ConfigArgSet(required = false, desc = "The name of the metric.")
    public Metric setName(String name) {
        this.name = name;
        return this;
    }

    @ConfigArgSet(required = false, desc = "Number of decimals.", defaultValue = "6")
    public Metric setDecimals(String decimals) {
        this.decimals = Integer.parseInt(decimals);
        return this;
    }

    @ConfigArgGet
    public String getName() {
        return name;
    }

    @ConfigArgGet
    public int getDecimals() {
        return decimals;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof Metric && this.getName().equals(((Metric) o).getName()));
    }

    public abstract String getFormat();

    public abstract String getUnitOfMeasure();

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17,37).append(name).toHashCode();
    }
}
