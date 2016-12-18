/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.salesforce.deployment;

/**
 */
public class DeploymentEventListenerImpl { // implements DeploymentEventListener {
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
//
//    public StringBuilder getMessages() {
//        return messages;
//    }
}
