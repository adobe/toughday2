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
package com.adobe.qe.toughday.internal.core.benckmark;

import com.adobe.qe.toughday.api.core.benchmark.ResultInfo;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;

public class ImmutableResultInfo<R, K> implements ResultInfo<R, K> {
    private final TestResult<K> testResult;
    private final R returnValue;
    private final Throwable throwable;

    public ImmutableResultInfo(TestResult<K> testResult, R returnValue, Throwable throwable) {
        this.testResult = testResult;
        this.returnValue = returnValue;
        this.throwable = throwable;
    }

    @Override
    public TestResult<K> getTestResult() {
        return testResult;
    }

    @Override
    public R getReturnValue() {
        return returnValue;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }
}
