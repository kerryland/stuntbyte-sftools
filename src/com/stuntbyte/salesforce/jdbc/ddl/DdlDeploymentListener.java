package com.stuntbyte.salesforce.jdbc.ddl;

import com.stuntbyte.salesforce.misc.BaseDeploymentEventListener;

/**
 * Listen for deployments
 */
public class DdlDeploymentListener extends BaseDeploymentEventListener {

    public StringBuilder errors;
    public StringBuilder messages;

    public DdlDeploymentListener(StringBuilder errors, StringBuilder messages) {
        this.errors = errors;
        this.messages = messages;
    }

    public void error(String message) {
        if (errors != null) {
            errors.append(message).append("\n");
        }
    }

    public void message(String message) {
        if (messages != null) {
            messages.append(message).append("\n");
        }
    }

}
