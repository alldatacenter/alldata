/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

public class GenericValidationResult {

    public String message;
    public String trace;

    public GenericValidationResult() {
    }

    public GenericValidationResult(String message, String trace) {
        this.message = message;
        this.trace = trace;
    }
}
