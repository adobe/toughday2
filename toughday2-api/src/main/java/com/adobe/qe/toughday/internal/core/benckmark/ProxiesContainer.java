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
package com.adobe.qe.toughday.internal.core.benckmark;


import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.labels.Nullable;
import com.adobe.qe.toughday.api.core.benchmark.Benchmark;
import com.adobe.qe.toughday.api.core.benchmark.ProxyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Container for proxies
 */
public class ProxiesContainer {
    ReadWriteLock containerLock = new ReentrantReadWriteLock();

    Map<Class, Class> proxies = new HashMap<>();
    Map<Class, ProxyFactory> proxyFactories = new HashMap<>();
    Map<Class, ProxyFactory> proxyHierarchyFactories = new HashMap<>();

    //caches
    Map<Class, Class> proxiesCache = new HashMap<>();
    Map<Class, ProxyFactory> proxyFactoriesCache = new HashMap<>();

    /**
     * Get proxy for the target object
     * Resolution:
     *  * If a proxy class and a proxy factory are registered for the same target class, the proxy class will be used
     *  * If proxy class/factory is registered for a more specific type in the type hierarchy,
     * it will be used.
     *  * If hierarchy proxy factories are registered for both a ancestor class and a interface of
     * a object, the one for the ancestor class is used
     *  * If hierarchy proxy factories are registered for two interfaces of a object, the one used will be
     * the one which appears first in the {@code object.getClass().getInterfaces()} list
     * @param target the target object
     * @param test The test object
     * @param benchmark The benchmark object
     * @param <T> The typf of the target object
     * @return A proxy object, or null if proxy class/factory was found for the target object
     * @throws Exception
     */
    public @Nullable <T> T getProxy(T target, AbstractTest test, Benchmark benchmark) throws Exception {
        Class targetClass = target.getClass();
        containerLock.readLock().lock();
        try {
            Class<T> proxyClass;
            if(proxiesCache.containsKey(targetClass)) {
                proxyClass = proxiesCache.get(targetClass);
                return proxyClass != null ? proxyClass.newInstance() : null;
            }

            if(proxies.containsKey(targetClass)) {
                proxyClass = proxies.get(targetClass);
                addToCache(proxiesCache, targetClass, proxyClass);
                return proxyClass != null ? proxyClass.newInstance() : null;
            }

            ProxyFactory<T> proxyFactory;
            if(proxyFactoriesCache.containsKey(targetClass)) {
                proxyFactory = proxyFactoriesCache.get(targetClass);
                return proxyFactory != null ? proxyFactory.createProxy(target, test, benchmark) : null;
            }

            if(proxyFactories.containsKey(targetClass)) {
                proxyFactory = proxyFactories.get(targetClass);
                addToCache(proxyFactoriesCache,targetClass, proxyFactory);
                return proxyFactory != null ? proxyFactory.createProxy(target, test, benchmark) : null;
            }

            for(Class p = targetClass; p != null; p = p.getSuperclass()) {
                if(proxyHierarchyFactories.containsKey(p)) {
                    proxyFactory = proxyHierarchyFactories.get(p);
                    addToCache(proxyFactoriesCache, targetClass, proxyFactory);
                    return proxyFactory != null ? proxyFactory.createProxy(target, test, benchmark) : null;
                }
            }

            for(Class p : targetClass.getInterfaces()) {
                if(proxyHierarchyFactories.containsKey(p)) {
                    proxyFactory = proxyHierarchyFactories.get(p);
                    addToCache(proxyFactoriesCache, targetClass, proxyFactory);
                    return proxyFactory != null ? proxyFactory.createProxy(target, test, benchmark) : null;
                }
            }

            //No need to check again for custom proxies unless the cache is invalidated
            addToCache(proxiesCache, targetClass, null);

            return null;
        } finally {
            containerLock.readLock().unlock();
        }
    }

    private <T> void addToCache(Map<Class, T> cache, Class key, T value) {
        containerLock.readLock().unlock();
        containerLock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            containerLock.readLock().lock();
            containerLock.writeLock().unlock();
        }
    }

    public ProxiesContainer clone() {
        containerLock.readLock().lock();
        try {
            ProxiesContainer clone = new ProxiesContainer();
            for (Map.Entry<Class, Class> entry : proxies.entrySet()) {
                clone.registerClassProxy(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<Class, ProxyFactory> entry : proxyFactories.entrySet()) {
                clone.registerClassProxyFactory(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<Class, ProxyFactory> entry : proxyHierarchyFactories.entrySet()) {
                clone.registerHierarchyFactory(entry.getKey(), entry.getValue());
            }
            return clone;
        } finally {
            containerLock.readLock().unlock();
        }

    }

    public <T, F extends T> void registerClassProxy(Class<T> klass, @Nullable Class<F> proxyClass) {
        if(proxyClass != null) {
            try {
                proxyClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("Proxies need to be instantiated using a constructor without arguments");
            }
        }

        containerLock.writeLock().lock();
        try {
            proxiesCache.clear();
            proxyFactoriesCache.clear();

            proxies.put(klass, proxyClass);
            proxiesCache.put(klass, proxyClass);

        } finally {
            containerLock.writeLock().unlock();
        }
    }

    public <T> void registerClassProxyFactory(Class<T> klass, @Nullable ProxyFactory<T> proxyFactory) {
        containerLock.writeLock().lock();
        try {
            proxiesCache.clear();
            proxyFactoriesCache.clear();

            proxyFactories.put(klass, proxyFactory);
            proxyFactoriesCache.put(klass, proxyFactory);
        } finally {
            containerLock.writeLock().unlock();
        }
    }

    public <T> void registerHierarchyFactory(Class<T> klass, @Nullable ProxyFactory<T> proxyFactory) {
        containerLock.writeLock().lock();
        try {
            proxiesCache.clear();
            proxyFactoriesCache.clear();

            proxyHierarchyFactories.put(klass, proxyFactory);
            proxyFactoriesCache.put(klass, proxyFactory);
        } finally {
            containerLock.writeLock().unlock();
        }
    }
}
