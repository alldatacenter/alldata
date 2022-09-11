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

import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_GROUP;
import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_MASTER;
import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_PROPERTIES;
import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_TIDS;
import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_TOPIC;
import static org.apache.flink.connectors.tubemq.TubemqValidator.CONNECTOR_TYPE_VALUE_TUBEMQ;
import static org.apache.flink.util.Preconditions.checkNotNull;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.flink.table.descriptors.ConnectorDescriptor;
import org.apache.flink.table.descriptors.DescriptorProperties;

/**
 * The {@link ConnectorDescriptor} for tubemq sources and sinks.
 */
public class Tubemq extends ConnectorDescriptor {

    @Nullable
    private boolean consumerRole = true;

    @Nullable
    private String topic;

    @Nullable
    private String master;

    @Nullable
    private String group;

    @Nullable
    private String tids;

    @Nonnull
    private Map<String, String> properties;

    public Tubemq() {
        super(CONNECTOR_TYPE_VALUE_TUBEMQ, 1, true);

        this.properties = new HashMap<>();
    }

    /**
     * Sets the tubemq topic to be used.
     *
     * @param topic The topic name.
     */
    public Tubemq topic(String topic) {
        checkNotNull(topic);

        this.topic = topic;
        return this;
    }

    /**
     * Sets the client role to be used.
     *
     * @param isConsumer The client role if consumer.
     */
    public Tubemq asConsumer(boolean isConsumer) {
        this.consumerRole = isConsumer;
        return this;
    }

    /**
     * Sets the address of tubemq master to connect.
     *
     * @param master The address of tubemq master.
     */
    public Tubemq master(String master) {
        checkNotNull(master);

        this.master = master;
        return this;
    }

    /**
     * Sets the tubemq (consumer or producer) group to be used.
     *
     * @param group The group name.
     */
    public Tubemq group(String group) {
        checkNotNull(group);

        this.group = group;
        return this;
    }

    /**
     * The tubemq consumers use these tids to filter records reading from server.
     *
     * @param tids The filter for consume record from server.
     */
    public Tubemq tids(String tids) {

        this.tids = tids;
        return this;
    }

    /**
     * Sets the tubemq property.
     *
     * @param key   The key of the property.
     * @param value The value of the property.
     */
    public Tubemq property(String key, String value) {
        checkNotNull(key);
        checkNotNull(value);

        properties.put(key, value);
        return this;
    }

    @Override
    protected Map<String, String> toConnectorProperties() {
        DescriptorProperties descriptorProperties = new DescriptorProperties();

        if (topic != null) {
            descriptorProperties.putString(CONNECTOR_TOPIC, topic);
        }

        if (master != null) {
            descriptorProperties.putString(CONNECTOR_MASTER, master);
        }
        if (consumerRole) {
            if (group != null) {
                descriptorProperties.putString(CONNECTOR_GROUP, group);
            }

            if (tids != null) {
                descriptorProperties.putString(CONNECTOR_TIDS, tids);
            }
        }

        descriptorProperties.putPropertiesWithPrefix(CONNECTOR_PROPERTIES, properties);

        return descriptorProperties.asMap();
    }
}
