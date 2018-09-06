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
package com.adobe.qe.toughday.internal.core;

import com.adobe.qe.toughday.api.core.TestId;

import java.util.UUID;

public class UUIDTestId extends TestId {
    private UUID id;

    public UUIDTestId() {
        this(UUID.randomUUID());
    }

    public UUIDTestId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(TestId testId) {
        if(!(testId instanceof UUIDTestId))
            return false;

        return this.id.equals(((UUIDTestId) testId).id);
    }

    @Override
    public long getHashCode() {
        return id.hashCode();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
