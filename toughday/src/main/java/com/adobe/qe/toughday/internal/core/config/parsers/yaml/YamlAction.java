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
package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import com.adobe.qe.toughday.internal.core.config.Actions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tuicu on 27/12/16.
 */
public class YamlAction {
    private Actions action;
    private String identifier;
    private Map<String, String> testMetaInfo = new HashMap<>();

    public void setAdd(String identifier) {
        this.action = Actions.ADD;
        this.identifier = identifier;
    }

    public void setConfig(String identifier) {
        this.action = Actions.CONFIG;
        this.identifier = identifier;
    }

    public void setExclude(String identifier) {
        this.action = Actions.EXCLUDE;
        this.identifier = identifier;
    }

    public void setProperties(Map<String, String> testMetaInfo) {
        this.testMetaInfo = testMetaInfo;
    }

    public Actions getAction() {
        return action;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getProperties() {
        return testMetaInfo;
    }

    @Override
    public String toString() {
        return action.value() + " " + identifier;
    }
}
