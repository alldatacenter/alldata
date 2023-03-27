/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.List;

public class ConnectorProperty {

    public enum Type {
        BOOLEAN, STRING, INT, SHORT, LONG, DOUBLE, LIST, CLASS, PASSWORD;
    }

    public enum Category {
        CONNECTION, CONNECTION_ADVANCED, CONNECTION_ADVANCED_SSL, CONNECTION_ADVANCED_REPLICATION, CONNECTION_ADVANCED_PUBLICATION, FILTERS, CONNECTOR, CONNECTOR_SNAPSHOT, CONNECTOR_ADVANCED, ADVANCED, ADVANCED_HEARTBEAT
    }

    public final String name;
    public final String displayName;
    public final String description;
    public final Type type;
    public final Object defaultValue;
    public final boolean isMandatory;
    public final Category category;
    public final List<String> allowedValues;

    public ConnectorProperty(String name, String displayName, String description, Type type, Object defaultValue, boolean isMandatory, Category category, List<String> allowedValues) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue instanceof Class ? ((Class<?>) defaultValue).getName() : defaultValue;
        this.isMandatory = isMandatory;
        this.category = category;
        this.allowedValues = allowedValues;
    }

}
