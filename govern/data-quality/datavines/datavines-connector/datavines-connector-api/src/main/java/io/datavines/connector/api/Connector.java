/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.connector.api;

import io.datavines.common.param.*;

import java.sql.SQLException;

public interface Connector {

    /**
     * get databases
     * @param param GetDatabasesRequestParam
     * @return
     */
    default ConnectorResponse getDatabases(GetDatabasesRequestParam param) throws SQLException {
        return null;
    }

    /**
     * get tables
     * @param param GetTablesRequestParam
     * @return
     */
    default ConnectorResponse getTables(GetTablesRequestParam param) throws SQLException {
        return null;
    }

    /**
     * get columns
     * @param param GetColumnsRequestParam
     * @return
     */
    default ConnectorResponse getColumns(GetColumnsRequestParam param) throws SQLException {
        return null;
    }

    /**
     * get partitions
     * @param param ConnectorRequestParam
     * @return
     */
    default ConnectorResponse getPartitions(ConnectorRequestParam param) {
        return null;
    }

    /**
     * get frontend config json
     * @return String
     */
    default String getConfigJson(boolean isEn) {
        return null;
    }

    /**
     * test connect
     * @param param TestConnectionRequestParam
     * @return
     */
    default ConnectorResponse testConnect(TestConnectionRequestParam param) {
        return null;
    }

    /**
     * test connect
     * @param param TestConnectionRequestParam
     * @return
     */
    default ConnectorResponse executeQuery(TestConnectionRequestParam param) {
        return null;
    }
}
