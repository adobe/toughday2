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
package com.adobe.qe.toughday.internal.core.config;

import java.util.Map;

/**
 * Created by tuicu on 27/12/16.
 */
public enum Actions implements ConfigAction {
    ADD {
        @Override
        public String value() {
            return ADD_ACTION;
        }

        @Override
        public void apply(ConfigParams configParams, String identifier, Map<String, Object> metaInfo) {
            configParams.addItem(identifier, metaInfo);
        }

        @Override
        public String actionParams() {
            return "ExtensionJar | [f.q.d.n]TestClass/[f.q.d.n]PublisherClass/[f.q.d.n]MetricClass/[f.q.d.n]FeederClass property1=val property2=val";
        }

        @Override
        public String actionDescription() {
            return "Add an extension or a test to the suite or a publisher/metric/feeder. For test, publisher, metric and feeder it can be used the fully qualified domain name which represents the package's name.";
        }
    },
    CONFIG {
        @Override
        public String value() {
            return CONFIG_ACTION;
        }

        @Override
        public void apply(ConfigParams configParams, String identifier, Map<String, Object> metaInfo) {
            configParams.configItem(identifier, metaInfo);
        }

        @Override
        public String actionParams() {
            return "TestName/PublisherName/MetricName/FeederName property1=val property2=val";
        }

        @Override
        public String actionDescription() {
            return "Override parameters for a test/publisher/metric/feeder from config file or a predefined suite";
        }
    },
    EXCLUDE {
        @Override
        public String value() {
            return EXCLUDE_ACTION;
        }

        @Override
        public void apply(ConfigParams configParams, String identifier, Map<String, Object> metaInfo) {
            if (metaInfo != null && metaInfo.size() != 0) {
                throw new IllegalArgumentException("--exclude cannot have properties for identifier: " + identifier);
            }

            configParams.excludeItem(identifier);
        }

        @Override
        public String actionParams() {
            return "TestName/PublisherName/MetricName/FeederName";
        }

        @Override
        public String actionDescription() {
            return "Exclude a test/publisher/metric/feeder from config file or a predefined suite.";
        }
    };

    public static Actions fromString(String actionString) {
        for(Actions action : Actions.values()) {
            if (action.value().equals(actionString))
                return action;
        }
        throw new IllegalStateException("There's no \"" + actionString + "\" action");
    }

    public static boolean isAction(String actionString) {
        for(Actions actions : Actions.values()) {
            if (actions.value().equals(actionString)) {
                return true;
            }
        }

        return false;
    }

    private static final String ADD_ACTION = "add";
    private static final String CONFIG_ACTION = "config";
    private static final String EXCLUDE_ACTION = "exclude";
}