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

package org.apache.inlong.sort.formats.json.canal;

import java.util.ArrayList;
import java.util.List;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.sort.protocol.ddl.expressions.Column;
import org.apache.inlong.sort.protocol.ddl.expressions.Position;
import org.apache.inlong.sort.protocol.ddl.enums.AlterType;
import org.apache.inlong.sort.protocol.ddl.enums.PositionType;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.operations.AlterOperation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link CanalJson}.
 */
public class CanalJsonSerializationTest {

    private static final Logger LOG = LoggerFactory.getLogger(CanalJsonSerializationTest.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCanalJsonSerialization() {

        List<AlterColumn> alterColumns = new ArrayList<>();

        Column column = new Column("columnDataType.getColumnName()", new ArrayList<>(),
                1,
                new Position(PositionType.FIRST, null), true, "23",
                "23");

        alterColumns.add(new AlterColumn(AlterType.ADD_COLUMN, column, null));

        AlterOperation alterOperation = new AlterOperation(alterColumns);

        CanalJson canalJson = CanalJson.builder()
                .data(null)
                .es(0)
                .table("table")
                .type("type")
                .database("database")
                .ts(0)
                .sql("sql")
                .mysqlType(null)
                .sqlType(null)
                .pkNames(null)
                .schema("schema")
                .oracleType(null)
                .operation(alterOperation)
                .incremental(false)
                .build();

        ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        try {
            String writeValueAsString = OBJECT_MAPPER.writeValueAsString(canalJson);
            LOG.info(writeValueAsString);
            objectMapper.readValue(writeValueAsString, CanalJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
