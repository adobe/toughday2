package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.lang.reflect.Method;

/**
 * Class responsible for building and configuring a Yaml instance that knows how to dump a configuration
 * in the desired format(without class tags, empty objects etc.)
 */
public class YamlBuilder {

    private static Yaml instance = null;

    // Configure yaml representer to exclude class tags when dumping an object.
    private static void configureYamlRepresenterToExcludeClassTags(Representer representer) {
        // Tag.MAP is by default ignored when dumping an object
        representer.addClassTag(GenerateYamlConfiguration.class, Tag.MAP);
        representer.addClassTag(YamlDumpConfiguration.class, Tag.MAP);
        for (Class type : ReflectionsContainer.getSubTypesOf(YamlDumpAction.class)) {
            representer.addClassTag(type, Tag.MAP);
        }

    }

    private static void buildInstance() {
        DumperOptions dumperOptions = getDumperOptions();

        // Configure the representer to ignore empty fields when dumping the object. By default, each empty filed is represented as {}.
        Representer representer = getRepresenter();
        configureYamlRepresenterToExcludeClassTags(representer);

        Constructor constructor = new Constructor(GenerateYamlConfiguration.class);
        instance = new Yaml(constructor, representer, dumperOptions);
    }

    /**
     * Getter for the Yaml instance.
     */
    public static Yaml getYamlInstance() {
        if (instance == null) {
            buildInstance();
        }

        return instance;
    }


    private static Representer getRepresenter() {
        return new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {

                Method method = null;
                try {
                    if (propertyValue == null) {
                        propertyValue = "";
                    }
                    method = propertyValue.getClass().getMethod("isEmpty");
                } catch (NoSuchMethodException ignored) { }

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
    }

    private static DumperOptions getDumperOptions() {
        DumperOptions dumperOptions = new DumperOptions();

        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setAllowReadOnlyProperties(true);

        return dumperOptions;
    }
}
