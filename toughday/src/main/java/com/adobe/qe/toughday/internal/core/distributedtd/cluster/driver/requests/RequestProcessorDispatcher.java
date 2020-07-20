package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests;

import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;

/**
 * Class responsible for choosing the appropriate RequestProcessor implementation to be used for processing requests by
 * the driver, depending on the role played by the Driver receiving the request (Master or Candidate).
 */
public class RequestProcessorDispatcher {
    private static RequestProcessorDispatcher instance = null;

    private RequestProcessorDispatcher() { }

    /**
     * Returns an instance of this class.
     */
    public static RequestProcessorDispatcher getInstance() {
        if (instance == null) {
            instance = new RequestProcessorDispatcher();
        }

        return instance;
    }

    /**
     * Returns the appropriate RequestProcessor to be used for processing the requests received by the driver.
     * @param driver : the Driver instance that must process the http requests.
     */
    public RequestProcessor getRequestProcessor(Driver driver) {
        if (driver.getDriverState().getCurrentState() == DriverState.Role.MASTER) {
            return MasterRequestProcessor.getInstance(driver);
        } else {
            return CandidateRequestProcessor.getInstance(driver);
        }

    }
}
