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
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ListFeeder extends NamedObjectImpl implements InputFeeder<String> {

    private List<String> values = new ArrayList<>();
    private AtomicInteger idx = new AtomicInteger(0);

    public void ListFeeder() {}

    public void ListFeeder(List<String> values) {
        this.values = values;
    }

    @ConfigArgSet(required = true, desc = "Comma separated values, to be returned by the feeder")
    public void setValues(String values) {
        this.values = Arrays.asList(values.split(","));
    }

    @ConfigArgGet
    public String getValues() {
        return Joiner.on(",").join(values);
    }

    @Override
    public String get() throws Exception {
        return values.get(idx.getAndUpdate(i -> (i + 1) % values.size()));
    }

    @Override
    public void init() throws Exception {

    }
}
