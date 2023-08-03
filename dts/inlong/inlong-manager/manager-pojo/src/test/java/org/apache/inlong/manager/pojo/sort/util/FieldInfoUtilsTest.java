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

package org.apache.inlong.manager.pojo.sort.util;

import org.apache.inlong.manager.common.fieldtype.strategy.ClickHouseFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.MongoDBFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.MySQLFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.OracleFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.PostgreSQLFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.SQLServerFieldTypeStrategy;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.sort.formats.common.ByteTypeInfo;
import org.apache.inlong.sort.formats.common.IntTypeInfo;
import org.apache.inlong.sort.formats.common.LocalZonedTimestampTypeInfo;
import org.apache.inlong.sort.formats.common.ShortTypeInfo;
import org.apache.inlong.sort.formats.common.StringTypeInfo;
import org.apache.inlong.sort.formats.common.TypeInfo;
import org.apache.inlong.sort.protocol.FieldInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Different data source field type conversion mapping test class.
 */
public class FieldInfoUtilsTest {

    @Test
    public void testPostgreSQLFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("age");
        streamField.setFieldType("int4");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new PostgreSQLFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof IntTypeInfo);
    }

    @Test
    public void testMySQLFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("age");
        streamField.setFieldType("tinyint unsigned");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new MySQLFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof ShortTypeInfo);
    }

    @Test
    public void testOracleFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("date");
        streamField.setFieldType("TIMESTAMP WITH LOCAL TIME ZONE");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new OracleFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof LocalZonedTimestampTypeInfo);
    }

    @Test
    public void testSQLServerFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("contentXML");
        streamField.setFieldType("XML");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new SQLServerFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof StringTypeInfo);
    }

    @Test
    public void testMongoDBFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("UUID");
        streamField.setFieldType("uuid");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new MongoDBFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof StringTypeInfo);
    }

    @Test
    public void testClickHouseFieldTypeInfo() {
        StreamField streamField = new StreamField();
        streamField.setIsMetaField(0);
        streamField.setFieldName("age");
        streamField.setFieldType("Nullable(uint8(12))");
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamFieldInfo(streamField,
                "nodeId", new ClickHouseFieldTypeStrategy());
        TypeInfo typeInfo = fieldInfo.getFormatInfo().getTypeInfo();
        Assertions.assertTrue(typeInfo instanceof ByteTypeInfo);
    }
}
