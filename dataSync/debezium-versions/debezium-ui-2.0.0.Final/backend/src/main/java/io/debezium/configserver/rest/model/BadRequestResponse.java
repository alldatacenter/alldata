/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest.model;

public class BadRequestResponse {

    public String message;

    public BadRequestResponse(String message) {
        this.message = message;
    }
}
