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

import com.adobe.qe.toughday.internal.core.config.ConfigParams;

import java.util.List;
import java.util.Map;

public class YamlConfiguration {

    private ConfigParams configParams = new ConfigParams();

    public void setGlobals(Map<String, Object> globals) {
        this.configParams.setGlobalParams(globals);
    }

    public void setPublishmode(Map<String, Object> publishModeProperties) {
        configParams.setPublishModeParams(publishModeProperties);
    }

    public void setRunmode(Map<String, Object> runmode) {
        configParams.setRunModeParams(runmode);
    }

    public void setTests(List<YamlParseAction> tests) {
        for(YamlParseAction yamlParseAction : tests) {
            yamlParseAction.getAction().apply(configParams, yamlParseAction.getIdentifier(), yamlParseAction.getProperties());
        }
    }

    public void setPublishers(List<YamlParseAction> publishers) {
        for(YamlParseAction yamlParseAction : publishers) {
            yamlParseAction.getAction().apply(configParams, yamlParseAction.getIdentifier(), yamlParseAction.getProperties());
        }
    }

    public void setMetrics(List<YamlParseAction> metrics) {
        for (YamlParseAction yamlParseAction : metrics) {
            yamlParseAction.getAction().apply(configParams, yamlParseAction.getIdentifier(), yamlParseAction.getProperties());
        }
    }

    public void setExtensions(List<YamlParseAction> extensions) {
        for (YamlParseAction yamlAction : extensions) {
            yamlAction.getAction().apply(configParams, yamlAction.getIdentifier(), yamlAction.getProperties());
        }
    }

    public ConfigParams getConfigParams() {
        return configParams;
    }
}
