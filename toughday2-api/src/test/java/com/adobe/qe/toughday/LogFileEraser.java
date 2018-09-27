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
package com.adobe.qe.toughday;

import com.adobe.qe.toughday.internal.core.Timestamp;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class LogFileEraser {

    private LogFileEraser() {}

    public static void deteleFiles(org.apache.logging.log4j.core.config.Configuration config) {
        File folder = new File("logs_" + Timestamp.START_TIME);

        if (folder.exists()) {
            if(config.getLoggerContext() != null) {
                config.getLoggerContext().reconfigure();
            }

            for (Map.Entry<String, Appender> appenderEntry : config.getAppenders().entrySet()) {
                appenderEntry.getValue().stop();
            }

            try {
                boolean allDeleted = true;
                for (File file : folder.listFiles()) {
                    if (!file.delete()) {
                        allDeleted = false;
                    }
                }

                if (allDeleted) {
                    FileUtils.deleteDirectory(folder);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
