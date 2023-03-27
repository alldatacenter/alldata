/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.List;

public class TaskStatus {

    public final ConnectorStatus.State taskStatus;
    public List<String> errors;

    public TaskStatus(ConnectorStatus.State taskStatus, List<String> errors) {
        this.taskStatus = taskStatus;
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "TaskStatus{" +
                "taskStatus='" + taskStatus + '\'' +
                ", errors=" + errors +
                '}';
    }
}
