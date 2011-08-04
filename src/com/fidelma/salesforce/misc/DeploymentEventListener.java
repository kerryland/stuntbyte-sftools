package com.fidelma.salesforce.misc;

/**
 */
public interface DeploymentEventListener {
    void error(String message);
    void finished(String message);

    void progress(String message);
}
