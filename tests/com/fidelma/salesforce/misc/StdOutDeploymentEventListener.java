package com.fidelma.salesforce.misc;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 29/05/11
 * Time: 7:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class StdOutDeploymentEventListener implements DeploymentEventListener {
    public void error(String message) {
        System.out.println(message);
    }

    public void finished(String message) {
        System.out.println(message);
    }
}
