package com.adobe.qe.toughday.internal.core.distributedtd.requests;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.PhaseParams;
import com.adobe.qe.toughday.internal.core.distributedtd.DistributedPhaseMonitor;
import com.adobe.qe.toughday.internal.core.distributedtd.YamlDumpConfigurationAsTaskForTDAgents;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverUpdateInfo;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.MasterElection;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessor;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessorDispatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import spark.Request;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CandidateRequestProcessorTest {
    @Mock Request request;
    @Mock Driver mockDriver;
    @Mock DriverState driverStateMock;
    private DistributedPhaseMonitor distributedPhaseMonitor= new DistributedPhaseMonitor();
    private final MasterElection masterElection = MasterElection.getInstance(3);
    private static ReflectionsContainer reflections = ReflectionsContainer.getInstance();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();

        reflections.getTestClasses().put("MockTest", MockTest.class);
    }

    @Before
    public void setup() {
        Mockito.when(mockDriver.getDriverState()).thenReturn(driverStateMock);
        Mockito.when(driverStateMock.getCurrentState()).thenReturn(DriverState.Role.CANDIDATE);
        Mockito.when(mockDriver.getMasterElection()).thenReturn(this.masterElection);
        Mockito.when(driverStateMock.getId()).thenReturn(1);

        masterElection.resetInvalidCandidates();
    }

    @Test
    public void testProcessUpdatesRequests() throws JsonProcessingException {
        Queue<String> registeredAgents = new LinkedList<>(Collections.singleton("10.0.0.1"));

        masterElection.markCandidateAsInvalid(0);
        masterElection.markCandidateAsInvalid(1);
        Mockito.when(driverStateMock.getRegisteredAgents()).thenReturn(registeredAgents);

        DriverUpdateInfo updates = new DriverUpdateInfo(1, DriverState.Role.CANDIDATE,
                masterElection.getInvalidCandidates(), registeredAgents, "", "");
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedYaml = objectMapper.writeValueAsString(updates);

        RequestProcessor processor = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver);

        Assert.assertEquals(expectedYaml, processor.processUpdatesRequest(request, mockDriver));
    }

    @Test
    public void testProcessMasterElectionRequest() {
        Mockito.when(request.body()).then((Answer<String>) answer -> "1");
        Mockito.when(driverStateMock.getMasterIdLock()).thenReturn(new ReentrantReadWriteLock());
        RequestProcessor processor = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver);

        masterElection.markCandidateAsInvalid(0);
        processor.processMasterElectionRequest(request, mockDriver);

        // test that driver-1 was added as invalid candidate
        Queue<Integer> expectedInvalidCandidates = new LinkedList<>(Arrays.asList(0, 1));
        Assert.assertEquals(expectedInvalidCandidates, masterElection.getInvalidCandidates());

        // test that driver-2 was elected as master
        Mockito.verify(driverStateMock).setMasterId(2);

        // test that driver-1 is running as a candidate after this process
        Mockito.verify(driverStateMock).setCurrentState(DriverState.Role.CANDIDATE);
    }

    @Test
    public void testProcessPhaseCompletionAnnouncement() {
        Mockito.when(request.body()).thenReturn("10.0.0.1");
        Mockito.when(request.queryParams("forward")).thenReturn("false");
        Mockito.when(mockDriver.getDistributedPhaseMonitor()).thenReturn(distributedPhaseMonitor);

        RequestProcessor processor = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver);
        processor.processPhaseCompletionAnnouncement(request);

        // test that the agent was added to the list of agents which finished executing the current phase
        Assert.assertEquals(Collections.singletonList("10.0.0.1"), mockDriver.getDistributedPhaseMonitor().getAgentsWhichCompletedCurrentPhase());
    }

    @Test
    public void processExecutionRequest() throws Exception {
        List<String> cmdLineArgs = new ArrayList<String>() {{
            add("--phase");
            add("name=testPhase");
            add("--host=localhost");
            add("--add");
            add("MockTest");
            add("count=400");
        }};

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        PhaseParams.namedPhases.clear();
        YamlDumpConfigurationAsTaskForTDAgents yamlDumpConfiguration
                = new YamlDumpConfigurationAsTaskForTDAgents(configuration);
        String yamlConfig = yamlDumpConfiguration.generateConfigurationObject();

        Mockito.when(request.body()).thenReturn(yamlConfig);
        Mockito.when(request.queryParams("forward")).thenReturn("false");
        Mockito.when(mockDriver.getDistributedPhaseMonitor()).thenReturn(distributedPhaseMonitor);
        Mockito.when(mockDriver.getConfiguration()).thenReturn(configuration);

        RequestProcessor processor = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver);
        processor.processExecutionRequest(request, mockDriver);

        // test that the configuration was set
        Mockito.verify(mockDriver, Mockito.times(1))
                .setConfiguration(Mockito.any(Configuration.class));

        // test that the first phase of the configuration to be executed is being monitored
        Assert.assertEquals("testPhase", mockDriver.getDistributedPhaseMonitor().getPhase().getName());

        // test that the sample content package should not be installed by the candidates
        Assert.assertFalse(mockDriver.getConfiguration().getGlobalArgs().getInstallSampleContent());
    }

}
