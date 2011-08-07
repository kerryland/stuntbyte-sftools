package com.fidelma.salesforce.misc;

import java.io.IOException;

/**
 */
public interface DeploymentEventListener {
    void error(String message) throws Exception;
    void finished(String message) throws IOException;

    void progress(String message) throws IOException;
}
