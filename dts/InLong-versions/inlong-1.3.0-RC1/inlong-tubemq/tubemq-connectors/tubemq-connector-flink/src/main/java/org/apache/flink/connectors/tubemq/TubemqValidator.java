/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.connectors.tubemq;

import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;

/**
 * The validator for {@link Tubemq}.
 */
public class TubemqValidator extends ConnectorDescriptorValidator {

    /**
     * The type of connector.
     */
    public static final String CONNECTOR_TYPE_VALUE_TUBEMQ = "tubemq";

    /**
     * The address of tubemq master.
     */
    public static final String CONNECTOR_MASTER = "connector.master";

    /**
     * The tubemq topic name.
     */
    public static final String CONNECTOR_TOPIC = "connector.topic";

    /**
     * The tubemq (consumer or producer) group name.
     */
    public static final String CONNECTOR_GROUP = "connector.group";

    /**
     * The tubemq consumers use these tids to filter records reading from server.
     */
    public static final String CONNECTOR_TIDS = "connector.tids";

    /**
     * The prefix of tubemq properties (optional).
     */
    public static final String CONNECTOR_PROPERTIES = "connector.properties";

    @Override
    public void validate(DescriptorProperties properties) {
        super.validate(properties);

        // Validates that the connector type is tubemq.
        properties.validateValue(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE_TUBEMQ, false);

        // Validate that the topic name is set.
        properties.validateString(CONNECTOR_TOPIC, false, 1, Integer.MAX_VALUE);

        // Validate that the master address is set.
        properties.validateString(CONNECTOR_MASTER, false, 1, Integer.MAX_VALUE);

        // Validate that the group name is set.
        properties.validateString(CONNECTOR_GROUP, false, 1, Integer.MAX_VALUE);

        // Validate that the tids is set.
        properties.validateString(CONNECTOR_TIDS, true, 1, Integer.MAX_VALUE);
    }
}
