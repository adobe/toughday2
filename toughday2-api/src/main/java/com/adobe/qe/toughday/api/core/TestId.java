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
package com.adobe.qe.toughday.api.core;

/**
 * Identifies a test in the suite and all its clones
 */
public abstract class TestId {
    /**
     * Mandatory equals method.
     * @param testId the other TestId instance
     * @return true if they are equals. false otherwise
     */
    public abstract boolean equals(TestId testId);

    /**
     * Mandatory method for computing the hash code
     * @return the hash code
     */
    public abstract long getHashCode();

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof TestId)) return false;
        return equals((TestId) other);
    }

    @Override
    public int hashCode() {
        return (int) getHashCode();
    }
}
