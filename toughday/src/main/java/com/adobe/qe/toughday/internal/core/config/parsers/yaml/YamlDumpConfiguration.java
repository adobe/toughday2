package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.*;

import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.DistributedConfig;
import org.apache.logging.log4j.Level;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Knows how to dump a Configuration object in yaml format in order to send it to the agents running in the cluster.
 * This class assumes that it is not necessary to dump the run mode and the publish mode fields of the Configuration
 * class since each Phase contained by this configuration already defines them.
 *
 * It is also assumed that the only action to be taken into consideration when dumping the configuration is Actions.ADD
 * since all the other actions were already processed before sending the execution query to the driver.
 */
public class YamlDumpConfiguration {

    private Configuration configuration;
    private List<YamlDumpPhase> phases = new ArrayList<>();

    public List<YamlDumpPhase> getPhases() {
        return this.phases;
    }

    /**
     * This method is used for collecting all configurable properties that were assigned a new value(different than the
     * default one) from the command line or the yaml configuration file used for running ToughDay.
     * @param type class of the object whose properties are collected
     * @param object instance
     */
    protected Map<String, Object> collectConfigurableProperties(Class type, Object object) {
        if (!type.isAssignableFrom(object.getClass())) {
            throw new IllegalArgumentException("The object must have the specified type.");
        }

        Map<String, Object> configurableArgs = new HashMap<>();

        /* add all inherited configurable properties */
        if (type.getSuperclass() != Object.class) {
            configurableArgs.putAll(collectConfigurableProperties(type.getSuperclass(), object));
        }

        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ConfigArgGet.class))
                .forEach(method -> {
                    String property = Configuration.propertyFromMethod(method.getName());
                    try {
                        Object value = method.invoke(object);

                        Class[] parametersType = {String.class};
                        Method m = type.getMethod(method.getName().replace("get", "set"), parametersType);
                        String defaultValue = m.getAnnotation(ConfigArgSet.class).defaultValue();

                        // skip all default values
                        if (!String.valueOf(value).equals(defaultValue) && value != null) {
                            if (value instanceof Level) {
                                configurableArgs.put(property, ((Level) value).name());
                            } else {
                                configurableArgs.put(property, value);
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    } catch ( NoSuchMethodException e) {
                        // skip for now
                    }
                });

        return configurableArgs;
    }

    private void addAction(ConfigParams.ClassMetaObject item, YamlDumpPhase yamlDumpPhase) {
        YamlDumpAddAction addAction = new YamlDumpAddAction(item.getClassName(), item.getParameters());
        if (ReflectionsContainer.getInstance().isTestClass(item.getClassName())) {
            yamlDumpPhase.getTests().add(addAction);
        } else if (ReflectionsContainer.getInstance().isMetricClass(item.getClassName())) {
            yamlDumpPhase.getMetrics().add(addAction);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(item.getClassName())) {
            yamlDumpPhase.getPublishers().add(addAction);
        }
    }

    private void createItem(Object object, List<Map.Entry<Actions, ConfigParams.MetaObject>> items ) {
        Map<String, Object> parameters = collectConfigurableProperties(object.getClass(), object);
        ConfigParams.MetaObject metaObject = new ConfigParams.ClassMetaObject(object.getClass().getSimpleName(), parameters);
        items.add(new AbstractMap.SimpleEntry<>(Actions.ADD, metaObject));
    }

    private void buildYamlDumpPhases() {
        configuration.getPhases().forEach(phase -> {
            // collect all configurable properties
            Map<String, Object> properties = collectConfigurableProperties(Phase.class, phase);
            Map<String, Object> runMode = collectConfigurableProperties(phase.getRunMode().getClass(), phase.getRunMode());
            Map<String, Object> publishMode = collectConfigurableProperties(phase.getPublishMode().getClass(), phase.getPublishMode());

            // add required type parameter for run mode and publish mode
            runMode.put("type", phase.getRunMode().getClass().getSimpleName().toLowerCase());
            publishMode.put("type", phase.getPublishMode().getClass().getSimpleName().toLowerCase());

            List<Map.Entry<Actions, ConfigParams.MetaObject>> items = new ArrayList<>();

            // create tests
            phase.getTestSuite().getTests().forEach(test -> createItem(test, items));
            // create metrics
            phase.getMetrics().forEach(metric -> createItem(metric, items));
            // create publishers
            phase.getPublishers().forEach(publisher -> createItem(publisher, items));

            YamlDumpPhase yamlDumpPhase = new YamlDumpPhase(properties, runMode, publishMode);
            items.forEach(entry -> addAction((ConfigParams.ClassMetaObject)entry.getValue(), yamlDumpPhase));

            this.phases.add(yamlDumpPhase);
        });
    }

    public YamlDumpConfiguration(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration must not be null.");
        }

        this.configuration = configuration;
    }

    /**
     * Getter for global params.
     */
    public Map<String, Object> getGlobals() {
       Map<String, Object> globals =
               collectConfigurableProperties(GlobalArgs.class, configuration.getGlobalArgs());
       /* this is required because duration is internally converted from string(which includes the unit of measure)
       to long value(duration in seconds) so we need to manually add the unit of measure when dumping it. */
       globals.put("duration", String.valueOf(globals.get("duration")) + 's');

       return globals;
    }

    /* used for dumping the configuration for running TD distributed */

    /**
     * Getter for the distributed configuration.
     */
    public Map<String, Object> getDistributedConfig() {
        return collectConfigurableProperties(DistributedConfig.class, configuration.getDistributedConfig());
    }

    /**
     * Dumps the configuration received in the constructor.
     */
    public String generateConfigurationObject() {
        buildYamlDumpPhases();
        Yaml yaml = YamlBuilder.getYamlInstance();

        return yaml.dump(this);
    }

}
