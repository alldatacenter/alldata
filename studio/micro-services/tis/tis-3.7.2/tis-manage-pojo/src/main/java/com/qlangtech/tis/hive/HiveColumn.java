package com.qlangtech.tis.hive;

import com.qlangtech.tis.plugin.ds.DataType;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;

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

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年1月14日 下午1:51:04
 */
public class HiveColumn {

    // public static String HIVE_TYPE_STRING = "STRING";

    // 插入后的name
    private String name;
    private boolean nullable;

    // 原来的name rawName as name
    private String rawName;

    // private String type;
    public DataType dataType;

    private int index;

    private String defalutValue;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }



    public void setName(String name) {
        this.name = name;
        if (getRawName() == null) {
            setRawName(name);
        }
    }

    /**
     * Reference:https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTableCreate/Drop/TruncateTable
     */
    public static DataType.TypeVisitor<String> hiveTypeVisitor = new DataType.TypeVisitor<String>() {
        @Override
        public String bigInt(DataType type) {
            return "BIGINT";
        }

        @Override
        public String decimalType(DataType type) {
            return "DECIMAL(" + type.columnSize + "," + type.getDecimalDigits() + ")";
        }

        @Override
        public String intType(DataType type) {
            return "INT";
        }

        @Override
        public String tinyIntType(DataType dataType) {
            return "TINYINT";
        }

        @Override
        public String smallIntType(DataType dataType) {
            return "SMALLINT";
        }

        @Override
        public String boolType(DataType dataType) {
            return "BOOLEAN";
        }

        @Override
        public String floatType(DataType type) {
            return "FLOAT";
        }

        @Override
        public String doubleType(DataType type) {
            return "DOUBLE";
        }

        @Override
        public String dateType(DataType type) {
            return "DATE";
        }

        @Override
        public String timestampType(DataType type) {
            return "TIMESTAMP";
        }

        @Override
        public String bitType(DataType type) {
            return "STRING";
        }

        @Override
        public String blobType(DataType type) {
            return "BINARY";
        }

        @Override
        public String varcharType(DataType type) {
            return "STRING";
        }
    };

    public String getDataType() {
        return Objects.requireNonNull(this.dataType, "dataType can not be null").accept(hiveTypeVisitor);
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
        if (getName() == null) {
            setName(rawName);
        }
    }

    public String getDefalutValue() {
        return defalutValue;
    }

    public void setDefalutValue(String defalutValue) {
        this.defalutValue = defalutValue;
    }

    public boolean hasAliasName() {
        return !StringUtils.equals(rawName, name);
    }

    public boolean hasDefaultValue() {
        return !StringUtils.isBlank(defalutValue);
    }

    @Override
    public String toString() {
        return getRawName() + " " + getName();
    }
}
