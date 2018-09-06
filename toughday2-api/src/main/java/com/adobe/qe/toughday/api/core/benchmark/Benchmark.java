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
import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.api.annotations.labels.Nullable;
import com.adobe.qe.toughday.api.annotations.labels.ThreadSafe;
import com.adobe.qe.toughday.api.core.benchmark.signatures.Callable;
import com.adobe.qe.toughday.api.core.benchmark.signatures.InjectTestResultCallable;
import com.adobe.qe.toughday.api.core.benchmark.signatures.VoidCallable;
import com.adobe.qe.toughday.api.core.benchmark.signatures.VoidInjectTestResultCallable;

/**
 * This class provides methods for benchmarking arbitrary code blocks and method calls + helper methods.
 * These benchmarks are used as entries in the run map. An entry will contain, among other information
 * the duration and the status (PASSED/FAILED/SKIPPED) of the code/method under observation.
 */
@ThreadSafe
public interface Benchmark {

    /**
     * Register a proxy class for the given class
     * The proxy class needs to be instantiable using the default constructor
     * If the proxy class implements {@link Proxy}, context information can be injected into the proxy instances
     * Resolution: If a proxy class and a proxy factory are registered for the same target class, the proxy class will be used
     * @param klass The target class
     * @param proxyClass The proxy class. If {@code null} is given, then the default proxy will be used.
     * @param <T> The type of the target
     * @param <F> The type of the proxy
     */
    <T, F extends T> void registerClassProxy(Class<T> klass, @Nullable Class<F> proxyClass);

    /**
     * Register a proxy factory for the given class
     * If the returned proxies implement {@link Proxy}, context information can be injected into the proxy instances
     * Resolution: If a proxy class and a proxy factory are registered for the same target class, the proxy class will be used
     * @param klass The target class
     * @param proxyFactory The proxy factory object. If {@code null} is given, then the default proxy will be used.
     * @param <T> The type of the target class
     */
    <T> void registerClassProxyFactory(Class<T> klass, @Nullable ProxyFactory<T> proxyFactory);

    /**
     * Register a proxy factory for a hierarchy.
     * If the returned proxies implement {@link Proxy}, context information can be injected into the proxy instances
     * Resolution:
     *  * If proxy class/factory is registered for a more specific type in the type hierarchy,
     * it will be used instead of this one.
     *  * If hierarchy proxy factories are registered for both a super/ancestor class and a interface of
     * a object, the one for the super/ancestor class is used
     *  * If hierarchy proxy factories are registered for two interfaces of a object, the one used will be
     * the one which appears first in the {@code object.getClass().getInterfaces()} list
     * @param klass the class at the top of the hierarchy
     * @param proxyFactory The proxy factory object. If {@code null} is given, then the default proxy will be used.
     * @param <T> The type of the target class
     */
    <T> void registerHierarchyProxyFactory(Class<T> klass, @Nullable ProxyFactory<T> proxyFactory);

    /**
     * Setter for the run map.
     * @param runMap
     */
    void setRunMap(RunMap runMap);

    /**
     * Getter for the run map.
     * @return
     */
    RunMap getRunMap();

    /**
     * Computes the test result of the operation from the functional interface. It creates a triplet containing:
     * The test result, the return value and any Throwable that occurred. If a throwable occurred the return value will be
     * {@code null} and vice versa.
     * IMPORTANT: This method does not record the test result in the run map. Use this method with caution and only when
     * you don't want the result to be immediately recorded. This gives the caller the responsibility of correctly
     * record the result and handle the return value/throwable.
     * @param test The {@link AbstractTest} representation of the of the operation from the functional interface
     * @param callable The functional interface with the operation that is benchmarked
     * @param <R> The type of the return value
     * @param <K> The type of the data from the {@link TestResult}
     * @return A triplet containing: The test result, the return value and any Throwable that occurred.
     */
    <R, K> ResultInfo<R, K> computeTestResult(AbstractTest test, InjectTestResultCallable<R, K> callable);

    /**
     * Measures the duration of a method call using either a registered proxy implementation for the object's class, or
     * the default proxy implementation.
     * Usage:
     * <pre>
     *     <code>
     *         benchmark().measure(test, myObject).myMethod();
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the <code>myMethod</code> call
     * and record it in the {@link RunMap}.
     * Limitations: This feature doesn't work for now with final methods, methods in final classes, or private methods, but
     * you can use the other flavours of the measure call to work around it.
     * @param test The {@link AbstractTest} representation of the method call
     * @param object The target object on which the method will be invoked
     * @param <T> The class of the target object
     * @return The proxy object, for chaining the method call
     */
    <T> T measure(AbstractTest test, T object) throws Throwable;

    /**
     * Measures the duration of a method call using either a registered proxy implementation for the object's class, or
     * the default proxy implementation.
     * Usage:
     * <pre>
     *     <code>
     *         benchmark().measure(parent, "Simple Step", myObject).myMethod();
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the <code>myMethod</code> call
     * and record it in the {@link RunMap}.
     * It will also create a {@link AbstractTest} representation for this code, with the name specified by the {@code label}
     * and the parent specified by {@code parent}
     * Limitations: This feature doesn't work for now with final methods, methods in final classes, or private methods, but
     * you can use the other flavours of the measure call to work around it.
     * @param parent The parent test.
     * @param label The name of the test to be created for this method call
     * @param object The target object on which the method will be invoked
     * @param <T> The class of the target object
     * @return The proxy object, for chaining the method call
     */
    <T> T measure(AbstractTest parent, String label, T object) throws Throwable;

    /**
     * Measures the duration of a method call using either the given proxy
     * Usage:
     * <pre>
     *     <code>
     *         benchmark().measure(test, myObject, myProxy).myMethod();
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the <code>myMethod</code> call
     * and record it in the {@link RunMap}.
     * The responsibility for constructing the TestResult and record it into the {@link RunMap} is deffered to the proxy object for
     * custom handling.
     * Limitations: This feature doesn't work for now with final methods, methods in final classes, or private methods, but
     * you can use the other flavours of the measure call to work around it.
     * @param test The {@link AbstractTest} representation of the method call
     * @param object The target object on which the method will be invoked
     * @param proxy A proxy object. If the {@link Proxy} interface is implemented, information will be injected in the proxy object.
     * @param <T> The class of the target object
     * @return The proxy object, for chaining the method call
     */
    <T> T measure(AbstractTest test, T object, T proxy) throws Throwable;

    /**
     * Measures the duration of a method call using either the given proxy
     * Usage:
     * <pre>
     *     <code>
     *         benchmark().measure(parent, "Simple Step", myObject, myProxy).myMethod();
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the <code>myMethod</code> call
     * and record it in the {@link RunMap}.
     * It will also create a {@link AbstractTest} representation for this code, with the name specified by the {@code label}
     * and the parent specified by {@code parent}
     * Limitations: This feature doesn't work for now with final methods, methods in final classes, or private methods, but
     * you can use the other flavours of the measure call to work around it.
     * @param parent The parent test.
     * @param label The name of the test to be created for this method call
     * @param object The target object on which the method will be invoked
     * @param proxy A proxy object. If the {@link Proxy} interface is implemented, information will be injected in the proxy object.
     * @param <T> The class of the target object
     * @return The proxy object, for chaining the method call
     */
    <T> T measure(AbstractTest parent, String label, T object, T proxy) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface.
     * <pre>
     *     <code>
     *         benchmark().measure(test, "Simple Step", () -> {
     *             //code that needs to be benchmarked
     *             return myResult;
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param test The {@link AbstractTest} representation of the of the operation from the functional interface
     * @param <T> The type of the return value
     * @param callable The functional interface with the operation that is benchmarked
     * @return The return value of the operation from the functional interface
     */
    <T> T measure(AbstractTest test, Callable<T> callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface.
     * <pre>
     *     <code>
     *         benchmark().measure(parent, label, "Simple Step", () -> {
     *             //code that needs to be benchmarked
     *             return myResult;
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param parent The parent test.
     * @param label The name of the ad hoc test
     * @param <T> The type of the return value
     * @param callable The functional interface with the operation that is benchmarked
     * @return The return value of the operation from the functional interface
     */
    <T> T measure(AbstractTest parent, String label, Callable<T> callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface.
     * <pre>
     *     <code>
     *         benchmark().measure(test, "Simple Step", () -> {
     *             //code that needs to be benchmarked
     *             return myResult;
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param test The {@link AbstractTest} representation of the of the operation from the functional interface
     * @param callable The functional interface with the operation that is benchmarked
     */
    void measure(AbstractTest test, VoidCallable callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface.
     * <pre>
     *     <code>
     *         benchmark().measure(parent, "Simple Step", () -> {
     *             //code that needs to be benchmarked
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param parent The parent test.
     * @param label The name of the ad hoc test
     * @param callable The functional interface with the operation that is benchmarked
     */
    void measure(AbstractTest parent, String label, VoidCallable callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface. Injects the {@link TestResult}.
     * <pre>
     *     <code>
     *         benchmark().measure(test, "Simple Step", (TestResult testResult) -> {
     *             //code that needs to be benchmarked
     *             return myResult;
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param test The {@link AbstractTest} representation of the of the operation from the functional interface
     * @param <T> The type of the return value
     * @param <K> The type of the data from the {@link TestResult} object 
     * @param callable The functional interface with the operation that is benchmarked
     * @return The return value of the operation from the functional interface
     */
    <T, K> T measure(AbstractTest test, InjectTestResultCallable<T, K> callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface. Injects the {@link TestResult}.
     * <pre>
     *     <code>
     *         benchmark().measure(parent, label, "Simple Step", (TestResult testResult) -> {
     *             //code that needs to be benchmarked
     *             return myResult;
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param parent The parent test.
     * @param label The name of the ad hoc test
     * @param <T> The type of the return value
     * @param <K> The type of the data from the {@link TestResult} object
     * @param callable The functional interface with the operation that is benchmarked
     * @return The return value of the operation from the functional interface
     */
    <T, K> T measure(AbstractTest parent, String label, InjectTestResultCallable<T, K> callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface. Injects the {@link TestResult}.
     * <pre>
     *     <code>
     *         benchmark().measure(test, "Simple Step", (TestResult testResult) -> {
     *             //code that needs to be benchmarked
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param test The {@link AbstractTest} representation of the of the operation from the functional interface
     * @param callable The functional interface with the operation that is benchmarked
     * @param <K> The type of the data from the {@link TestResult} object
     */
    <K> void measure(AbstractTest test, VoidInjectTestResultCallable<K> callable) throws Throwable;

    /**
     * Measures the duration of the operation from the functional interface. Injects the {@link TestResult}.
     * <pre>
     *     <code>
     *         benchmark().measure(parent, label, "Simple Step", (TestResult testResult) -> {
     *             //code that needs to be benchmarked
     *         });
     *     </code>
     * </pre>
     * The above code will create a {@link TestResult} with (among others) the duration and the status of the operation from
     * the functional interface and record it in the {@link RunMap}.
     * @param parent The parent test.
     * @param label The name of the ad hoc test
     * @param callable The functional interface with the operation that is benchmarked
     * @param <K> The type of the data from the {@link TestResult} object
     */
    <K> void measure(AbstractTest parent, String label, VoidInjectTestResultCallable<K> callable) throws Throwable;
}
