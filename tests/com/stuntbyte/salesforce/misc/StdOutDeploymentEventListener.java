package com.stuntbyte.salesforce.misc;

/**
 * Write deployment messages and errors to stdout
 */
public class StdOutDeploymentEventListener extends BaseDeploymentEventListener {
    public void error(String message) {
        System.out.println(message);
    }

    public void message(String message) {
        System.out.println(message);
    }
}
