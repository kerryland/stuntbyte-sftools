package com.fidelma.salesforce.misc;

import com.fidelma.salesforce.deployment.DeploymentEventListener;
import com.sforce.soap.metadata.AsyncResult;

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
