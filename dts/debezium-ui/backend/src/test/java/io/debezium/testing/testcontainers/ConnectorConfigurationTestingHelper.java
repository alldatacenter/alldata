/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.testing.testcontainers;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConnectorConfigurationTestingHelper {

    public static ObjectNode getConfig(ConnectorConfiguration config) {
        return config.getConfiguration();
    }

}
