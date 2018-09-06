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

import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import com.adobe.qe.toughday.api.core.TestId;
import com.adobe.qe.toughday.internal.core.UUIDTestId;
import com.adobe.qe.toughday.api.annotations.labels.NotNull;
import com.adobe.qe.toughday.api.annotations.labels.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is an ad hoc test instance, usually resulted from calling Benchmark.measure(...)
 * This is not an actual test. It can't be rerun. It's merely a dummy object that is used to have an AbstractTest
 * representation of a arbitrary code execution/method call.
 */
@Internal
public final class AdHocTest extends AbstractTest {
    private static final List<AbstractTest> EMPTY_ARRAY = new ArrayList<>();

    private static class AdHocTestId extends TestId {
        private AbstractTest parent;
        private String name;
        private long nameHash;

        public AdHocTestId(AbstractTest parent, String name) {
            this.parent = parent;
            this.name = name;
        }


        @Override
        public boolean equals(TestId testId) {
            if(this == testId)
                return true;

            if(!(testId instanceof AdHocTestId))
                return false;

            AdHocTestId adHocTestId = (AdHocTestId) testId;
            if(!parent.getId().equals((adHocTestId.parent.getId())))
                return false;

            if(nameHash != 0 && adHocTestId.nameHash != 0 && nameHash != adHocTestId.nameHash)
                return false;

            return name.equals(adHocTestId.name);
        }

        @Override
        public long getHashCode() {
            long h = nameHash;

            if (h == 0 && name.length() > 0) {
                for (int i = 0; i < name.length(); ++i) {
                    h = 31 * h + name.charAt(i);
                }
                nameHash = h;
            }
            return h * parent.getId().getHashCode();
        }
    }

    private void init(TestId id, AbstractTest parent, String name) {
        assert id != null : "The ID must not be null";
        assert parent != null : "The parent must not be null";
        assert name != null : "The name must not be null";
        setID(id);
        setParent(parent);
        setName(name);
    }

    public AdHocTest(@NotNull TestId id, @NotNull AbstractTest parent, @NotNull String name) {
        init(id, parent, name);
    }

    /**
     * Constructor
     * @param id the id of the test
     * @param parent the parent  test
     * @param name the name of the test
     */
    public AdHocTest(@Nullable UUID id, @NotNull AbstractTest parent, @NotNull String name) {
        if(id != null) {
            init(new UUIDTestId(id), parent, name);
        } else {
            init(new AdHocTestId(parent, name), parent, name);
        }
    }

    /**
     * Constructor
     * @param parent the parent  test
     * @param name the name of the test
     */
    public AdHocTest(@NotNull AbstractTest parent, @NotNull String name) {
        this(new AdHocTestId(parent, name), parent, name);
    }

    /**
     * Dummy {@code getChildren()} implementation
     * @return an empty list
     */
    @Override
    public List<AbstractTest> getChildren() {
        return EMPTY_ARRAY;
    }

    /**
     * These tests don't have a runner, as they are ad hoc executions. They are not meant to be run
     * @return {@code null}
     */
    @Override
    public Class<? extends AbstractTestRunner> getTestRunnerClass() {
        return null;
    }

    /**
     * Creates a new instance with the same id, parent and name.
     * @return a new instance with the same id, parent and name.
     */
    @Override
    public AbstractTest newInstance() {
        return new AdHocTest(getId(), getParent(), getName());
    }
}
