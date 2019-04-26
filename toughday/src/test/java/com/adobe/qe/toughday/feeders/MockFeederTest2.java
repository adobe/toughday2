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

public class MockFeederTest2 extends SequentialTest {

    private InputFeeder requiredInputFeeder;

    @FeederSet(required = true)
    public void setRequiredInputFeeder(InputFeeder requiredInputFeeder) {
        this.requiredInputFeeder = requiredInputFeeder;
    }


    @Override
    public void test() throws Throwable {

    }

    @Override
    public AbstractTest newInstance() {
        return null;
    }
}
