package com.fidelma.salesforce.misc;

import java.io.IOException;

/**
 */
public interface DeploymentEventListener {
    void progress(String message) throws IOException;
    void error(String message) throws Exception;
    void message(String message) throws IOException;
}
