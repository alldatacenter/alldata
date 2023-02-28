/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.plugin.ds;

import com.google.common.base.Joiner;
import com.qlangtech.tis.manage.common.Option;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ColumnMetaData extends Option {

    public static final String KEY_COLS_METADATA = "cols-metadata";

    public static Map<String, ColumnMetaData> toMap(List<ColumnMetaData> cols) {
        return cols.stream().collect(Collectors.toMap((c) -> c.getName(), (c) -> c));
    }

    public static StringBuffer buildExtractSQL(String tableName, List<ColumnMetaData> cols) {
        return buildExtractSQL(tableName, false, cols);
    }

    public static StringBuffer buildExtractSQL(String tableName, boolean useAlias, List<ColumnMetaData> cols) {
        if (CollectionUtils.isEmpty(cols)) {
            throw new IllegalStateException("tableName:" + tableName + "");
        }
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT ");
        sql.append(Joiner.on(",").join(cols.stream().map((r) -> {
            if (useAlias) {
                return "a." + r.getKey();
            } else {
                return r.getKey();
            }
        }).iterator())).append("\n");
        sql.append("FROM ").append(tableName);
        if (useAlias) {
            sql.append(" AS a");
        }
        return sql;
    }

    private final String key;

    private final DataType type;


    private final int index;
    private ReservedFieldType schemaFieldType;

    // private final String dbType;
    // private final String hiveType;
    // 是否是主键
    private final boolean pk;

    private final boolean nullable;

    /**
     * 列的注释
     */
    private transient String comment;


    /**
     * @param index
     * @param key      column名字
     * @param type     column类型 java.sql.Types
     * @param pk
     * @param nullable 是否可空
     */
    public ColumnMetaData(int index, String key, DataType type, boolean pk, boolean nullable) {
        super(key, key);
        this.pk = pk;
        this.key = key;
        this.type = type;
        this.index = index;
        this.nullable = nullable;
    }

    public ColumnMetaData(int index, String key, DataType type, boolean pk) {
        this(index, key, type, pk, !pk);
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ReservedFieldType getSchemaFieldType() {
        return schemaFieldType;
    }

    public void setSchemaFieldType(ReservedFieldType schemaFieldType) {
        this.schemaFieldType = schemaFieldType;
    }

    public boolean isNullable() {
        return this.nullable;
    }

    public int getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    public DataType getType() {
        return type;
    }

    public boolean isPk() {
        return this.pk;
    }


    @Override
    public String toString() {
        return "ColumnMetaData{" +
                "key='" + key + '\'' +
                ", type=" + type +
                ", index=" + index +
                ", schemaFieldType=" + schemaFieldType +
                ", pk=" + pk +
                '}';
    }

}
