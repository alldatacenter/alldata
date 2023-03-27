/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import javax.json.bind.annotation.JsonbProperty;

public class ConnectConnectorStatus {

    @JsonbProperty("state")
    public ConnectorStatus.State status;

    @JsonbProperty("worker_id")
    public String workerId;

    @Override
    public String toString() {
        return "ConnectConnectorStatus{" +
                "status=" + status +
                ", workerId='" + workerId + '\'' +
                '}';
    }
}
