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

import java.util.Map;

/**
 * Specifies how the add action is dumped when generating a yaml configuration file.
 */
public class YamlDumpAddAction extends YamlDumpAction {

    public YamlDumpAddAction(String identifier, Map<String, Object> properties) {
        this.action = Actions.ADD;
        this.identifier = identifier;
        this.properties = properties;
    }

    public String getAdd() {
        return identifier;
    }

}