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
import com.sun.org.apache.xpath.internal.operations.String;

public class MockFeederTest3 extends SequentialTest {

    private InputFeeder optionalNoopReplacementInputFeeder1;
    private InputFeeder<String> optionalNoopReplacementInputFeeder2;
    private InputFeeder optionalNoReplacementInputFeeder;


    @FeederSet(required = false, allowNoopReplacement = true)
    public void setOptionalNoopReplacementInputFeeder1(InputFeeder optionalNoopReplacementInputFeeder) {
        this.optionalNoopReplacementInputFeeder1 = optionalNoopReplacementInputFeeder;
    }

    @FeederSet(required = false, allowNoopReplacement = true)
    public void setOptionalNoopReplacementInputFeeder2(InputFeeder<String> optionalNoopReplacementInputFeeder) {
        this.optionalNoopReplacementInputFeeder2 = optionalNoopReplacementInputFeeder;
    }


    @FeederSet(required = false, allowNoopReplacement = false)
    public void setOptionalNoReplacementInputFeeder(InputFeeder optionalNoReplacementInputFeeder) {
        this.optionalNoReplacementInputFeeder = optionalNoReplacementInputFeeder;
    }

    public InputFeeder getOptionalNoopReplacementInputFeeder1() {
        return optionalNoopReplacementInputFeeder1;
    }

    public InputFeeder<String> getOptionalNoopReplacementInputFeeder2() {
        return optionalNoopReplacementInputFeeder2;
    }

    public InputFeeder getOptionalNoReplacementInputFeeder() {
        return optionalNoReplacementInputFeeder;
    }

    @Override
    public void test() throws Throwable {
        optionalNoopReplacementInputFeeder1.get();
        optionalNoopReplacementInputFeeder2.get();
    }

    @Override
    public AbstractTest newInstance() {
        return null;
    }
}
