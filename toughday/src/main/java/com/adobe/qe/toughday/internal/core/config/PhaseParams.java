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

import java.io.Serializable;
import java.util.*;

public class PhaseParams implements Serializable {
    public static Map<String, PhaseParams> namedPhases = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, Object> runmode = new HashMap<>();
    private Map<String, Object> publishmode = new HashMap<>();
    private List<Map.Entry<Actions, ConfigParams.MetaObject>> items = new ArrayList<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getRunmode() {
        return runmode;
    }

    public void setRunmode(Map<String, Object> runmode) {
        this.runmode = runmode;
    }

    public List<Map.Entry<Actions, ConfigParams.MetaObject>> getItems() {
        return items;
    }

    public void setItems(List<Map.Entry<Actions, ConfigParams.MetaObject>> tests) {
        this.items = tests;
    }

    public Map<String, Object> getPublishmode() {
        return publishmode;
    }

    public void setPublishmode(Map<String, Object> publishmode) {
        this.publishmode = publishmode;
    }

    /**
     * Method for taking the configuration of another phase
     *
     * @param phaseParams - the parameters of the phase to take the configuration from
     * In case the phase corresponding to phaseParams also has "useconfig" set,
     * the method will be called recursively for phaseParams in order to try to
     * take the configuration from its own "useconfig". The process repeats until
     * a phase that does not have "useconfig" set is found. Once it is found, the
     * configuration is taken over by the current phase and the process returns to the last
     * recursive call, when the configuration is again taken from the phase that was
     * previously configured, and so on.
     *
     * "useconfig" will be removed from the phase once it has been computed
     *
     * It also detects possible loops caused by the "useconfig" functionality
     * @param checked - a set with the names of the phases we passed through since the first call
     */
    public void merge(PhaseParams phaseParams, Set<String> checked) {
        String useconfig = phaseParams.properties.get("useconfig") != null ? phaseParams.getProperties().get("useconfig").toString() : null;

        if (useconfig != null && !namedPhases.containsKey(useconfig)) {
            throw new IllegalArgumentException("Could not find phase named \"" +
                    phaseParams.getProperties().get("useconfig") + "\".");
        } else if (useconfig != null) {
            // if the phase params from which the configuration is to be taken also has
            // "useconfig" set, then call the method recursively for @phaseParams
            if (checked.contains(useconfig)) {
                throw new IllegalArgumentException("Trying to use the configuration of another phase results in loop.");
            }

            checked.add(useconfig);
            phaseParams.merge(namedPhases.get(useconfig), checked);
        }

        Map<String, Object> props = ConfigParams.deepClone(phaseParams.properties);
        props.remove("name");
        props.putAll(this.properties);
        this.properties = props;
        properties.remove("useconfig");

        // take the run mode configuration
        if (runmode.isEmpty()) {
            this.runmode = phaseParams.runmode;
        }

        List<Map.Entry<Actions, ConfigParams.MetaObject>> tests = ConfigParams.deepClone(phaseParams.items);
        tests.addAll(this.items);

        this.items = tests;
    }
}
