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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Description(desc = "Publisher for writing at standard output.")
public class ConsolePublisher extends Publisher {
    private static final Logger LOG = LoggerFactory.getLogger(ConsolePublisher.class);
    private boolean begun = false;
    private boolean finished = false;
    private boolean clearScreen = true;
    private boolean rawPublishCalled = false;
    private final CleanerThread cleaner;
    private Scanner sc;
    private AtomicInteger extraLines;
    private String asterisks = null;


    @ConfigArgSet(required = false, defaultValue = "false", desc = "Enable the raw result publishing")
    public void setRawPublish(String rawPublish) {
        super.setRawPublish(rawPublish);
    }

    @ConfigArgSet(required = false, defaultValue = "true", desc = "Clear the screen before printing each stat")
    public void setClear(String clearScreen) {
        this.clearScreen = Boolean.parseBoolean(clearScreen);
    }

    @ConfigArgGet
    public boolean getClear() {
        return this.clearScreen;
    }

    class CleanerThread extends Thread {

        @Override
        public void run() {
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(isr);
            try {
                while (!finished && !this.isInterrupted()) {
                        while (!in.ready()) {
                        if (finished) {
                            this.interrupt();
                            break;
                        }
                        this.sleep(200);
                    }

                    if (finished) {
                        this.interrupt();
                        break;
                    }

                    if (in.readLine() != null) {
                        extraLines.incrementAndGet();
                    }
                }
            } catch (IOException | InterruptedException e) {
                // nop
            }
        }
    }

    public ConsolePublisher() {
        setRawPublish(Boolean.FALSE.toString()); //TODO remove this when call config arg set is merged
        sc = new Scanner(System.in);
        extraLines = new AtomicInteger(0);
        this.cleaner = new CleanerThread();
        this.cleaner.start();
    }


    private void alignMetrics() {
        System.out.println();
        System.out.printf("%-35s", " ");
    }

    // publish aggregated results method, called periodically
    private void publishAggregated(Map<String, List<MetricResult>> results) {
        final int METRIC_LENGTH = 12;
        final int METRICS_PER_LINE_LIMIT = 3;
        int nrMetrics = results.values().iterator().next().size() - 1;
        int nrLinesPerTest = nrMetrics / METRICS_PER_LINE_LIMIT + 2;
        int nrStats = results.size() * nrLinesPerTest;
        final String FORMAT = "%-37s | ";
        // "clear" screen
        if (!begun) {
            if (asterisks == null) {
                // this wil have to be changed when metrics become local to phases probably
                // maybe change asterisks depending on the number of metrics per phase
                int metricsPerLine = nrMetrics < 3 ? nrMetrics : METRICS_PER_LINE_LIMIT;

                // 35 asterisks for the test name, 40 for each metric, -1 because there will be one extra space left
                asterisks = CharBuffer.allocate(35 + 40 * metricsPerLine - 1).toString().replace('\0', '*');
            }
            System.out.println();
            System.out.println(asterisks);
        }

        if (begun && clearScreen) {
//            for (int i=0; i < nrStats + extraLines.get(); i++ ) {
//                System.out.print("\33[1A\33[2K");
//            }
            System.out.print(String.format("%c[%dA", 0x1B, nrStats + extraLines.get()));
        }

        for (String testName : results.keySet()) {
            System.out.printf("%-35.35s", testName);
            List<MetricResult> metricResults = results.get(testName);
            metricResults.remove(0);
            int metricsPerLineCounter = 0;

            for (MetricResult resultInfo : metricResults) {
                String metricIdentifier = resultInfo.getName();
                String padding = StringUtils.repeat(' ', METRIC_LENGTH - metricIdentifier.length());
                String resultFormat = resultInfo.getFormat();
                String unitOfMeasure = resultInfo.getUnitOfMeasure();

                System.out.printf(FORMAT, metricIdentifier + ":" + padding +
                        String.format(resultFormat, resultInfo.getValue()) + " " + unitOfMeasure);
                metricsPerLineCounter++;

                if (metricsPerLineCounter == METRICS_PER_LINE_LIMIT) {
                    alignMetrics();
                    metricsPerLineCounter = 0;
                }
            }

            System.out.println();
            System.out.println();
        }
        begun = true;

        extraLines.set(0);
    }

    @Override
    protected void doPublishAggregatedIntermediate(Map<String, List<MetricResult>> results) {
        publishAggregated(results);
    }

    @Override
    protected void doPublishAggregatedFinal(Map<String, List<MetricResult>> results) {
        this.clearScreen = false;
        String message = "FINAL RESULTS";
        int spaces = message.length() > asterisks.length() ? 0 : (asterisks.length() - message.length()) / 2;
        System.out.println(asterisks);
        System.out.println(CharBuffer.allocate(spaces).toString().replace('\0', ' ') + message);
        System.out.println(asterisks);
        publishAggregated(results);
        System.out.println(asterisks);
        System.out.println();
        System.out.println("___");
        System.out.println();

        this.begun = false;
        this.clearScreen = true;
    }

    @Override
    protected void doPublishRaw(Collection<TestResult> testResults) {
        if(!rawPublishCalled) {
            System.out.println("Raw publish is not supported in " + this.getClass().getSimpleName());
            rawPublishCalled = true;
        }
    }

    @Override
    public void finish() {
        this.finished = true;
        this.cleaner.interrupt();
    }

    private static String getFriendlyDuration(long millis) {
        if (millis < 0) {
            return "0 s";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days > 0)
            sb.append(days).append(" d ");
        if (hours > 0)
            sb.append(hours).append(" h ");
        if (minutes > 0)
            sb.append(minutes).append(" m ");
        sb.append(seconds);
        sb.append(" s");

        return(sb.toString());
    }

    private void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

}
