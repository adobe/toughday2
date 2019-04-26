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

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.annotations.labels.NotNull;

/**
 * As Annotations from Interfaces are not inherited in Java, you'll have to add similar annotations to your implementation.\
 * If your implementation allows this, you can extend {@link NamedObjectImpl} for convenience
 */
public interface NamedObject {
    @ConfigArgGet
    @NotNull String getName();

    @ConfigArgSet(required = false, desc = "The name of this object")
    void setName(String name);
}
