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

package org.apache.inlong.sort.parser.impl;

import com.google.common.base.Preconditions;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.inlong.sort.function.EncryptFunction;
import org.apache.inlong.sort.function.JsonGetterFunction;
import org.apache.inlong.sort.function.RegexpReplaceFirstFunction;
import org.apache.inlong.sort.function.RegexpReplaceFunction;
import org.apache.inlong.sort.parser.Parser;
import org.apache.inlong.sort.parser.result.FlinkSqlParseResult;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * parse flink sql script file
 * script file include CREATE TABLE statement
 */
public class NativeFlinkSqlParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(FlinkSqlParser.class);

    private final TableEnvironment tableEnv;
    private final String statements;

    public NativeFlinkSqlParser(TableEnvironment tableEnv, String statements) {
        this.tableEnv = tableEnv;
        this.statements = statements;
        registerUDF();
    }

    /**
     * Get a instance of NativeFlinkSqlParser
     *
     * @param tableEnv The tableEnv,it is the execution environment of flink sql
     * @param statements The statements, it is statement set
     * @return NativeFlinkSqlParser The flink sql parse handler
     */
    public static NativeFlinkSqlParser getInstance(TableEnvironment tableEnv, String statements) {
        return new NativeFlinkSqlParser(tableEnv, statements);
    }

    /**
     * Register udf
     */
    private void registerUDF() {
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE_FIRST", RegexpReplaceFirstFunction.class);
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE", RegexpReplaceFunction.class);
        tableEnv.createTemporarySystemFunction("ENCRYPT", EncryptFunction.class);
        tableEnv.createTemporarySystemFunction("JSON_GETTER", JsonGetterFunction.class);
    }

    /**
     * parse flink sql script file
     *
     * @return ParseResult the result of parsing
     */
    @Override
    public ParseResult parse() {
        Preconditions.checkNotNull(statements, "sql statement set is null");
        Preconditions.checkNotNull(tableEnv, "tableEnv is null");
        List<String> createTableSqls = new ArrayList<>();
        List<String> insertSqls = new ArrayList<>();
        String[] statementSet = statements.split(";(\\r?\\n|\\r)");
        for (String statement : statementSet) {
            statement = statement.trim();
            if (statement.toUpperCase(Locale.ROOT).startsWith("CREATE TABLE") || statement.toUpperCase(Locale.ROOT)
                    .startsWith("CREATE VIEW")) {
                createTableSqls.add(statement);
            } else if (statement.toUpperCase(Locale.ROOT).startsWith("INSERT INTO")) {
                insertSqls.add(statement);
            } else if (!statement.isEmpty()) {
                log.warn("Not support sql statement: " + statement);
            }
        }
        return new FlinkSqlParseResult(tableEnv, createTableSqls, insertSqls);
    }
}
