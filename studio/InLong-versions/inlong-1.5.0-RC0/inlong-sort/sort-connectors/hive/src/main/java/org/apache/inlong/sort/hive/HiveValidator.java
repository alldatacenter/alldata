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

package org.apache.inlong.sort.hive;

import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;

/**
 * hive validator for hive properties
 */
public class HiveValidator extends ConnectorDescriptorValidator {

    public static final String CONNECTOR_TYPE_VALUE_HIVE = "hive";
    public static final String CONNECTOR_HIVE_VERSION = "hive-version";
    public static final String CONNECTOR_VERSION_VALUE_2_2_0 = "2.2.0";
    public static final String CONNECTOR_VERSION_VALUE_2_3_6 = "2.3.6";
    public static final String CONNECTOR_VERSION_VALUE_3_1_3 = "3.1.3";
    public static final String CONNECTOR_VERSION_VALUE_1_2_2 = "1.2.2";
    public static final String CONNECTOR_HIVE_DATABASE = "default-database";
    public static final String CONNECTOR_HIVE_TABLE = "table-name";
    /**
     * Key for describing the property version. This property can be used for backwards
     * compatibility in case the property format changes.
     */
    public static final String HIVE_PROPERTY_VERSION = "property-version";

    @Override
    public void validate(DescriptorProperties properties) {
        properties.validateValue(CONNECTOR, CONNECTOR_TYPE_VALUE_HIVE, false);
        properties.validateString(CONNECTOR_HIVE_DATABASE, false, 1);
        properties.validateString(CONNECTOR_HIVE_VERSION, false, 1);
    }

}
