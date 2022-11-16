/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

public class PropertyValidationResult {

    public String property;
    public String message;

    public PropertyValidationResult() {
    }

    public PropertyValidationResult(String property, String message) {
        this.property = property;
        this.message = message;
    }
}
