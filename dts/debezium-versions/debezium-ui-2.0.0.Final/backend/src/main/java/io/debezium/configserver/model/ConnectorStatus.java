/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectorStatus {

    private String name;
    private State connectorStatus;
    private String connectorType;
    private String databaseName;
    private final Map<Long, TaskStatus> taskStates = new HashMap<>();

    public enum State {
        UNASSIGNED,
        RUNNING,
        PAUSED,
        FAILED,
        DESTROYED
    }

    public ConnectorStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getConnectorStatus() {
        return connectorStatus;
    }

    public void setConnectorStatus(State connectorStatus) {
        this.connectorStatus = connectorStatus;
    }

    public Map<Long, TaskStatus> getTaskStates() {
        return taskStates;
    }

    public TaskStatus getTaskState(Long taskNumber) {
        return taskStates.get(taskNumber);
    }

    public void setTaskState(Long taskNumber, State state, List<String> errors) {
        this.taskStates.put(taskNumber, new TaskStatus(state, errors));
    }

    public void setConnectorType(String connectorClassName) {
        switch (connectorClassName) {
            case "io.debezium.connector.postgresql.PostgresConnector":
                this.connectorType = "postgres";
                this.databaseName = "PostgreSQL";
                break;
            case "io.debezium.connector.mongodb.MongoDbConnector":
                this.connectorType = "mongodb";
                this.databaseName = "MongoDB";
                break;
            case "io.debezium.connector.mysql.MySqlConnector":
                this.connectorType = "mysql";
                this.databaseName = "MySQL";
                break;
            case "io.debezium.connector.sqlserver.SqlServerConnector":
                this.connectorType = "sqlserver";
                this.databaseName = "SQL Server";
                break;
            case "io.debezium.connector.oracle.OracleConnector":
                this.connectorType = "oracle";
                this.databaseName = "Oracle";
                break;
            default:
                this.connectorType = connectorClassName;
                this.databaseName = "unknown";
                break;
        }
    }

    public String getConnectorType() {
        return connectorType;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String toString() {
        return "ConnectorStatus{" +
                "connectorType='" + connectorType + '\'' +
                ", databaseName=" + databaseName +
                ", connectorState=" + connectorStatus +
                ", taskStates=" + taskStates +
                '}';
    }
}
