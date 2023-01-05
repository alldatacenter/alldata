/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.sql.parser.visitor.BlockScriptBuffer;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-07 13:45
 **/
public abstract class CreateTableSqlBuilder {
    protected final IDataxProcessor.TableMap tableMapper;
    public BlockScriptBuffer script;
    protected final List<ColWrapper> pks;
    public int maxColNameLength;
    private final String escapeChar;

    public CreateTableSqlBuilder(IDataxProcessor.TableMap tableMapper, DataSourceMeta dsMeta) {
        this.tableMapper = tableMapper;
        this.script = new BlockScriptBuffer();
        this.pks = this.getCols().stream()
                .filter((c) -> c.isPk())
                .map((c) -> createColWrapper(c))
                .collect(Collectors.toList());

        maxColNameLength = 0;
        for (CMeta col : this.getCols()) {
            int m = StringUtils.length(col.getName());
            if (m > maxColNameLength) {
                maxColNameLength = m;
            }
        }
        maxColNameLength += 4;
        if (supportColEscapeChar()) {
            this.escapeChar = dsMeta.getEscapeChar();
            if (StringUtils.isEmpty(this.escapeChar)) {
                throw new IllegalArgumentException("must contain escapeChar for DB entity");
            }
        } else {
            this.escapeChar = StringUtils.EMPTY;
        }
    }


    protected void appendExtraColDef(List<ColWrapper> pks) {
    }

    protected void appendTabMeta(List<ColWrapper> pks) {
    }


    public CreateDDL build() {

        script.append("CREATE TABLE ").append((getCreateTableName().getEntityName())).append("\n");
        script.append("(\n");


        final int colSize = getCols().size();
        int colIndex = 0;

        for (ColWrapper col : preProcessCols(pks, getCols())) {
            script.append("    ");

            this.appendColName(col.getName());

            script.append(col.getMapperType());

            col.appendExtraConstraint(script);
            if (++colIndex < colSize) {
                script.append(",");
            }
            script.append("\n");
        }

        // script.append("    `__cc_ck_sign` Int8 DEFAULT 1").append("\n");
        this.appendExtraColDef(pks);
        script.append(")\n");
        this.appendTabMeta(pks);
//            script.append(" ENGINE = CollapsingMergeTree(__cc_ck_sign)").append("\n");
//            // Objects.requireNonNull(pk, "pk can not be null");
//            if (pk != null) {
//                script.append(" ORDER BY `").append(pk.getName()).append("`\n");
//            }
//            script.append(" SETTINGS index_granularity = 8192");
//        CREATE TABLE tis.customer_order_relation
//                (
//                        `customerregister_id` String,
//                        `waitingorder_id` String,
//                        `worker_id` String,
//                        `kind` Int8,
//                        `create_time` Int64,
//                        `last_ver` Int8,
//                        `__cc_ck_sign` Int8 DEFAULT 1
//                )
//        ENGINE = CollapsingMergeTree(__cc_ck_sign)
//        ORDER BY customerregister_id
//        SETTINGS index_granularity = 8192
        return new CreateDDL(script.getContent(), this);
    }

    public static class CreateDDL {
        private final StringBuffer script;
        private final CreateTableSqlBuilder builder;

        public CreateDDL(StringBuffer script, CreateTableSqlBuilder builder) {
            this.script = script;
            this.builder = builder;
        }

        public StringBuffer getDDLScript() {
            return this.script;
        }

        public String getSelectAllScript() {
            return "SELECT " + builder.getCols().stream()
                    .map((c) -> builder.wrapWithEscape(c.getName()))
                    .collect(Collectors.joining(","))
                    + " FROM " + (builder.getCreateTableName().getEntityName());
        }
    }

    public void appendColName(String col) {
        script.append(escapeChar)
                .append(String.format("%-" + (maxColNameLength) + "s", col + (escapeChar)));
    }

    protected String wrapWithEscape(String val) {
        return this.escapeChar + val + this.escapeChar;
    }

    /**
     * 在打印之前先对cols进行预处理，比如排序等
     *
     * @param cols
     * @return
     */
    protected List<ColWrapper> preProcessCols(List<ColWrapper> pks, List<CMeta> cols) {
        return cols.stream().map((c) -> createColWrapper(c)).collect(Collectors.toList());
    }

    protected abstract ColWrapper createColWrapper(CMeta c);//{
//        return new ColWrapper(c);
    // }

    public static abstract class ColWrapper {
        protected final CMeta meta;

        public ColWrapper(CMeta meta) {
            this.meta = meta;
        }

        public abstract String getMapperType();

        protected void appendExtraConstraint(BlockScriptBuffer ddlScript) {

        }

        public String getName() {
            return this.meta.getName();
        }
    }


//    protected char colEscapeChar() {
//        return '`';
//    }

    protected boolean supportColEscapeChar() {
        return true;
    }

    protected CreateTableName getCreateTableName() {
        return new CreateTableName(tableMapper.getTo(), this);
    }

    protected List<CMeta> getCols() {
        return tableMapper.getSourceCols();
    }


    public static class CreateTableName {
        private final String schema;
        private final String tabName;
        final CreateTableSqlBuilder sqlBuilder;

        public CreateTableName(String tabName, CreateTableSqlBuilder sqlBuilder) {
            this(StringUtils.EMPTY, tabName, sqlBuilder);
        }

        public CreateTableName(String schema, String tabName, CreateTableSqlBuilder sqlBuilder) {
            if (StringUtils.isEmpty(tabName)) {
                throw new IllegalArgumentException("param tabName can not be empty");
            }
            this.schema = schema;
            this.tabName = tabName;
            this.sqlBuilder = sqlBuilder;
        }

        public final String getEntityName() {
            StringBuffer entityName = new StringBuffer();
            if (StringUtils.isNotEmpty(schema)) {
                entityName.append(sqlBuilder.wrapWithEscape(schema)).append(".");
            }
            entityName.append(sqlBuilder.wrapWithEscape(tabName));
            return entityName.toString();
        }
    }
}
