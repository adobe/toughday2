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

import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Actions;
import com.adobe.qe.toughday.internal.core.Timestamp;
import com.adobe.qe.toughday.internal.core.config.ConfigParams;
import com.adobe.qe.toughday.internal.core.config.PhaseParams;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GenerateYamlConfiguration {

    private ConfigParams configParams;
    private Map<String, Class> itemsIdentifiers;
    private java.util.List<YamlDumpAction> yamlPublisherActions;
    private List<YamlDumpAction> yamlMetricActions;
    private List<YamlDumpAction> yamlExtensionActions;
    private List<YamlDumpAction> yamlTestActions;
    private List<YamlDumpPhase> yamlDumpPhases;

    private static final String DEFAULT_YAML_CONFIGURATION_FILENAME = "toughday_";
    private static final String DEFAULT_YAML_EXTENSION = ".yaml";
    private static final String TIMESTAMP = Timestamp.START_TIME;
    public static final String yamlConfigFilename = DEFAULT_YAML_CONFIGURATION_FILENAME + TIMESTAMP + DEFAULT_YAML_EXTENSION;

    public GenerateYamlConfiguration(ConfigParams configParams, Map<String, Class> items) {
        this.configParams = configParams;
        this.itemsIdentifiers = items;
        yamlPublisherActions = new ArrayList<>();
        yamlMetricActions = new ArrayList<>();
        yamlExtensionActions = new ArrayList<>();
        yamlDumpPhases = new ArrayList<>();
        yamlTestActions = new ArrayList<>();
        createActionsForItems();
    }

    public Map<String, Object> getGlobals() {
        Map<String, Object> globals = configParams.getGlobalParams();
        globals.remove("configfile");
        return globals;
    }

    public Map<String, Object> getDistributedConfig() {
        Map<String, Object> distributedConfigParms = configParams.getDistributedConfigParams();
        // remove this so that the driver won't try to trigger the execution
        distributedConfigParms.remove("driverip");
        return distributedConfigParms;
    }

    public Map<String, Object> getPublishmode() {
        return configParams.getPublishModeParams();
    }

    public Map<String, Object> getRunmode() {
        return configParams.getRunModeParams();
    }

    public List<YamlDumpAction> getTests() {
        return yamlTestActions;
    }

    public List<YamlDumpAction> getPublishers() {
        return yamlPublisherActions;
    }

    public List<YamlDumpAction> getMetrics() {
        return yamlMetricActions;
    }

    public List<YamlDumpAction> getExtensions() { return yamlExtensionActions; }

    public List<YamlDumpPhase> getPhases() {
        if (configParams.getPhasesParams().isEmpty()) {
            return new ArrayList<>();
        }

        return yamlDumpPhases;
    }

    // creates a list of actions for each item(tests, publishers, metrics, extensions)
    private void createActionsForItems() {

        for (Map.Entry<Actions, ConfigParams.MetaObject> item : configParams.getItems()) {
            chooseAction(item, 0);
        }

        List<PhaseParams> phasesParams = configParams.getPhasesParams();
        for (int i = 0; i < phasesParams.size(); ++i) {
            PhaseParams phaseParams = phasesParams.get(i);
            YamlDumpPhase yamlDumpPhase = new YamlDumpPhase(phaseParams.getProperties(), phaseParams.getRunmode(), phaseParams.getPublishmode());
            yamlDumpPhases.add(yamlDumpPhase);

            for (Map.Entry<Actions, ConfigParams.MetaObject> item : phasesParams.get(i).getItems()) {
                chooseAction(item, i + 1);
            }
        }
    }

    private void chooseAction(Map.Entry<Actions, ConfigParams.MetaObject> item, int index) {
        switch (item.getKey()) {
            case ADD:
                addAction((ConfigParams.ClassMetaObject)item.getValue(), index);
                break;
            case CONFIG:
                configAction((ConfigParams.NamedMetaObject)item.getValue(), index);
                break;
            case EXCLUDE:
                excludeAction(((ConfigParams.NamedMetaObject)item.getValue()).getName(), index);
                break;
        }
    }

    private void addAction(ConfigParams.ClassMetaObject item, int index) {
        YamlDumpAddAction addAction = new YamlDumpAddAction(item.getClassName(), item.getParameters());
        if (ReflectionsContainer.getInstance().isTestClass(item.getClassName())) {
            if (index == 0) {
                yamlTestActions.add(addAction);
                return;
            }

            yamlDumpPhases.get(index - 1).getTests().add(addAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(item.getClassName())) {
            if (index == 0) {
                yamlPublisherActions.add(addAction);
                return;
            }

            yamlDumpPhases.get(index - 1).getPublishers().add(addAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(item.getClassName())
                || item.getClassName().equals("BASICMetrics") || item.getClassName().equals("DEFAULTMetrics")){
            if (index == 0) {
                yamlMetricActions.add(addAction);
                return;
            }

            yamlDumpPhases.get(index - 1).getMetrics().add(addAction);
        } else if (item.getClassName().endsWith(".jar")) {
            yamlExtensionActions.add(addAction);
        }
    }

    private void configAction(ConfigParams.NamedMetaObject item, int index) {
        YamlDumpConfigAction configAction = new YamlDumpConfigAction(item.getName(), item.getParameters());
        if (ReflectionsContainer.getInstance().isTestClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            if (index == 0) {
                yamlTestActions.add(configAction);
                return;
            }

            yamlDumpPhases.get(index - 1).getTests().add(configAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            yamlPublisherActions.add(configAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            yamlMetricActions.add(configAction);
        }
    }

    private void excludeAction(String item, int index) {
        YamlDumpExcludeAction excludeAction = new YamlDumpExcludeAction(item);
        if (ReflectionsContainer.getInstance().isTestClass(itemsIdentifiers.get(item).getSimpleName())) {
            if (index == 0) {
                yamlTestActions.add(excludeAction);
                return;
            }

            yamlDumpPhases.get(index - 1).getTests().add(excludeAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemsIdentifiers.get(item).getSimpleName())) {
            yamlPublisherActions.add(excludeAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemsIdentifiers.get(item).getSimpleName())) {
            yamlMetricActions.add(excludeAction);
        }
    }

    public String createYamlStringRepresentation() {
        Yaml yaml = YamlBuilder.getYamlInstance();

        return yaml.dump(this);
    }

    /**
     * Creates a YAML configuration file.
     */
    public void createYamlConfigurationFile() {
        String yamlStringRepresentation = createYamlStringRepresentation();

        try {
            FileWriter fileWriter = new FileWriter(yamlConfigFilename);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(yamlStringRepresentation);
            bufferedWriter.flush();

            fileWriter.close();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}