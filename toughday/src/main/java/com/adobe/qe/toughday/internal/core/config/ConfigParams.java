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
    private Map<String, Object> distributedConfigParams = new HashMap<>();
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

    public void setDistributedConfigParams(Map<String, Object> distributedConfigParams) {
        this.distributedConfigParams = distributedConfigParams;
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

        if (!properties.containsKey("name")) {
            properties.put("name", "phase" + (phasesParams.size() + 1));
        }

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

    public Map<String, Object> getDistributedConfigParams() { return distributedConfigParams; }

    public Map<String, Object> getPublishModeParams() { return publishModeParams; }

    public Map<String, Object> getRunModeParams() { return runModeParams; }

    public void merge(ConfigParams other) {
        globalParams.putAll(other.getGlobalParams());
        distributedConfigParams.putAll(other.distributedConfigParams);
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
