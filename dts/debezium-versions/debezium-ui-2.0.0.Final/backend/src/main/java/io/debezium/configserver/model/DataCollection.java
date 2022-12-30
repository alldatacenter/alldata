/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

public class DataCollection {

    // schema or catalog
    public String namespace;
    public String name;

    public DataCollection(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }
}
