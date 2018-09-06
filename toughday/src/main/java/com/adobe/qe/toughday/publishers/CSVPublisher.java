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
package com.adobe.qe.toughday.publishers;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.core.MetricResult;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Description(desc = "Publish statistics to a csv file")
public class CSVPublisher extends Publisher {
    private static final Logger LOG = LoggerFactory.getLogger(CSVPublisher.class);

    /**
     * The default name of the file where aggregated results are published
     */
    public static final String DEFAULT_FILE_PATH = "results.csv";

    /**
     * The default name of the file where raw results are published
     */
    private static final String DEFAULT_RAW_FILE_PATH = "results.raw.csv";

    /**
     * Format of the raw results
     */
    private static final String RAW_FORMAT = "%s,%s,%s,%s,%s,%s,%s";

    /**
     * Header for the raw results
     */
    private static final String[] RAW_HEADER = { "Name", "Status", "Thread", "Start Timestamp", "End Timestamp", "Duration", "Data" };

    private Gson GSON = new Gson();

    private String header;
    private String aggregatedFormat;
    private boolean append = true;
    private boolean created = false;

    private PrintWriter resultsPrintWriter;
    private BufferedWriter resultsWriter;
    private String filePath = DEFAULT_FILE_PATH;

    private PrintWriter rawResultsWriter;
    private String rawFilePath = DEFAULT_RAW_FILE_PATH;

    @ConfigArgSet(required = false, desc = "The filename to write results to", defaultValue = DEFAULT_FILE_PATH)
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @ConfigArgGet
    public String getFilePath() {
        return this.filePath;
    }

    @ConfigArgSet(required = false, desc = "Append instead of rewrite", defaultValue = "true")
    public void setAppend(String value) {
        append = Boolean.valueOf(value);
    }

    @ConfigArgGet
    public boolean getAppend() {
        return append;
    }

    @ConfigArgSet(required = false, desc = "The filename to write the raw results to", defaultValue = DEFAULT_RAW_FILE_PATH)
    public void setRawFilePath(String rawResultsFilePath) {
        this.rawFilePath = rawResultsFilePath;
    }

    @ConfigArgGet
    public String getRawFilePath() {
        return rawFilePath;
    }

    @ConfigArgSet(required = false, defaultValue = "false", desc = "Enable the aggregated result publishing")
    public void setAggregatedPublish(String aggregatedPublish) {
        super.setAggregatedPublish(aggregatedPublish);
    }

    @Override
    protected void doPublishAggregatedIntermediate(Map<String, List<MetricResult>> results) {
        if (header == null) {
            createHeaderFormat(results.values().iterator().next());
        }
        publishAggregated(results);
    }

    @Override
    protected void doPublishAggregatedFinal(Map<String, List<MetricResult>> results) {
        publishAggregated(results);
    }

    public void publishAggregated(Map<String, List<MetricResult>> testsResults) {
        try {
            if(!created || !append) {
                resultsPrintWriter = new PrintWriter(filePath);
                created = true;
                resultsWriter = new BufferedWriter(resultsPrintWriter);
                resultsWriter.write(header);
                resultsWriter.newLine();
                resultsWriter.flush();
            }
            for (String testName : testsResults.keySet()) {
                List<Object> results = new ArrayList<>();
                List<MetricResult> testResultInfos = testsResults.get(testName);
                for (MetricResult resultInfo : testResultInfos) {
                    results.add(resultInfo.getValue());
                }

                resultsWriter.write(String.format(aggregatedFormat, results.toArray()));
                resultsWriter.newLine();
            }

            resultsWriter.flush();
            resultsPrintWriter.flush();

            if(!append) {
                resultsWriter.close();
                resultsPrintWriter.close();
            }
        } catch (IOException e) {
            LOG.error("Could not publish aggregated results", e);
        }
    }

    @Override
    protected void doPublishRaw(Collection<TestResult> testResults) {
        try {
            if (rawResultsWriter == null) {
                rawResultsWriter = new PrintWriter(new BufferedWriter(new FileWriter(rawFilePath)));
                rawResultsWriter.println(String.format(RAW_FORMAT, RAW_HEADER));
            }

            for (TestResult testResult : testResults) {
                Object data = testResult.getData();
                rawResultsWriter.println(String.format(RAW_FORMAT,
                        testResult.getTestFullName(),
                        testResult.getStatus().toString(),
                        testResult.getThreadId(),
                        testResult.getFormattedStartTimestamp(),
                        testResult.getFormattedEndTimestamp(),
                        testResult.getDuration(),
                        StringEscapeUtils.escapeCsv(data != null ? GSON.toJson(data) : "")
                ));
            }
            rawResultsWriter.flush();
        } catch (IOException e) {
            LOG.error("Could not publish results", e);
        }
    }

    @Override
    public void finish() {
        rawResultsWriter.flush();
    }

    private void createHeaderFormat(List<MetricResult> resultsList) {
        header = "";
        aggregatedFormat = "";
        for (MetricResult resultInfo : resultsList) {
            if(!header.isEmpty()) {
                header += ",";
                aggregatedFormat += ",";
            }
            header += resultInfo.getName();
            aggregatedFormat += resultInfo.getFormat();
        }
    }
}
