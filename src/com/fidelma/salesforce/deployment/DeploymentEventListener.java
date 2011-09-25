package com.fidelma.salesforce.deployment;

import com.sforce.soap.metadata.AsyncResult;

import java.io.IOException;

/**
 */
public interface DeploymentEventListener {
    void progress(String message) throws IOException;
    void error(String message) throws Exception;
    void message(String message) throws IOException;

    void setAsyncResult(AsyncResult asyncResult);
}
