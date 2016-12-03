package com.stuntbyte.salesforce.deployment;

import java.io.IOException;

/**
 * For objects that are interested in the Deployment process
 */
public interface DeploymentEventListener {
    void progress(String message) throws IOException;
    void error(String message) throws Exception;
    void message(String message) throws IOException;
}
