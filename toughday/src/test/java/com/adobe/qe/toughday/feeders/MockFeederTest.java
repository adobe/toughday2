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

import com.adobe.qe.toughday.api.annotations.feeders.FeederSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.SequentialTest;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;

import java.util.List;
import java.util.Map;

public class MockFeederTest extends SequentialTest {

    private InputFeeder inputFeeder1;
    private InputFeeder<Map> inputFeeder2;
    private OutputFeeder outputFeeder1;
    private OutputFeeder<List> outputFeeder2;

    @FeederSet(required = false)
    public void setInputFeeder1(InputFeeder inputFeeder1) {
        this.inputFeeder1 = inputFeeder1;
    }

    public InputFeeder getInputFeeder1() {
        return inputFeeder1;
    }

    @FeederSet(required = false)
    public void setInputFeeder2(InputFeeder<Map> inputFeeder2) {
        this.inputFeeder2 = inputFeeder2;
    }

    public InputFeeder<Map> getInputFeeder2() {
        return inputFeeder2;
    }

    @FeederSet(required = false)
    public void setOutputFeeder1(OutputFeeder outputFeeder1) {
        this.outputFeeder1 = outputFeeder1;
    }

    public OutputFeeder getOutputFeeder1() {
        return outputFeeder1;
    }

    @FeederSet(required = false)
    public void setOutputFeeder2(OutputFeeder<List> outputFeeder2) {
        this.outputFeeder2 = outputFeeder2;
    }

    public OutputFeeder<List> getOutputFeeder2() {
        return outputFeeder2;
    }

    @Override
    public void test() throws Throwable {

    }

    @Override
    public AbstractTest newInstance() {
        return null;
    }
}
