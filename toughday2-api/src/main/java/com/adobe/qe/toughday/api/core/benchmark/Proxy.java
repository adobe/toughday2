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
package com.adobe.qe.toughday.api.core.benchmark;

import com.adobe.qe.toughday.api.core.AbstractTest;

/**
 * If your custom proxy implements this interface, the setters from it will be called to inject information into
 * your proxy.
 * @param <T> Type of the object being proxied
 */
public interface Proxy<T> {
    /**
     * Method for injecting the test
     * @param test
     */
    void setTest(AbstractTest test);

    /**
     * Method for injecting the target
     * @param target
     */
    void setTarget(T target);

    /**
     * Method for injecting the benchmark object
     * @param benchmark
     */
    void setBenchmark(Benchmark benchmark);

    /**
     * Method for getting the injected benchmark object
     * @return
     */
    Benchmark benchmark();
}
