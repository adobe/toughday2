/*
Copyright 2018 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.internal.core.config;

import com.adobe.qe.toughday.api.annotations.feeders.FeederSet;
import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;
import com.adobe.qe.toughday.feeders.NoopFeeder;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

public class FeederInjector {

    public static <T> void injectFeeder(Method method, T object, Map<String, Object> args, boolean applyDefaults, Map<String, Feeder> feederContext) throws InvocationTargetException, IllegalAccessException {

        if (method.getAnnotation(FeederSet.class) == null) {
            //This method is not a feeder inject
            return;
        }

        FeederSet feederSetAnnotation = method.getAnnotation(FeederSet.class);
        String property = Configuration.propertyFromMethod(method.getName());
        Object value = args.remove(property);

        if (value == null) {
            //No configuration for this feeder inject method found
            if(feederSetAnnotation.required()) {
                throw new IllegalStateException("Feeder \"" + property + "\" is required for class " + object.getClass().getSimpleName());
            }
            if(feederSetAnnotation.allowNoopReplacement()) {
               method.invoke(object, NoopFeeder.INSTANCE);
            }
            return;
        }

        String feederName = value.toString();
        Feeder feeder = feederContext.get(feederName);
        assertion(feeder != null, "Cloud not configure object of class: " + object.getClass() + ". Cloud not bind feeder for: " + property + ". Feeder not found, or not yet declared.");

        if (InputFeeder.class.isAssignableFrom(method.getParameterTypes()[0])) {
            assertion(InputFeeder.class.isInstance(feeder),
                    "Cloud not configure object of class: " + object.getClass() + ". Cloud not bind feeder for: " + property + ". Expected an input feeder, received an output feeder.");
            Type parameterType = method.getGenericParameterTypes()[0];
            if (!parameterType.equals(InputFeeder.class)) {
                Type expected = TypeResolver.resolveGenericType(InputFeeder.class, method.getGenericParameterTypes()[0]);
                Type actual = TypeResolver.resolveGenericType(InputFeeder.class, feeder.getClass());
                assertion(expected.equals(actual),
                        "Cloud not configure object of class: " + object.getClass() + ". Cloud not bind feeder for: " + property + ". Type mismatch. Expected: " + expected + ", but received: " + actual);
            }
        } else if (OutputFeeder.class.isAssignableFrom(method.getParameterTypes()[0])) {
            assertion(OutputFeeder.class.isInstance(feeder),
                    "Cloud not configure object of class: " + object.getClass() + ". Cloud not bind feeder for: " + property + ". Expected an output feeder, received an input feeder.");

            Type parameterType = method.getGenericParameterTypes()[0];
            if (!parameterType.equals(OutputFeeder.class)) {
                Type expected = TypeResolver.resolveGenericType(OutputFeeder.class, method.getGenericParameterTypes()[0]);
                Type actual = TypeResolver.resolveGenericType(OutputFeeder.class, feeder.getClass());
                assertion(expected.equals(actual),
                        "Cloud not configure object of class: " + object.getClass() + ". Cloud not bind feeder for: " + property + ". Type mismatch. Expected: " + expected + ", but received: " + actual);
            }
        }

        method.invoke(object, feeder);
    }

    private static void assertion(boolean value, String message) {
        if(value) {
            return;
        }
        throw new IllegalStateException(message);
    }
}
