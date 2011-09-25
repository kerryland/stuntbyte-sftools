package com.fidelma.salesforce.deployment;

/**
 */
public class DeploymentEventListenerImpl implements DeploymentEventListener {
    final StringBuilder errors = new StringBuilder();
    final StringBuilder messages = new StringBuilder();

    public void error(String message) {
        System.out.println("ERROR " + message);
        errors.append(message).append(".\n");
    }

    public void message(String message) {
        System.out.println("MESSAGE " + message);
        messages.append(message).append(".\n");
    }

    public void progress(String message) {
        System.out.println("PROGRESS " + message);
        messages.append(message);
    }

    public StringBuilder getErrors() {
        return errors;
    }

    public StringBuilder getMessages() {
        return messages;
    }
}
