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

import com.adobe.qe.toughday.api.core.MetricResult;

public class MetricResultImpl<T> implements MetricResult {

    private String name;
    private T value;
    private String format;
    private String unitOfMeasure;

    public MetricResultImpl(String name, T value, String format, String unitOfMeasure) {
        this.value = value;
        this.format = format;
        this.unitOfMeasure = unitOfMeasure;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

}
