package com.adobe.qe.toughday.internal.core.distributedtd.requests;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.distributedtd.DistributedPhaseMonitor;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessor;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessorDispatcher;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.TaskBalancer;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class MasterRequestProcessorTest {
    @Mock Request requestMock;
    @Mock Response responseMock;
    @Mock Driver mockDriver;
    @Mock DistributedPhaseMonitor distributedPhaseMonitorMock;
    @Mock DriverState driverStateMock;
    @Mock TaskBalancer taskBalancerMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static ReflectionsContainer reflections = ReflectionsContainer.getInstance();

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();

        reflections.getTestClasses().put("MockTest", MockTest.class);
    }

    @Before
    public void setup() {
        Mockito.when(mockDriver.getDriverState()).thenReturn(driverStateMock);
        Mockito.when(driverStateMock.getCurrentState()).thenReturn(DriverState.Role.MASTER);
    }

    @Test
    public void testSampleContentInstallationFailureForcesExecutionToStop() {
        Mockito.when(requestMock.body()).thenReturn("false");
        RequestProcessor requestProcessor = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver);

        requestProcessor.acknowledgeSampleContentSuccessfulInstallation(requestMock, mockDriver, responseMock);

        /* check that the driver finishes the distributed execution if TD sample content package was not successfully
        /* installed
         */
        Mockito.verify(mockDriver).finishDistributedExecution();
    }

    @Test
    public void testProcessHeartbeatRequest()  {
        Map<String, Map<String, Long>> executions = new HashMap<>();
        Map<String, Long> testsPerAgent = new HashMap<String, Long>() {{
            put("Agent1", 200L);
            put("Agent2", 231L);
        }};
        executions.put("MockTest", testsPerAgent);

        Mockito.when(mockDriver.getDistributedPhaseMonitor()).thenReturn(distributedPhaseMonitorMock);
        Mockito.when(distributedPhaseMonitorMock.getExecutions()).thenReturn(executions);

        Gson gson = new Gson();
        String expected = gson.toJson(executions);
        String actual = RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver)
                .processHeartbeatRequest(requestMock, mockDriver);

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void testProcessAgentFailureAnnouncementThrowsExceptionForMaster() {
        /* the master is the one sending heartbeat messages to the agents running in the cluster and should therefore
         * never receive this type of request from another driver.
         */
        RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver)
                .processAgentFailureAnnouncement(requestMock, mockDriver);
    }

    private void mockitoSetupForRegisterRequest(String newAgentIP) {
        Mockito.when(requestMock.body()).thenReturn(newAgentIP);
        Mockito.when(requestMock.queryParams("forward")).thenReturn("false");
        Mockito.when(mockDriver.getDistributedPhaseMonitor()).thenReturn(distributedPhaseMonitorMock);
        Mockito.when(mockDriver.getTaskBalancer()).thenReturn(taskBalancerMock);
    }

    @Test
    public void testProcessRegisterRequestWhenExecutionDidNotStarted() {
        String newAgentIP = "10.0.0.1";

        mockitoSetupForRegisterRequest(newAgentIP);
        Mockito.when(distributedPhaseMonitorMock.isPhaseExecuting()).thenReturn(false);
        Mockito.when(driverStateMock.getRegisteredAgents()).thenReturn(new LinkedList<>());

        RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver)
                .processRegisterRequest(requestMock, mockDriver);

        // check that the new agent was added to the list of active agents
        Mockito.verify(driverStateMock, Mockito.times(1)).registerAgent(newAgentIP);

        // check that the work redistribution process is not scheduled when the execution did not started
        Mockito.verify(taskBalancerMock, Mockito.times(0)).
                scheduleWorkRedistributionProcess(mockDriver, newAgentIP, true);
    }

    @Test
    public void testRegisterRequestTriggersWorkRedistributionWhenTDIsExecuted() {
        String newAgentIP = "10.0.0.1";

        mockitoSetupForRegisterRequest(newAgentIP);
        Mockito.when(distributedPhaseMonitorMock.isPhaseExecuting()).thenReturn(true);

        RequestProcessorDispatcher.getInstance().getRequestProcessor(mockDriver)
                .processRegisterRequest(requestMock, mockDriver);
        Mockito.verify(taskBalancerMock, Mockito.times(1))
                .scheduleWorkRedistributionProcess(mockDriver, newAgentIP, true);

    }
}
