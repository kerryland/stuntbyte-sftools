package com.fidelma.salesforce.misc;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 25/06/11
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class DeploymentEventListenerImpl implements DeploymentEventListener {
    final StringBuilder errors = new StringBuilder();
    final StringBuilder messages = new StringBuilder();

    public void error(String message) {
        errors.append(message).append(".\n");
    }

    public void finished(String message) {
        messages.append(message).append(".\n");
    }

    public void progress(String message) {
        messages.append(message);
    }

    public StringBuilder getErrors() {
        return errors;
    }

    public StringBuilder getMessages() {
        return messages;
    }
}
