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
import com.adobe.qe.toughday.internal.core.config.Configuration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateYamlConfiguration {

    private ConfigParams configParams;
    private Map<String, Class> itemsIdentifiers;
    private java.util.List<YamlDumpAction> yamlTestActions;
    private java.util.List<YamlDumpAction> yamlPublisherActions;
    private List<YamlDumpAction> yamlMetricActions;
    private List<YamlDumpAction> yamlExtensionActions;

    private static final String DEFAULT_YAML_CONFIGURATION_FILENAME = "toughday_";
    private static final String DEFAULT_YAML_EXTENSION = ".yaml";
    private static final String TIMESTAMP = Timestamp.START_TIME;

    public GenerateYamlConfiguration(ConfigParams configParams, Map<String, Class> items) {
        this.configParams = configParams;
        this.itemsIdentifiers = items;
        yamlTestActions = new ArrayList<>();
        yamlPublisherActions = new ArrayList<>();
        yamlMetricActions = new ArrayList<>();
        yamlExtensionActions = new ArrayList<>();
        createActionsForItems();
    }

    public Map<String, Object> getGlobals() {
        Map<String, Object> globals = configParams.getGlobalParams();
        globals.remove("configfile");
        return globals;
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

    // creates a list of actions for each item(tests, publishers, metrics, extensions)
    private void createActionsForItems() {
        for (Map.Entry<Actions, ConfigParams.MetaObject> item : configParams.getItems()) {
            switch (item.getKey()) {
                case ADD:
                    addAction((ConfigParams.ClassMetaObject)item.getValue());
                    break;
                case CONFIG:
                    configAction((ConfigParams.NamedMetaObject)item.getValue());
                    break;
                case EXCLUDE:
                    excludeAction(((ConfigParams.NamedMetaObject)item.getValue()).getName());
                    break;
            }
        }
    }

    private void addAction(ConfigParams.ClassMetaObject item) {
        YamlDumpAddAction addAction = new YamlDumpAddAction(item.getClassName(), item.getParameters());
        if (ReflectionsContainer.getInstance().isTestClass(item.getClassName())) {
            yamlTestActions.add(addAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(item.getClassName())) {
            yamlPublisherActions.add(addAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(item.getClassName())
                || item.getClassName().equals("BASICMetrics") || item.getClassName().equals("DEFAULTMetrics")){
            yamlMetricActions.add(addAction);
        } else if (item.getClassName().endsWith(".jar")) {
            yamlExtensionActions.add(addAction);
        }
    }

    private void configAction(ConfigParams.NamedMetaObject item) {
        YamlDumpConfigAction configAction = new YamlDumpConfigAction(item.getName(), item.getParameters());
        if (ReflectionsContainer.getInstance().isTestClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            yamlTestActions.add(configAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            yamlPublisherActions.add(configAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemsIdentifiers.get(item.getName()).getSimpleName())) {
            yamlMetricActions.add(configAction);
        }
    }

    private void excludeAction(String item) {
        YamlDumpExcludeAction excludeAction = new YamlDumpExcludeAction(item);
        if (ReflectionsContainer.getInstance().isTestClass(itemsIdentifiers.get(item).getSimpleName())) {
            yamlTestActions.add(excludeAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemsIdentifiers.get(item).getSimpleName())) {
            yamlPublisherActions.add(excludeAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemsIdentifiers.get(item).getSimpleName())) {
            yamlMetricActions.add(excludeAction);
        }
    }

    // Configure yaml representer to exclude class tags when dumping an object.
    private void configureYamlRepresenterToExcludeClassTags(Representer representer) {
        // Tag.MAP is by default ignored when dumping an object
        representer.addClassTag(GenerateYamlConfiguration.class, Tag.MAP);
        for (Class klass : ReflectionsContainer.getSubTypesOf(YamlDumpAction.class)) {
            representer.addClassTag(klass, Tag.MAP);
        }

    }

    /**
     * Creates a YAML configuration file.
     */
    public void createYamlConfigurationFile() {

        final String filename = DEFAULT_YAML_CONFIGURATION_FILENAME + TIMESTAMP + DEFAULT_YAML_EXTENSION;

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        org.yaml.snakeyaml.constructor.Constructor constructor = new org.yaml.snakeyaml.constructor.Constructor
                (GenerateYamlConfiguration.class);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setAllowReadOnlyProperties(true);

        // Configure the representer to ignore empty fields when dumping the object. By default, each empty filed is represented as {}.

        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {

                Method method = null;
                try {
                    method = propertyValue.getClass().getMethod("isEmpty");
                } catch (NoSuchMethodException e) { }

                if (method == null) {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                } else {

                    try {
                        if (Boolean.valueOf(method.invoke(propertyValue).toString())) {
                            return null;
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };

        configureYamlRepresenterToExcludeClassTags(representer);

        // dump configuration
        Yaml yaml = new Yaml(constructor, representer, dumperOptions);
        String yamlObjectRepresentation = yaml.dump(this);

        try {
            bufferedWriter.write(yamlObjectRepresentation);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fileWriter.close();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}