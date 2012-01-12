package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.deployment.DeploymentEventListener;
import com.sforce.soap.metadata.AsyncResult;

import java.io.IOException;

/**
 */
public class BaseDeploymentEventListener implements DeploymentEventListener {
    private AsyncResult asyncResult;

    public void progress(String message) throws IOException {
    }

    public void error(String message) throws Exception {
    }

    public void message(String message) throws IOException {
    }

    public void setAsyncResult(AsyncResult asyncResult) {
        this.asyncResult = asyncResult;
    }

    public AsyncResult getAsyncResult() {
        return asyncResult;
    }
}
