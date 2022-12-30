/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

public class ConnectorDefinition {

    public String id;
    public String className;
    public String displayName;
    public String version;
    public boolean enabled;

    public ConnectorDefinition() {
    }

    public ConnectorDefinition(String id, String className, String displayName, String version, boolean enabled) {
        this.id = id;
        this.className = className;
        this.displayName = displayName;
        this.version = version;
        this.enabled = enabled;
    }

}
