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

import org.apache.flink.table.descriptors.ConnectorDescriptor;
import org.apache.flink.table.descriptors.DescriptorProperties;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The {@link ConnectorDescriptor} for kudu sinks.
 */
public class Kudu extends ConnectorDescriptor {

    @Nonnull
    private final Map<String, String> properties;
    @Nonnull
    private String table;
    @Nonnull
    private String masters;

    /**
     * Kudu connectorDescriptor.
     */
    public Kudu() {
        super(KuduValidator.CONNECTOR_TYPE_VALUE_KUDU, 1, true);

        this.properties = new HashMap<>();
    }

    /**
     * Sets the kudu table name.
     *
     * @param table The kudu table name.
     */
    public Kudu table(String table) {
        checkNotNull(table);
        this.table = table;
        return this;
    }

    /**
     * Sets the kudu server masters.
     *
     * @param masters the kudu server masters.
     */
    public Kudu masters(String masters) {
        checkNotNull(masters);
        this.masters = masters;
        return this;
    }

    /**
     * Sets the kudu property.
     *
     * @param key   The key of the property.
     * @param value The value of the property.
     */
    public Kudu property(String key, String value) {
        checkNotNull(key);
        checkNotNull(value);

        properties.put(key, value);
        return this;
    }

    @SuppressWarnings("checkstyle:FileTabCharacter")
    @Override
    protected Map<String, String> toConnectorProperties() {
        DescriptorProperties descriptorProperties = new DescriptorProperties();

        descriptorProperties.putString(KuduValidator.CONNECTOR_TABLE, table);
        descriptorProperties.putString(KuduValidator.CONNECTOR_MASTERS, masters);

        descriptorProperties
                .putPropertiesWithPrefix(KuduValidator.CONNECTOR_PROPERTIES, properties);

        return descriptorProperties.asMap();
    }
}
