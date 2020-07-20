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
import com.adobe.qe.toughday.internal.core.config.ConfigurationParser;
import com.adobe.qe.toughday.internal.core.config.ParserArgHelp;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class YamlParser implements ConfigurationParser {
    public static final String CONFIG_FILE_ARG_NAME = "configfile";
    public static final String CONFIG_FILE_DESCRIPTION = "Config file in yaml format.";


    public static final ParserArgHelp configFile = new ParserArgHelp() {
        @Override
        public String name() {
            return CONFIG_FILE_ARG_NAME;
        }

        @Override
        public String defaultValue() {
            return "";
        }

        @Override
        public String description() {
            return CONFIG_FILE_DESCRIPTION;
        }
    };

    private Constructor getYamlConstructor() {
        Constructor constructor = new Constructor(YamlConfiguration.class);
        TypeDescription yamlParserDesc = new TypeDescription(YamlConfiguration.class);

        yamlParserDesc.putListPropertyType("tests", YamlParseAction.class);
        yamlParserDesc.putListPropertyType("phases", YamlParsePhase.class);
        yamlParserDesc.putListPropertyType("publishers", YamlParseAction.class);
        yamlParserDesc.putListPropertyType("metrics", YamlParseAction.class);
        yamlParserDesc.putListPropertyType("extensions", YamlParseAction.class);
        yamlParserDesc.putListPropertyType("feeders", YamlParseAction.class);

        constructor.addTypeDescription(yamlParserDesc);

        return constructor;
    }

    public ConfigParams parse(String stringYaml) {
        Constructor constructor = getYamlConstructor();

        Yaml yaml = new Yaml(constructor);
        YamlConfiguration yamlConfig = (YamlConfiguration) yaml.load(stringYaml);

        return yamlConfig.getConfigParams();
    }

    @Override
    public ConfigParams parse(String[] cmdLineArgs) {
        String configFilePath = null;

        for(String arg : cmdLineArgs) {
            if (arg.startsWith("--" + CONFIG_FILE_ARG_NAME +  "=")) {
                configFilePath = arg.split("=")[1];
            }
        }

        if(configFilePath != null) {
            try {
                Constructor constructor = getYamlConstructor();

                Yaml yaml = new Yaml(constructor);
                YamlConfiguration yamlConfig = (YamlConfiguration) yaml.load(new FileInputStream(configFilePath));

                return yamlConfig.getConfigParams();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return new ConfigParams();
    }
}
