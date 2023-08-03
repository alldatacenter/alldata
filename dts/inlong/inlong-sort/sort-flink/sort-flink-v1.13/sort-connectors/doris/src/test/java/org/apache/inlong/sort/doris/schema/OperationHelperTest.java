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

package org.apache.inlong.sort.doris.schema;

import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.protocol.ddl.enums.AlterType;
import org.apache.inlong.sort.protocol.ddl.enums.PositionType;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.expressions.Column;
import org.apache.inlong.sort.protocol.ddl.expressions.Position;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;

import org.apache.flink.shaded.guava18.com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Test for {@link OperationHelper}
 */
public class OperationHelperTest {

    private final Map<Integer, Column> allTypes2Columns =
            ImmutableMap.<Integer, Column>builder()
                    .put(Types.CHAR, new Column("c", Collections.singletonList("32"), Types.CHAR,
                            new Position(PositionType.FIRST, null), true, "InLong", "a column"))
                    .put(Types.VARCHAR, new Column("c", Collections.singletonList("32"), Types.VARCHAR,
                            new Position(PositionType.FIRST, null), false, "InLong", "a column"))
                    .put(Types.SMALLINT, new Column("c", Collections.singletonList("8"), Types.SMALLINT,
                            new Position(PositionType.AFTER, "b"), true, "2023", "a column"))
                    .put(Types.INTEGER, new Column("c", Collections.singletonList("11"), Types.INTEGER,
                            new Position(PositionType.AFTER, "b"), true, "2023", "a column"))
                    .put(Types.BIGINT, new Column("c", Collections.singletonList("16"), Types.BIGINT,
                            new Position(PositionType.AFTER, "b"), true, "2023", "a column"))
                    .put(Types.REAL,
                            new Column("c", Arrays.asList("11", "2"), Types.REAL, new Position(PositionType.AFTER, "b"),
                                    true, "99.99", "a column"))
                    .put(Types.DOUBLE, new Column("c", Arrays.asList("11", "2"), Types.DOUBLE,
                            new Position(PositionType.AFTER, "b"), true, "99.99", "a column"))
                    .put(Types.FLOAT, new Column("c", Arrays.asList("11", "2"), Types.FLOAT,
                            new Position(PositionType.AFTER, "b"), true, "99.99", "a column"))
                    .put(Types.DECIMAL, new Column("c", Arrays.asList("11", "2"), Types.DECIMAL,
                            new Position(PositionType.AFTER, "b"), true, "99.99", "a column"))
                    .put(Types.NUMERIC, new Column("c", Arrays.asList("11", "2"), Types.NUMERIC,
                            new Position(PositionType.AFTER, "b"), true, "99.99", "a column"))
                    .put(Types.BIT,
                            new Column("c", null, Types.BIT, new Position(PositionType.AFTER, "b"), true, "false",
                                    "a column"))
                    .put(Types.TIME,
                            new Column("c", null, Types.TIME, new Position(PositionType.AFTER, "b"), true, "10:30",
                                    "a column"))
                    .put(Types.TIME_WITH_TIMEZONE,
                            new Column("c", null, Types.TIME_WITH_TIMEZONE, new Position(PositionType.AFTER, "b"),
                                    true, "10:30", "a column"))
                    .put(Types.TIMESTAMP_WITH_TIMEZONE,
                            new Column("c", null, Types.TIMESTAMP_WITH_TIMEZONE, new Position(PositionType.AFTER, "b"),
                                    true, "2023-01-01 10:30", "a column"))
                    .put(Types.TIMESTAMP, new Column("c", null, Types.TIMESTAMP, new Position(PositionType.AFTER, "b"),
                            true, "2023-01-01 10:30", "a column"))
                    .put(Types.BINARY, new Column("c", null, Types.BINARY, new Position(PositionType.AFTER, "b"),
                            true, "this is a BINARY", "a column"))
                    .put(Types.VARBINARY, new Column("c", null, Types.BINARY, new Position(PositionType.AFTER, "b"),
                            true, "this is a VARBINARY", "a column"))
                    .put(Types.BLOB,
                            new Column("c", null, Types.BLOB, new Position(PositionType.AFTER, "b"), true,
                                    "this is a BLOB",
                                    "a column"))
                    .put(Types.CLOB,
                            new Column("c", null, Types.CLOB, new Position(PositionType.AFTER, "b"), true,
                                    "this is a CLOB",
                                    "a column"))
                    .put(Types.DATE,
                            new Column("c", null, Types.DATE, new Position(PositionType.AFTER, "b"), true, "2023-01-01",
                                    "a column"))
                    .put(Types.BOOLEAN,
                            new Column("c", null, Types.BOOLEAN, new Position(PositionType.AFTER, "b"), true, "true",
                                    "a column"))
                    .put(Types.LONGNVARCHAR,
                            new Column("c", null, Types.LONGNVARCHAR, new Position(PositionType.AFTER, "b"),
                                    true, "this is a LONGNVARCHAR", "a column"))
                    .put(Types.LONGVARBINARY,
                            new Column("c", null, Types.LONGVARBINARY, new Position(PositionType.AFTER, "b"),
                                    true, "this is a LONGVARBINARY", "a column"))
                    .put(Types.LONGVARCHAR,
                            new Column("c", null, Types.LONGVARCHAR, new Position(PositionType.AFTER, "b"),
                                    true, "this is a LONGVARCHAR", "a column"))
                    .put(Types.ARRAY,
                            new Column("c", null, Types.ARRAY, new Position(PositionType.AFTER, "b"), true,
                                    "this is a ARRAY",
                                    "a column"))
                    .put(Types.NCHAR,
                            new Column("c", null, Types.NCHAR, new Position(PositionType.AFTER, "b"), true,
                                    "this is a NCHAR",
                                    "a column"))
                    .put(Types.NCLOB,
                            new Column("c", null, Types.NCLOB, new Position(PositionType.AFTER, "b"), true,
                                    "this is a NCLOB",
                                    "a column"))
                    .put(Types.TINYINT, new Column("c", Collections.singletonList("1"), Types.TINYINT,
                            new Position(PositionType.FIRST, null), true, "1", "a column"))
                    .put(Types.OTHER,
                            new Column("c", null, Types.OTHER, new Position(PositionType.AFTER, "b"), true,
                                    "this is a OTHER",
                                    "a column"))
                    .build();
    private final Map<Integer, String> addColumnStatements =
            ImmutableMap.<Integer, String>builder()
                    .put(Types.CHAR,
                            "ADD COLUMN `c` CHAR(32) DEFAULT 'InLong' COMMENT 'a column' FIRST")
                    .put(Types.VARCHAR,
                            "ADD COLUMN `c` VARCHAR(96) NOT NULL DEFAULT 'InLong' COMMENT 'a column' FIRST")
                    .put(Types.SMALLINT,
                            "ADD COLUMN `c` SMALLINT(8) DEFAULT '2023' COMMENT 'a column' AFTER `b`")
                    .put(Types.INTEGER,
                            "ADD COLUMN `c` INT(11) DEFAULT '2023' COMMENT 'a column' AFTER `b`")
                    .put(Types.BIGINT,
                            "ADD COLUMN `c` BIGINT(16) DEFAULT '2023' COMMENT 'a column' AFTER `b`")
                    .put(Types.REAL,
                            "ADD COLUMN `c` DECIMAL(11, 2) DEFAULT '99.99' COMMENT 'a column' AFTER `b`")
                    .put(Types.DOUBLE,
                            "ADD COLUMN `c` DOUBLE DEFAULT '99.99' COMMENT 'a column' AFTER `b`")
                    .put(Types.FLOAT,
                            "ADD COLUMN `c` FLOAT DEFAULT '99.99' COMMENT 'a column' AFTER `b`")
                    .put(Types.DECIMAL,
                            "ADD COLUMN `c` DECIMAL(11, 2) DEFAULT '99.99' COMMENT 'a column' AFTER `b`")
                    .put(Types.NUMERIC,
                            "ADD COLUMN `c` DECIMAL(11, 2) DEFAULT '99.99' COMMENT 'a column' AFTER `b`")
                    .put(Types.BIT,
                            "ADD COLUMN `c` BOOLEAN  DEFAULT 'false' COMMENT 'a column' AFTER `b`")
                    .put(Types.TIME,
                            "ADD COLUMN `c` STRING DEFAULT '10:30' COMMENT 'a column' AFTER `b`")
                    .put(Types.TIME_WITH_TIMEZONE,
                            "ADD COLUMN `c` STRING DEFAULT '10:30' COMMENT 'a column' AFTER `b`")
                    .put(Types.TIMESTAMP_WITH_TIMEZONE,
                            "ADD COLUMN `c` DATETIME DEFAULT '2023-01-01 10:30' COMMENT 'a column' AFTER `b`")
                    .put(Types.TIMESTAMP,
                            "ADD COLUMN `c` DATETIME DEFAULT '2023-01-01 10:30' COMMENT 'a column' AFTER `b`")
                    .put(Types.BINARY,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a BINARY' COMMENT 'a column' AFTER `b`")
                    .put(Types.VARBINARY,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a VARBINARY' COMMENT 'a column' AFTER `b`")
                    .put(Types.BLOB,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a BLOB' COMMENT 'a column' AFTER `b`")
                    .put(Types.CLOB,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a CLOB' COMMENT 'a column' AFTER `b`")
                    .put(Types.DATE,
                            "ADD COLUMN `c` DATE DEFAULT '2023-01-01' COMMENT 'a column' AFTER `b`")
                    .put(Types.BOOLEAN,
                            "ADD COLUMN `c` BOOLEAN DEFAULT 'true' COMMENT 'a column' AFTER `b`")
                    .put(Types.LONGNVARCHAR,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a LONGNVARCHAR' COMMENT 'a column' AFTER `b`")
                    .put(Types.LONGVARBINARY,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a LONGVARBINARY' COMMENT 'a column' AFTER `b`")
                    .put(Types.LONGVARCHAR,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a LONGVARCHAR' COMMENT 'a column' AFTER `b`")
                    .put(Types.ARRAY,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a ARRAY' COMMENT 'a column' AFTER `b`")
                    .put(Types.NCHAR,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a NCHAR' COMMENT 'a column' AFTER `b`")
                    .put(Types.NCLOB,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a NCLOB' COMMENT 'a column' AFTER `b`")
                    .put(Types.TINYINT,
                            "ADD COLUMN `c` TINYINT(1) DEFAULT '1' COMMENT 'a column' FIRST")
                    .put(Types.OTHER,
                            "ADD COLUMN `c` STRING DEFAULT 'this is a OTHER' COMMENT 'a column' AFTER `b`")
                    .build();
    private OperationHelper helper;

    @Before
    public void init() {
        helper = OperationHelper.of(
                (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat("canal-json"));
    }

    /**
     * Test for {@link OperationHelper#buildAddColumnStatement(List)}
     */
    @Test
    public void testBuildAddColumnStatement() {
        for (Entry<Integer, Column> kv : allTypes2Columns.entrySet()) {
            Assert.assertEquals(addColumnStatements.get(kv.getKey()),
                    helper.buildAddColumnStatement(Collections.singletonList(new AlterColumn(
                            AlterType.ADD_COLUMN, kv.getValue(), null))));
        }
    }

    /**
     * Test for {@link OperationHelper#buildDropColumnStatement(List)}
     */
    @Test
    public void testBuildDropColumnStatement() {
        for (Entry<Integer, Column> kv : allTypes2Columns.entrySet()) {
            Assert.assertEquals("DROP COLUMN `c`",
                    helper.buildDropColumnStatement(Collections.singletonList(
                            new AlterColumn(AlterType.DROP_COLUMN, null, kv.getValue()))));
        }

    }

    /**
     * Test for {@link OperationHelper#buildCreateTableStatement(String, String, List, CreateTableOperation)}
     */
    @Test
    public void testBuildCreateTableStatement() {
        List<String> primaryKeys = Arrays.asList("a", "b");
        List<Column> columns = Arrays.asList(new Column("a", Collections.singletonList("32"), Types.VARCHAR,
                new Position(PositionType.FIRST, null), false, "InLong", "a column"),
                new Column("b", Collections.singletonList("32"), Types.VARCHAR,
                        new Position(PositionType.FIRST, null), false, "InLong", "a column"),
                new Column("c", Collections.singletonList("32"), Types.VARCHAR,
                        new Position(PositionType.FIRST, null), true, "InLong", "a column"),
                new Column("d", Collections.singletonList("32"), Types.VARCHAR,
                        new Position(PositionType.FIRST, null), true, "InLong", "a column"));
        CreateTableOperation operation = new CreateTableOperation();
        operation.setComment("create table auto");
        operation.setColumns(columns);
        String database = "inlong_database";
        String table = "inlong_table";
        Assert.assertEquals("CREATE TABLE IF NOT EXISTS `inlong_database`.`inlong_table`(\n"
                + "\t`a` VARCHAR(96) NOT NULL DEFAULT 'InLong' COMMENT 'a column',\n"
                + "\t`b` VARCHAR(96) NOT NULL DEFAULT 'InLong' COMMENT 'a column',\n"
                + "\t`c` VARCHAR(96) DEFAULT 'InLong' COMMENT 'a column',\n"
                + "\t`d` VARCHAR(96) DEFAULT 'InLong' COMMENT 'a column'\n"
                + ")\n"
                + "UNIQUE KEY(`a`,`b`)\n"
                + "COMMENT 'create table auto'\n"
                + "DISTRIBUTED BY HASH(`a`,`b`)\n"
                + "PROPERTIES (\n"
                + "\t\"light_schema_change\" = \"true\"\n"
                + ")",
                helper.buildCreateTableStatement(database, table, primaryKeys, operation));
    }
}
