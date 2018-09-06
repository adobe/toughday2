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
package com.adobe.qe.toughday.mocks;

import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.util.List;

@Internal
public class MockTest extends AbstractTest {
    private static Logger logger;

    @Override
    public List<AbstractTest> getChildren() {
        return null;
    }

    @Override
    public Class<? extends AbstractTestRunner> getTestRunnerClass() {
        return null;
    }

    @Override
    public AbstractTest newInstance() {
        return null;
    }

    @Override
    public Logger logger() {
        return getLogger();
    }

    public static Logger getLogger() {
        if (logger == null) {
            logger = new SimpleLogger("null", Level.INFO, false, false,
                    false, false, null,
                    null, new PropertiesUtil("."), null);
        }

        return logger;
    }
}
