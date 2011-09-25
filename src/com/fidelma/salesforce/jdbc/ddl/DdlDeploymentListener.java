package com.fidelma.salesforce.jdbc.ddl;

import com.fidelma.salesforce.misc.BaseDeploymentEventListener;

/**
 * Created by IntelliJ IDEA.
 * User: kerry
 * Date: 25/09/11
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class DdlDeploymentListener extends BaseDeploymentEventListener {

    public StringBuilder errors;
    public StringBuilder messages;

    public DdlDeploymentListener() {
        errors = new StringBuilder();
        messages = new StringBuilder();
    }

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
