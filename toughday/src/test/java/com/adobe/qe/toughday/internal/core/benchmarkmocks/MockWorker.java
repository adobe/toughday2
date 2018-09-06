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
package com.adobe.qe.toughday.internal.core.benchmarkmocks;

import java.util.ArrayList;
import java.util.List;

public class MockWorker {
    public static final String METHOD1 = "Method1";
    public static final String METHOD2 = "Method2";
    public static final String METHOD3 = "Method3";
    public static final String METHOD4 = "Method4";
    public static final String METHOD5 = "Method5";
    public static final String METHOD6 = "Method6";
    public static final String RETURN_VALUE = "ReturnValue";

    private String calledMethod;
    private List arguments = new ArrayList<>();
    private Throwable fail;
    private Throwable skip;
    private boolean interrupt;

    public void method(String s) throws Throwable {
        calledMethod = METHOD1;
        arguments.add(s);
        Thread.sleep(10);
        doThrow();
    }

    public void method(String s, int i) throws Throwable {
        calledMethod = METHOD2;
        arguments.add(s);
        arguments.add(i);
        Thread.sleep(30);
        doThrow();
    }

    public void method(String s, long x, int... args) throws Throwable {
        calledMethod = METHOD3;
        arguments.add(s);
        arguments.add(x);
        for(int arg : args) {
            arguments.add(arg);
        }
        Thread.sleep(50);
        doThrow();
    }

    public void method(int... args) throws Throwable {
        calledMethod = METHOD4;
        for(int arg : args) {
            arguments.add(arg);
        }
        Thread.sleep(70);
        doThrow();
    }

    public void method() throws Throwable {
        calledMethod = METHOD5;
        Thread.sleep(90);
        doThrow();
    }

    public Object methodWithReturnValue(Long... args) throws Throwable {
        calledMethod = METHOD6;
        for(Long arg : args) {
            arguments.add(arg);
        }
        Thread.sleep(110);
        doThrow();
        return RETURN_VALUE;
    }

    public String getCalledMethod() { return calledMethod; }

    public List getArguments() { return arguments; }

    public void fail(Throwable fail) { this.fail = fail; }

    public void skip(Throwable skip) { this.skip = skip; }

    public void interrupt() {
        this.interrupt = true;
    }

    private void doThrow() throws Throwable {
        if (this.fail != null) throw fail;
        if (this.skip != null) throw skip;
        if(this.interrupt) Thread.currentThread().interrupt();
    }
}
