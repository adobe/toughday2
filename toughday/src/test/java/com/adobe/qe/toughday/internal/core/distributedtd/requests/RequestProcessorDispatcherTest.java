package com.adobe.qe.toughday.internal.core.distributedtd.requests;

import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.MasterRequestProcessor;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessorDispatcher;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.CandidateRequestProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

public class RequestProcessorDispatcherTest {
    @Mock Driver mockDriver;
    @Mock DriverState driverStateMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }


    @Test
    public void testGetInstance() {
        RequestProcessorDispatcher requestProcessorDispatcher = RequestProcessorDispatcher.getInstance();

        Assert.assertNotNull(requestProcessorDispatcher);
    }

    @Test
    public void testMasterRequestProcessorIsReturnedForMaster() {
        Mockito.when(mockDriver.getDriverState()).then((Answer<DriverState>) invocationOnMocl -> driverStateMock);
        Mockito.when(driverStateMock.getCurrentState()).then((Answer<DriverState.Role>) invocationOnMock -> DriverState.Role.MASTER);

        RequestProcessorDispatcher requestProcessorDispatcher = RequestProcessorDispatcher.getInstance();
        Assert.assertTrue(requestProcessorDispatcher.getRequestProcessor(mockDriver) instanceof MasterRequestProcessor);
    }

    @Test
    public void testCandidateRequestProcessorIsReturnedForCandidate() {
        Mockito.when(mockDriver.getDriverState()).then((Answer<DriverState>) invocationOnMocl -> driverStateMock);
        Mockito.when(driverStateMock.getCurrentState()).then((Answer<DriverState.Role>) invocationOnMock -> DriverState.Role.CANDIDATE);

        RequestProcessorDispatcher requestProcessorDispatcher = RequestProcessorDispatcher.getInstance();
        Assert.assertTrue(requestProcessorDispatcher.getRequestProcessor(mockDriver) instanceof CandidateRequestProcessor);
    }

}
