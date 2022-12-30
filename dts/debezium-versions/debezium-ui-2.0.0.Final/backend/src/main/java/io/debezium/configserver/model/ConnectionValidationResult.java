/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.Collections;
import java.util.List;

public class ConnectionValidationResult {

    public Status status;
    public List<PropertyValidationResult> propertyValidationResults;
    public List<GenericValidationResult> genericValidationResults;

    public ConnectionValidationResult(Status status, List<PropertyValidationResult> propertyValidationResults, List<GenericValidationResult> genericValidationResults) {
        this.status = status;
        this.propertyValidationResults = propertyValidationResults;
        this.genericValidationResults = genericValidationResults;
    }

    public static ConnectionValidationResult valid() {
        return new ConnectionValidationResult(Status.VALID, Collections.emptyList(), Collections.emptyList());
    }

    public static ConnectionValidationResult invalid(List<PropertyValidationResult> propertyValidationResults) {
        return invalid(propertyValidationResults, Collections.emptyList());
    }

    public static ConnectionValidationResult invalid(List<PropertyValidationResult> propertyValidationResults, List<GenericValidationResult> genericValidationResults) {
        return new ConnectionValidationResult(Status.INVALID, propertyValidationResults, genericValidationResults);
    }

    public static enum Status {
        VALID, INVALID;
    }
}
