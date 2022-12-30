/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.Map;
import java.util.stream.Collectors;

public class ConnectConnectorConfigResponse {

    private String name;
    private Map<String, String> config;

    public ConnectConnectorConfigResponse() {
    }

    public ConnectConnectorConfigResponse(String name) {
        this.name = name;
    }

    public ConnectConnectorConfigResponse(String name, Map<String, ?> config) {
        this.name = name;
        setConfig(config);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, ?> config) {
        this.config = config.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
    }
}
