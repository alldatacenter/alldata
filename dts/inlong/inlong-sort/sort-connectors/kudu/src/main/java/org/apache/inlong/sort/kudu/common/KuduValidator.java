/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.kudu.common;

import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;

/**
 * The validator for {@link Kudu}.
 */
public class KuduValidator extends ConnectorDescriptorValidator {

    /**
     * The type of connector.
     */
    public static final String CONNECTOR_TYPE_VALUE_KUDU = "kudu-inlong";

    /**
     * The name of kudu table.
     */
    public static final String CONNECTOR_TABLE = "table-name";

    /**
     * The masters of kudu server.
     */
    public static final String CONNECTOR_MASTERS = "masters";

    /**
     * The prefix of kudu properties (optional).
     */
    public static final String CONNECTOR_PROPERTIES = "connector.properties";

    @Override
    public void validate(DescriptorProperties properties) {
        super.validate(properties);

        // Validates that the connector type is kudu.
        properties.validateValue(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE_KUDU, false);
        properties.validateString(CONNECTOR_TABLE, false);
        properties.validateString(CONNECTOR_MASTERS, false);
    }

}
