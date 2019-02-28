package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.api.annotations.labels.Nullable;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;

/**
 * Class responsible for handling the communication between the agents and the drivers running in the cluster.
 */
public class HttpUtils {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    public static final String POST_METHOD = "POST";
    public static final String GET_METHOD = "GET";
    public static final String URL_PREFIX = "http://";
    public static final int HTTP_REQUEST_RETRIES = 3;

    private HttpResponse sendGetRequest(String URI) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(URI);

        try {
            return httpClient.execute(request);
        } catch (IOException e) {
            LOG.warn("Http request could not be sent to  " + URI + ". Received error " + e.getMessage());
        }

        return null;
    }

    private HttpResponse sendPostRequest(String requestContent, String URI) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(URI);

        try {
            StringEntity params = new StringEntity(requestContent);
            request.setEntity(params);
            request.setHeader("Content-type", "text/plain");

            // submit request and wait for ack from agent
            return httpClient.execute(request);
        } catch (IOException e)  {
            LOG.warn("Http request could not be sent to  " + URI + ". Received error " + e.getMessage());
        }

        return null;
    }

    private boolean checkSuccessfulRequest(HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() >= 200 &&
                response.getStatusLine().getStatusCode() < 300;
    }

    /**
     * Method used for sending http requests.
     * @param requestContent content of the request
     * @param URI the path that uniquely identifies the component in the cluster and the http endpoint to be used
     *           when sending the request
     * @param retries how many times to retry sending the request in case of failure
     * @return true is the the request is successfully sent, false otherwise.
     */
    @Nullable
    public HttpResponse sendHttpRequest(String requestType, String requestContent, String URI, int retries) {
        HttpResponse response = null;

        while (retries > 0) {
            if (requestType.equals(POST_METHOD)) {
                response = sendPostRequest(requestContent, URI);
            } else if (requestType.equals(GET_METHOD)) {
                response = sendGetRequest(URI);
            }

            if (checkSuccessfulRequest(response)) {
                return response;
            }

            retries--;
        }

        return null;
    }

}
