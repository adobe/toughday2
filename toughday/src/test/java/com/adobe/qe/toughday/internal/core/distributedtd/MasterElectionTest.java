package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.MasterElection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MasterElectionTest {
    private MasterElection masterElection = MasterElection.getInstance(3);

    @Mock Driver mockDriver;
    @Mock DriverState driverStateMock;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();

    }

    @Before
    public void setup() {
        this.masterElection.resetInvalidCandidates();
    }

    @Test
    public void testAllCandidatesAreConsideredValidAfterTheInstanceIsCreated() {
        Assert.assertTrue( MasterElection.getInstance(3).getInvalidCandidates().isEmpty());
    }

    @Test
    public void testMarkCandidateAsInvalid() {
        this.masterElection.markCandidateAsInvalid(0);
        Queue<Integer> expectedInvalidCandidates = new LinkedList<Integer>() {{
            add(0);
        }};
        Assert.assertEquals(expectedInvalidCandidates, masterElection.getInvalidCandidates());
    }

    @Test
    public void testIsCandidateInvalid() {
        this.masterElection.markCandidateAsInvalid(0);

        Assert.assertTrue(this.masterElection.isCandidateInvalid(0));
        Assert.assertFalse(this.masterElection.isCandidateInvalid(1));
        Assert.assertFalse(this.masterElection.isCandidateInvalid(2));
    }

    @Test
    public void testResetInvalidCandidates() {
        this.masterElection.markCandidateAsInvalid(0);
        Assert.assertEquals(1, this.masterElection.getInvalidCandidates().size());

        this.masterElection.resetInvalidCandidates();
        Assert.assertTrue(this.masterElection.getInvalidCandidates().isEmpty());
    }

    @Test
    public void checkListOfInvalidCandidatesIsClearedWhenNoOptionAvailable() {
        int currentDriverId = 2;

        Mockito.when(mockDriver.getDriverState()).thenReturn(driverStateMock);
        Mockito.when(driverStateMock.getId()).thenReturn(currentDriverId);
        Mockito.when(driverStateMock.getMasterIdLock()).thenReturn(new ReentrantReadWriteLock());

        masterElection.markCandidateAsInvalid(0);
        masterElection.markCandidateAsInvalid(1);
        masterElection.markCandidateAsInvalid(2);

        // before starting the election process all candidates should be considered invalid
        Assert.assertEquals(3, masterElection.getInvalidCandidates().size());

        masterElection.electMaster(mockDriver);

        // after the election process, all candidates should be considered eligible to become the new master
        Assert.assertTrue(masterElection.getInvalidCandidates().isEmpty());
    }

    @Test
    public void testElectMasterFromCandidatePerspective() {
        int expectedMaster = 1;
        int currentDriverId = 2;

        Mockito.when(mockDriver.getDriverState()).thenReturn(driverStateMock);
        Mockito.when(driverStateMock.getId()).thenReturn(currentDriverId);
        Mockito.when(driverStateMock.getMasterIdLock()).thenReturn(new ReentrantReadWriteLock());

        masterElection.markCandidateAsInvalid(0);
        masterElection.electMaster(mockDriver);

        // check that the list of invalid candidates was not modified
        Queue<Integer> expectedInvalidCandidates = new LinkedList<Integer>() {{
            add(0);
        }};
        Assert.assertEquals(expectedInvalidCandidates, masterElection.getInvalidCandidates());

        // check that current master (with id 2) is running as candidate
        Mockito.verify(driverStateMock, Mockito.times(1))
                .setCurrentState(DriverState.Role.CANDIDATE);

        // check that driver-1 was elected as master
        Mockito.verify(driverStateMock, Mockito.times(1)).setMasterId(expectedMaster);

        // check that the master election heartbeat task was scheduled
        Mockito.verify(mockDriver, Mockito.times(1)).scheduleMasterHeartbeatTask();
    }


}
