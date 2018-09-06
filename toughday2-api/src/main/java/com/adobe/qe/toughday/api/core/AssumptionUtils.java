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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Class with helpers to validate assumptions about the extensions
 */
public final class AssumptionUtils {
    /**
     * Validator for methods annotated with test_annotations.
     * @param method
     * @param annotation
     * @return true if the method is valid, false if not.
     */
    public static void validateAnnotatedMethod(Method method, Class<? extends Annotation> annotation) {
        if(method.getParameterTypes().length != 0) {
            throw new AssertionError("Method \"" + method + "\" annotated with " + annotation.getSimpleName() + " cannot have parameters");
        }

        if(!(Modifier.isFinal(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()))) {
            throw new AssertionError("Method \"" + method + "\" annotated with " + annotation.getSimpleName() + " must be final or private");
        }
    }
}
