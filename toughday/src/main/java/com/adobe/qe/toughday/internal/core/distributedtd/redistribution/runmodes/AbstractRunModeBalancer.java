package com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRunModeBalancer<T extends RunMode> implements RunModeBalancer<T> {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    @Override
    public Map<String, String> getRunModePropertiesToRedistribute(Class type, T object) {
        final Map<String, String> properties = new HashMap<>();

        if (object == null) {
            throw new IllegalArgumentException("Run mode object must not be null.");
        }

        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ConfigArgGet.class))
                .filter(method -> method.getAnnotation(ConfigArgGet.class).redistribute())
                .forEach(method -> {
                    String propertyName = Configuration.propertyFromMethod(method.getName());
                    try {
                        Object value = method.invoke(object);
                        properties.put(propertyName, String.valueOf(value));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        LOG.warn("Property " + propertyName + " could not be collected for redistribution instructions." +
                                " Received error " + e.getMessage());
                    }
                });

        return properties;
    }

    @Override
    public void processRunModeInstructions(RedistributionInstructions redistributionInstructions, T runMode) {
        if (redistributionInstructions == null || runMode == null) {
            throw new IllegalArgumentException("Rebalance instructions and run mode must not be null.");
        }

        Map<String, String> runModeProperties = redistributionInstructions.getRunModeProperties();
        if (runModeProperties == null || runModeProperties.isEmpty()) {
            return;
        }

        Arrays.stream(runMode.getClass().getDeclaredMethods())
                .filter(method -> runModeProperties.containsKey(Configuration.propertyFromMethod(method.getName())))
                .filter(method -> method.isAnnotationPresent(ConfigArgSet.class))
                .forEach(method -> {
                    String property = Configuration.propertyFromMethod(method.getName());
                    LOG.info("[Agent] Setting property " + property + " to " + runModeProperties.get(property));

                    try {
                        method.invoke(runMode, runModeProperties.get(property));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        LOG.warn("[Agent] Property " + property + " could not be set to " + runModeProperties.get(property));
                    }

                });
    }
}
