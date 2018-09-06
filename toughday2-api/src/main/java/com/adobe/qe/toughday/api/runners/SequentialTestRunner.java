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
package com.adobe.qe.toughday.api.runners;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.api.core.SequentialTest;

/**
 * Runner for a sequential test.
 */
public class SequentialTestRunner extends AbstractTestRunner<SequentialTest> {
    public SequentialTestRunner(Class<? extends AbstractTest> testClass) {
        super(testClass);
    }

    @Override
    protected void run(SequentialTest testObject, RunMap runMap) throws Throwable {
            testObject.benchmark().measure(testObject, ()->{
                testObject.test();
            });
    }
}
