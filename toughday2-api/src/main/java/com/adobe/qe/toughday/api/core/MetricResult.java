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

public interface MetricResult<T> {

    /**
     * Getter for the name of the metric.
     * @return
     */
    String getName();

    /**
     * Getter for the value of this metric.
     */
    T getValue();


    /**
     * Getter for the format of the result of this metric. For instance, if this metric is going to return a string as
     * a result, this method should return "%s".
     */
    String getFormat();

    /**
     * Getter fot the unit of measure for the value of this metric.
     */
    String getUnitOfMeasure();


}
