/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

public class FrontendConfigArtifacts {

    public final String type;

    public FrontendConfigArtifacts(String type) {
        this.type = type;
    }
}
