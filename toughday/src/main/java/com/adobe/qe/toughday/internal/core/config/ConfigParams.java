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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * An object that has the configuration params parsed, but still in String form.
 * What a ConfigurationParser returns and is used by Configuration to instantiate all concrete objects.
 */
public class ConfigParams implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger(ConfigParams.class);

    private List<PhaseParams> phasesParams = new ArrayList<>();
    private Map<String, Object> globalParams = new HashMap<>();
    private Map<String, Object> publishModeParams = new HashMap<>();
    private Map<String, Object> runModeParams = new HashMap<>();
    private List<Map.Entry<Actions, MetaObject>> items = new ArrayList<>();

    private boolean globalLevel = true;

    public static class MetaObject  implements Serializable {
        private Map<String, Object> parameters;

        public MetaObject(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }

    public static class ClassMetaObject extends MetaObject {
        private String className;

        public ClassMetaObject(String className, Map<String, Object> parameters) {
            super(parameters);
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    public static class NamedMetaObject extends MetaObject {
        private String name;

        public NamedMetaObject(String name, Map<String, Object> parameters) {
            super(parameters);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class PhaseParams implements Serializable {
        public static Map<String, PhaseParams> namedPhases = new HashMap<>();
        private Map<String, Object> properties = new HashMap<>();
        private Map<String, Object> runmode = new HashMap<>();
        private Map<String, Object> publishmode = new HashMap<>();
        private List<Map.Entry<Actions, MetaObject>> items = new ArrayList<>();

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

        public List<Map.Entry<Actions, MetaObject>> getItems() {
            return items;
        }

        public void setItems(List<Map.Entry<Actions, MetaObject>> tests) {
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

            Map<String, Object> props = deepClone(phaseParams.properties);
            props.remove("name");
            props.putAll(this.properties);
            this.properties = props;
            properties.remove("useconfig");

            // take the run mode configuration
            if (runmode.isEmpty()) {
                this.runmode = phaseParams.runmode;
            }

            List<Map.Entry<Actions, MetaObject>> tests = deepClone(phaseParams.items);
            tests.addAll(this.items);

            this.items = tests;
        }
    }

    /** Creates a copy of the object.
     * @param object
     * @return
     */
    public static<T> T deepClone(Object object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
            oos.writeObject(object);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
            return (T) ois.readObject();
        }
        catch (Exception e) {
            LOGGER.error(e);
            return null;
        }
    }


    public void setGlobalParams(Map<String, Object> globalParams) {
        this.globalParams = globalParams;
    }

    public void setPhasesParams(List<PhaseParams> phasesParams) {
        this.phasesParams = phasesParams;
    }

    public void setPublishModeParams(Map<String, Object> publishModeParams) {
        if (!globalLevel) {
            phasesParams.get(phasesParams.size() - 1).setPublishmode(publishModeParams);
        } else {
            this.publishModeParams = publishModeParams;
        }
    }

    public void setRunModeParams(Map<String, Object> runModeParams) {
        if (!globalLevel) {
            phasesParams.get(phasesParams.size() - 1).setRunmode(runModeParams);
        } else {
            this.runModeParams = runModeParams;
        }
    }

    public void createPhasewWithProperties(Map<String, Object> properties) {
        PhaseParams phase = new PhaseParams();
        phase.setProperties(properties);
        phasesParams.add(phase);
    }

    public void addItem(String itemName, Map<String, Object> params) {
        Map.Entry<Actions, MetaObject> newEntry = new AbstractMap.SimpleEntry<>(Actions.ADD,
                new ClassMetaObject(itemName, params));

        addToItemsOrLastPhase(newEntry);
    }

    public void configItem(String itemName, Map<String, Object> params) {
        Map.Entry<Actions, MetaObject> newEntry = new AbstractMap.SimpleEntry<>(Actions.CONFIG,
                new NamedMetaObject(itemName, params));

        addToItemsOrLastPhase(newEntry);
    }

    public void excludeItem(String itemName) {
        Map.Entry<Actions, MetaObject> newEntry = new AbstractMap.SimpleEntry<>(Actions.EXCLUDE,
                new NamedMetaObject(itemName, null));

        addToItemsOrLastPhase(newEntry);
    }

    private void addToItemsOrLastPhase(Map.Entry<Actions, MetaObject> newEntry) {
        if (!globalLevel) {
            phasesParams.get(phasesParams.size() - 1).getItems().add(newEntry);
        } else {
            items.add(newEntry);
        }
    }

    public List<PhaseParams> getPhasesParams() {
        return phasesParams;
    }

    public Map<String, Object> getGlobalParams(){
        return globalParams;
    }

    public Map<String, Object> getPublishModeParams() { return publishModeParams; }

    public Map<String, Object> getRunModeParams() { return runModeParams; }

    public void merge(ConfigParams other) {
        globalParams.putAll(other.getGlobalParams());
        items.addAll(other.items);
        phasesParams.addAll(other.phasesParams);

        if(other.runModeParams.containsKey("type"))
            this.runModeParams.clear();
        this.runModeParams.putAll(other.runModeParams);

        if(other.publishModeParams.containsKey("type"))
            this.publishModeParams.clear();
        this.publishModeParams.putAll(other.publishModeParams);
    }

    public List<Map.Entry<Actions, MetaObject>> getItems() {
        return items;
    }

    public boolean isGlobalLevel() {
        return globalLevel;
    }

    public void setGlobalLevel(boolean globalLevel) {
        this.globalLevel = globalLevel;
    }
}
