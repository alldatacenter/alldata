/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.Collections;
import java.util.List;

public class PropertiesValidationResult {

    public Status status;
    public List<PropertyValidationResult> propertyValidationResults;

    public PropertiesValidationResult(Status status, List<PropertyValidationResult> propertyValidationResults) {
        this.status = status;
        this.propertyValidationResults = propertyValidationResults;
    }

    public static PropertiesValidationResult valid() {
        return new PropertiesValidationResult(Status.VALID, Collections.emptyList());
    }

    public static PropertiesValidationResult invalid(List<PropertyValidationResult> propertyValidationResults) {
        return new PropertiesValidationResult(Status.INVALID, propertyValidationResults);
    }

    public static enum Status {
        VALID, INVALID;
    }
}
