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
package com.adobe.qe.toughday.feeders;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.NamedObjectImpl;
import com.adobe.qe.toughday.api.feeders.InputFeeder;

public class StringValueFeeder extends NamedObjectImpl implements InputFeeder<String> {
    private String value;

    public StringValueFeeder() {}

    public StringValueFeeder(String value) {
        this.value = value;
    }

    @ConfigArgSet(required = true,desc = "The value which will be return")
    public void setValue(String value) {
        this.value = value;
    }

    @ConfigArgGet
    public String getValue() {
        return this.value;
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public String get() throws Exception {
        return this.value;
    }
}
