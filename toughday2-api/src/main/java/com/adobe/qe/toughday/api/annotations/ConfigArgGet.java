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
package com.adobe.qe.toughday.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on a getter to expose it as a configuration property.
 * These properties will be automatically picked up, shown in logging or in "dry" runmodes
 * Supported classes: subtypes of AbstractTest, subtypes of Publisher and
 * GlobalArgs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface ConfigArgGet {
    String name() default "";

    /**
     * Use this field when you want to specify that a certain property must be taken into
     * consideration when redistributing the work between the agents running in the cluster.
     * The property will be automatically collected by the AbstractRunModeBalancer.
     * Currently, this annotations is supported only for classes implementing the RunMode
     * interface.
     * @return
     */
    boolean redistribute() default false;
}
