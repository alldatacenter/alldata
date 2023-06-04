/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.parser.result;

import static org.apache.inlong.common.util.MaskDataUtils.maskSensitiveMessage;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;

import java.io.Serializable;
import java.util.List;

/**
 * Flink sql parse result, It is a concrete implementation of ParseResult
 */
@Data
@Slf4j
public class FlinkSqlParseResult implements ParseResult, Serializable {

    private static final long serialVersionUID = -28762188896227462L;

    private final TableEnvironment tableEnv;
    private final List<String> createTableSqls;
    private final List<String> loadSqls;

    /**
     * The constructor of FlinkSqlParseResult
     *
     * @param tableEnv        The tableEnv,it is the execution environment of flink sql
     * @param createTableSqls The createTableSqls,it is a collection of  create table sql
     * @param loadSqls        The loadSqls,it is a collection of sql that load data into table
     */
    public FlinkSqlParseResult(TableEnvironment tableEnv, List<String> createTableSqls, List<String> loadSqls) {
        this.tableEnv = Preconditions.checkNotNull(tableEnv, "tableEnv is null");
        this.createTableSqls = Preconditions.checkNotNull(createTableSqls, "createTableSqls is null");
        Preconditions.checkState(!createTableSqls.isEmpty(), "createTableSqls is empty");
        this.loadSqls = Preconditions.checkNotNull(loadSqls, "loadSqls is null");
        Preconditions.checkState(!loadSqls.isEmpty(), "loadSqls is empty");
    }

    @Override
    public void execute() throws Exception {
        executeCreateTableSqls(createTableSqls);
        executeLoadSqls(loadSqls);
    }

    @Override
    public void waitExecute() throws Exception {
        executeCreateTableSqls(createTableSqls);
        executeLoadSqls(loadSqls).await();
    }

    @Override
    public boolean tryExecute() throws Exception {
        executeCreateTableSqls(createTableSqls);
        return executeLoadSqls(loadSqls) != null;
    }

    private TableResult executeLoadSqls(List<String> sqls) {
        StatementSet st = tableEnv.createStatementSet();
        for (String sql : sqls) {
            log.info("execute loadSql:\n{}", sql);
            st.addInsertSql(sql);
        }
        return st.execute();
    }

    private void executeCreateTableSqls(List<String> sqls) {
        for (String sql : sqls) {
            log.info("execute createSql:\n{}", maskSensitiveMessage(sql));
            tableEnv.executeSql(sql);
        }
    }

}
