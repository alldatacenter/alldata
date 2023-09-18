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
public abstract class CreateTableSqlBuilder extends AbstractCreateTableSqlBuilder {

    public BlockScriptBuffer script;

    public CreateTableSqlBuilder(IDataxProcessor.TableMap tableMapper, DataSourceMeta dsMeta) {
        super(tableMapper, dsMeta);
        this.script = new BlockScriptBuffer();
    }


    protected void appendExtraColDef(List<ColWrapper> pks) {
    }

    protected void appendTabMeta(List<ColWrapper> pks) {
    }

    @Override
    public CreateDDL build() {
        CreateTableName tabNameBuilder = getCreateTableName();
        script.append(tabNameBuilder.createTablePredicate())
                .append(" ").append((tabNameBuilder.getEntityName())).append("\n");
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

        this.appendExtraColDef(pks);
        script.append(")\n");
        this.appendTabMeta(pks);

        return new CreateDDL(script.getContent(), this);
    }


    public void appendColName(String col) {
        script.append(escapeChar)
                .append(String.format("%-" + (maxColNameLength) + "s", col + (escapeChar)));
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


    public static class CreateTableName {
        private final String schema;
        private final String tabName;
        final AbstractCreateTableSqlBuilder sqlBuilder;
        private String createTablePredicate = "CREATE TABLE";

        public CreateTableName(String tabName, AbstractCreateTableSqlBuilder sqlBuilder) {
            this(StringUtils.EMPTY, tabName, sqlBuilder);
        }

        public CreateTableName(String schema, String tabName, AbstractCreateTableSqlBuilder sqlBuilder) {
            if (StringUtils.isEmpty(tabName)) {
                throw new IllegalArgumentException("param tabName can not be empty");
            }
            this.schema = schema;
            this.tabName = tabName;
            this.sqlBuilder = sqlBuilder;
        }

        public String createTablePredicate() {
            return this.createTablePredicate;
        }

        public void setCreateTablePredicate(String createTablePredicate) {
            this.createTablePredicate = createTablePredicate;
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
