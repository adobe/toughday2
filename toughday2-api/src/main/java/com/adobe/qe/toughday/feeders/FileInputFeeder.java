/*
Copyright 2018 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.feeders;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.core.NamedObjectImpl;
import com.adobe.qe.toughday.api.feeders.InputFeeder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Description(desc = "Feeder that returns content from a file line by line and restarts when the file ends.")
public class FileInputFeeder extends NamedObjectImpl implements InputFeeder<String> {
    private String filePath;
    private boolean cacheable;
    private boolean cached = false;

    private Object lock = new Object();
    private ArrayList<String> fileContent = new ArrayList<>(); // Only if cacheable == true
    private BufferedReader reader;
    private AtomicInteger idx = new AtomicInteger(0);

    @ConfigArgSet(desc = "The file from where to read the lines")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @ConfigArgGet
    public String getFilePath() {
        return this.filePath;
    }

    @ConfigArgSet(required = false, defaultValue = "true", desc = "Can the content of the file be cached in memory. (Usually false for large file)")
    public void setCacheable(String cacheable) {
        this.cacheable = Boolean.parseBoolean(cacheable);
    }

    @ConfigArgGet
    public boolean getCacheable() {
        return cacheable;
    }

    @Override
    public String get(Object... keys) throws Exception {
        // if the file can be cached
        if(cacheable && cached) {
            return fileContent.get(idx.getAndUpdate(i -> (i + 1) % fileContent.size()));
        }

        synchronized (lock) {
            if(cached) {
                return get();
            }

            String line = reader.readLine();
            if(cacheable && line != null) {
                fileContent.add(line);
            }

            if (line == null) {
                reader.close();
                init();
                cached = cacheable;
                return get();
            }

            return line;
        }
    }

    @Override
    public void init() throws Exception {
        File file = new File(filePath);
        if(!file.exists()) {
            throw new IllegalArgumentException("The file does not exist");
        }
        if(file.length() == 0) {
            throw new IllegalArgumentException("The file cannot be empty");
        }
        this.reader = new BufferedReader(new FileReader(filePath));
    }
}
