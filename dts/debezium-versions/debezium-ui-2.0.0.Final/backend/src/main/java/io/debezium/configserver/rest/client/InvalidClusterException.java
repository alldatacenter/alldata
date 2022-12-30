/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest.client;

public class InvalidClusterException extends KafkaConnectException {

    public InvalidClusterException(String s, Exception e) {
        super(s, e);
    }

    public InvalidClusterException(String s) {
        super(s);
    }
}
