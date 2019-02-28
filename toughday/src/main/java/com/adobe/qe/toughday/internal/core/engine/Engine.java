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
package com.adobe.qe.toughday.internal.core.engine;

import com.adobe.qe.toughday.Main;
import com.adobe.qe.toughday.api.annotations.feeders.FeederGet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AssumptionUtils;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.core.RunnersContainer;
import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.internal.core.*;
import com.adobe.qe.toughday.api.annotations.Setup;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.utils.PackageManagerClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Engine for running a test suite.
 */
public class Engine {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);
    public static final int RESULT_AGGREATION_DELAY = 1000; //in 1 Second
    protected static final int WAIT_TERMINATION_FACTOR = 30;
    protected static final double TIMEOUT_CHECK_FACTOR = 0.03;
    protected static Random _rnd = new Random();

    private final Configuration configuration;
    private GlobalArgs globalArgs;
    private ExecutorService engineExecutorService = Executors.newFixedThreadPool(2);
    private final ReentrantReadWriteLock engineSync = new ReentrantReadWriteLock();
    private List<Phase> phases;
    private Phase currentPhase;
    private final ReadWriteLock currentPhaseLock = new ReentrantReadWriteLock();
    private volatile boolean testsRunning;

    /**
     * Constructor
     * @param configuration A Configuration object.
     * @throws InvocationTargetException caused by reflection
     * @throws NoSuchMethodException caused by reflection
     * @throws InstantiationException caused by reflection
     * @throws IllegalAccessException caused by reflection
     */
    public Engine(Configuration configuration) {
        this.configuration = configuration;
        this.globalArgs = configuration.getGlobalArgs();
        this.phases = configuration.getPhases();

        //TODO find a better way to do this
        for (Phase phase : phases) {
            phase.getPublishMode().setEngine(this);
        }
    }


    public Configuration getConfiguration() { return configuration; }

    /**
     * Returns the global args
     * @return
     */
    public GlobalArgs getGlobalArgs() {
        return globalArgs;
    }

    public boolean areTestsRunning() { return testsRunning; }

    /**
     * Recursive method for preparing a test to run.
     * @param test
     * @return this object. (builder pattern)
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    private Engine add(AbstractTest test)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        createRunners(test);
        addToRunMap(test);
        return this;
    }

    private Engine addToRunMap(AbstractTest test) {
        currentPhase.getPublishMode().getRunMap().addTest(test);
        currentPhase.getCounts().put(test, new AtomicLong(0));
        if(test.includeChildren()) {
            for (AbstractTest child : test.getChildren()) {
                addToRunMap(child);
            }
        }
        return this;
    }

    private Engine createRunners(AbstractTest test)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RunnersContainer.getInstance().addRunner(test);
        for(AbstractTest child : test.getChildren()) {
            createRunners(child);
        }
        return this;
    }

    private static class LogStream extends OutputStream {
        Logger logger;
        String mem = "";

        public LogStream(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void write (int b) {
            byte[] bytes = new byte[1];
            bytes[0] = (byte) (b & 0xff);
            mem = mem + new String(bytes);

            if (mem.endsWith ("\n")) {
                mem = mem.substring(0, mem.length () - 1);
                flush ();
            }
        }

        /**
         * Flushes the output stream.
         */
        public void flush () {
            logger.info(mem);
            mem = "";
        }
    }

    /**
     * Method for starting running tests.
     * @throws Exception
     */
    public void runTests() {
        try {
            run();
        } catch (Exception e) {
            LOG.error("Failure in tests execution ", e);
        }
    }

    public static void printConfiguration(Configuration configuration, PrintStream out) throws InvocationTargetException, IllegalAccessException {
        out.println();
        out.println("#################### Configuration ######################");
        out.println("Global configuration:");
        printObject(out, configuration.getGlobalArgs());

        out.println("#################### Feeders ######################");
        for (Feeder feeder : configuration.getFeeders()) {
            printObject(out, feeder);
        }

        int i = 1;
        for (Phase phase : configuration.getPhases()) {
            out.println("######################## Phase " + i + " ########################");
            ++i;

            out.println("Phase configuration: ");
            printObject(out, phase);

            out.println("Run mode configuration: ");
            printObject(out, phase.getRunMode());

            out.println("Publish mode configuration: ");
            printObject(out, phase.getPublishMode());

            out.println("Tests:");
            for (AbstractTest test : phase.getTestSuite().getTests()) {
                printObject(out, test);
            }

            out.println("Publishers:");
            for (Publisher publisher : phase.getPublishers()) {
                printObject(out, publisher);
            }

            out.println("Metrics:");
            for (Metric metric : phase.getMetrics()) {
                printObject(out, metric);
            }

            out.println("#########################################################");

            out.println();
            out.println();
        }
    }

    public static void installToughdayContentPackage(GlobalArgs globalArgs) throws Exception {
        logGlobal("Installing ToughDay 2 Content Package...");
        PackageManagerClient packageManagerClient = AEMTestBase.createClient(globalArgs).adaptTo(PackageManagerClient.class);

        String tdContentPackageGroup = "com.adobe.qe.toughday";
        String tdContentPackageName = ReflectionsContainer.getInstance().getToughdayContentPackage();

        //Upload and install test content package
        if(packageManagerClient.isPackageCreated(tdContentPackageName, tdContentPackageGroup)) {
            packageManagerClient.deletePackage(tdContentPackageName, tdContentPackageGroup);
        }

        packageManagerClient.uploadPackage(
                Engine.class.getClassLoader().getResourceAsStream(tdContentPackageName), tdContentPackageName);
        packageManagerClient.installPackage(tdContentPackageName, tdContentPackageGroup);
        logGlobal("Finished installing ToughDay 2 Content Package.");
    }

    public static void printObject(PrintStream out, Object obj)
            throws InvocationTargetException, IllegalAccessException {
        Class objectClass = obj.getClass();
        out.println("- Configuration for object of class " + objectClass.getSimpleName()+" ["+objectClass.getName()+"]");
        out.println(String.format("\t%-32s %-64s", "Property", "Value"));
        for(Method method : objectClass.getMethods()) {
            if (method.isAnnotationPresent(ConfigArgGet.class)) {
                ConfigArgGet configArg = method.getAnnotation(ConfigArgGet.class);

                printObjectProperty(out,
                        StringUtils.isEmpty(configArg.name()) ? Configuration.propertyFromMethod(method.getName()) : configArg.name(),
                        method.invoke(obj));
            }

            if (method.isAnnotationPresent(FeederGet.class)) {
                FeederGet feederGet = method.getAnnotation(FeederGet.class);
                Object feeder = method.invoke(obj);
                if(feeder == null) return;
                try {
                    String name = (String) feeder.getClass().getMethod("getName").invoke(feeder);
                    printObjectProperty(out,
                            Configuration.propertyFromMethod(method.getName()),
                            name);
                } catch (NoSuchMethodException e) {
                    //Should not happen
                    return;
                }
            }
        }

        out.println();
        out.println();
    }

    public static void printObjectProperty(PrintStream out, String propertyName, Object propertyValue) {
        out.println(String.format("\t%-32s %-64s", propertyName, propertyValue));
    }

    /**
     * Returns the current Date and Time in a String format.
     */
    public static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS z")
                .format(Calendar.getInstance().getTime());
    }
    
    //TODO find a place in the AbstractTestRunner for this. The problem is that we need to invoke it at a specific time.
    public void runSetup(AbstractTest test) throws Exception {
        LinkedList<Method> setupMethods = new LinkedList<>();
        Class currentClass = test.getClass();
        while(!currentClass.getName().equals(AbstractTest.class.getName())) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.getAnnotation(Setup.class) != null) {
                    AssumptionUtils.validateAnnotatedMethod(method, Setup.class);
                    setupMethods.addFirst(method);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        test.benchmark().setRunMap(currentPhase.getPublishMode().getRunMap());
        for(Method setupMethod : setupMethods) {
            setupMethod.setAccessible(true);
            setupMethod.invoke(test);
            setupMethod.setAccessible(false);
        }
        test.benchmark().setRunMap(null);

        for (AbstractTest child : test.getChildren()) {
            runSetup(child);
        }
    }

    private void run() throws Exception {
        if(globalArgs.getInstallSampleContent() && !globalArgs.getDryRun()) {
            printConfiguration(configuration, new PrintStream(new LogStream(LOG)));
            installToughdayContentPackage(globalArgs);
        }

        for(Feeder feeder : configuration.getFeeders()) {
            feeder.init();
        }

        Engine.logGlobal(String.format("Running tests for %s seconds or until count for all tests has been reached",
                configuration.getGlobalArgs().getDuration()));

        Engine.logGlobal("Test execution started at: " + Engine.getCurrentDateTime());

        if (globalArgs.getDryRun()) {
            System.out.println("NOTE: This is just a dry run. No test is actually executed.");
            printConfiguration(this.getConfiguration(), System.out);
            return;
        }

        // Create the result aggregator thread
        AsyncResultAggregator resultAggregator = new AsyncResultAggregator(this);

        // Create the timeout checker thread
        AsyncTimeoutChecker timeoutChecker = new AsyncTimeoutChecker(this, Thread.currentThread());

        Thread shutdownHook = new Thread() {
            public void run() {
                try {
                    currentPhaseLock.readLock().lock();

                    boolean wasMeasurable;
                    wasMeasurable = currentPhase.getMeasurable();
                    currentPhase.setMeasurable("false");

                    currentPhase.getRunMode().finishExecutionAndAwait();

                    // interrupt extra test threads
                    // TODO: this is suboptimal, replace with a better mechanism for notifications
                    List<Thread> threadsList = AbstractTest.getExtraThreads();
                    synchronized (threadsList) {
                        for (Thread t : threadsList) {
                            t.interrupt();
                        }
                    }
                    timeoutChecker.finishExecution();
                    resultAggregator.finishExecution();
                    resultAggregator.aggregateResults();
                    shutdownAndAwaitTermination(currentPhase.getRunMode().getExecutorService());
                    shutdownAndAwaitTermination(engineExecutorService);
                    currentPhase.getPublishMode().publish(currentPhase.getPublishMode().getRunMap().getCurrentTestResults());

                    if (wasMeasurable) {
                        currentPhase.getPublishMode().publishFinalResults(resultAggregator.filterResults());
                    }
                } catch (Throwable e) {
                    System.out.println("Exception in shutdown hook!");
                    e.printStackTrace();
                } finally {
                    currentPhaseLock.readLock().unlock();
                }

                Engine.logGlobal("Test execution finished at: " + Engine.getCurrentDateTime());
                LogManager.shutdown();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        Set<Phase> phasesWithoutDuration = configuration.getPhasesWithoutDuration();
        engineExecutorService.execute(resultAggregator);
        engineExecutorService.execute(timeoutChecker);

        int i = 0;
        for (Phase phase : phases) {
            ++i;
            testsRunning = true;

            try {
                currentPhaseLock.writeLock().lock();
                currentPhase = phase;
            } finally {
                currentPhaseLock.writeLock().unlock();
            }

            phasesWithoutDuration.remove(phase);

            currentPhase.getPublishMode().getRunMap().reinitStartTimes();

            // Run the setup step of the suite
            for (SuiteSetup setupStep : phase.getTestSuite().getSetupStep()) {
                setupStep.setup();
            }

            for (AbstractTest test : phase.getTestSuite().getTests()) {
                runSetup(test);
            }

            for (AbstractTest test : phase.getTestSuite().getTests()) {
                add(test);
            }

            RunMode currentRunmode = phase.getRunMode();
            Long currentDuration = GlobalArgs.parseDurationToSeconds(phase.getDuration());

            currentRunmode.runTests(this);
            long start = System.currentTimeMillis();

            try {
                Thread.sleep(currentDuration * 1000L);

            } catch (InterruptedException e) {
                LOG.info("Phase interrupted.");
                long elapsed = System.currentTimeMillis() - start;

                if (configuration.getDistributedConfig().getAgent()) {
                    Agent.announcePhaseCompletion();
                }

                // if the phase finishes sooner than its duration,
                // the remainder is split equally between the remaining phases
                // whose duration was not given as input
                if (!phasesWithoutDuration.isEmpty()) {
                long timeToDistributePerPhase = (currentDuration - elapsed / 1000) / phasesWithoutDuration.size();
                    for (Phase phaseWithoutDuration : configuration.getPhasesWithoutDuration()) {
                        phaseWithoutDuration.setDuration(phaseWithoutDuration.getDuration() + timeToDistributePerPhase + "s");
                    }
                }
            }

            // in case of SIGINT, the shutdownhook acquires the read lock in order to
            // make the current phase not measurable, because the final results would have
            // been printed twice: once in the shutdownhook and the second time here, a few
            // lines below
            // TODO: find better fix for SIGINT handling

            phase.getRunMode().finishExecutionAndAwait();

            // execute this block of code only if the current phase is not the last one
            // and if it is measurable
            if (i < phases.size() && currentPhase.getMeasurable()) {
                shutdownAndAwaitTermination(phase.getRunMode().getExecutorService());
                resultAggregator.aggregateResults();
                phase.getPublishMode().publishFinalResults(resultAggregator.filterResults());
            }
        }

        testsRunning = false;
        shutdownHook.run();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public ReentrantReadWriteLock getEngineSync() {
        return engineSync;
    }

    /**
     * Method for getting the next weighted random test form the test suite
     * TODO: optimize
     */
    public static AbstractTest getNextTest(TestSuite testSuite, Map<AbstractTest, AtomicLong> counts, ReentrantReadWriteLock engineSync) throws InterruptedException {
        //If we didn't find the next test we start looking for it assuming that not all counts are done
        while (testSuite.getTests().size() != 0) {
            engineSync.readLock().lock();
            try {
                int randomNumber = _rnd.nextInt(testSuite.getTotalWeight());
                for (AbstractTest test : testSuite.getTests()) {
                    int testWeight = test.getWeight();

                    long testRuns = counts.get(test).get();
                    Long maxRuns = test.getCount();

                    //If max runs was exceeded for a test
                    if (maxRuns >= 0 && testRuns > maxRuns) {
                        //Try to acquire the lock for removing the test from the suite
                        engineSync.readLock().unlock();
                        engineSync.writeLock().lock();
                        try {
                            if(!testSuite.contains(test.getName())) { break; }
                            //Remove test from suite
                            testSuite.remove(test);
                            //Start looking for the test from the beginning as the total weight changed
                            break;
                        } finally {
                            engineSync.writeLock().unlock();
                            engineSync.readLock().lock();
                        }
                    }
                    if (randomNumber < testWeight) {
                        return test;
                    }
                    randomNumber = randomNumber - testWeight;
                }
            } finally {
                engineSync.readLock().unlock();
            }
        }
        return null;
    }

    public static void logGlobal(String message) {
        LOG.info(message);
        LogManager.getLogger(Main.class).info(message);
    }

    /**
     * Method for forcing an ExecutorService to finish.
     * @param pool
     */
    protected void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(WAIT_TERMINATION_FACTOR * RESULT_AGGREATION_DELAY, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(WAIT_TERMINATION_FACTOR * RESULT_AGGREATION_DELAY, TimeUnit.MILLISECONDS))
                    LOG.error("Thread pool did not terminate. Process must be killed");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public Phase getCurrentPhase() {
        currentPhaseLock.readLock().lock();
        Phase phase = currentPhase;
        currentPhaseLock.readLock().unlock();

        return phase;
    }

    public ReadWriteLock getCurrentPhaseLock() {
        return currentPhaseLock;
    }
}
