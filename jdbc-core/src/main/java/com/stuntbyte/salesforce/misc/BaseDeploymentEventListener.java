package com.stuntbyte.salesforce.misc;

import com.stuntbyte.salesforce.deployment.DeploymentEventListener;

import java.io.IOException;

/**
 */
public class BaseDeploymentEventListener implements DeploymentEventListener {
    public void progress(String message) throws IOException {
    }

    public void error(String message) throws Exception {
    }

    public void message(String message) throws IOException {
    }


}
