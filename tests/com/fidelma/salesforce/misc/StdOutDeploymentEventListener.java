package com.fidelma.salesforce.misc;

/**
 * Write deployment messages and errors to stdout
 */
public class StdOutDeploymentEventListener implements DeploymentEventListener {
    public void error(String message) {
        System.out.println(message);
    }

    public void message(String message) {
        System.out.println(message);
    }

    public void progress(String message) {

    }
}
