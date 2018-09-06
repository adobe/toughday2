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
import com.adobe.qe.toughday.api.annotations.labels.Nullable;

/**
 * Use this interface to create proxy factories. Useful when your proxy needs more logic than calling the default constructor
 * and the injections provided by the {@link Proxy} interface in order to be constructed.
 */
public interface ProxyFactory<T> {
    /**
     * Create a new proxy
     *
     * @param target
     * @param test
     * @param benchmark
     * @return A proxy object. If {@code null} os returned, the default proxy will be used.
     */
    @Nullable T createProxy(T target, AbstractTest test, Benchmark benchmark);
}
