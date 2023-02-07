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

package org.apache.inlong.sort.function;

import java.util.ArrayList;
import java.util.List;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link JsonGetterFunction}
 */
public class JsonGetterFunctionTest extends AbstractTestBase {

    /**
     * Test for JsonGetter function
     *
     * @throws Exception The exception may throw when test Encrypt function
     */
    @Test
    public void testJsonGetterFunction() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        // step 1. Register custom function of json getter
        tableEnv.createTemporaryFunction("JSON_GETTER", JsonGetterFunction.class);
        // step 2. Generate test data and convert to DataStream
        List<Row> data = new ArrayList<>();
        data.add(Row.of("{\"name\":\"abc\"}"));
        TypeInformation<?>[] types = {
                BasicTypeInfo.STRING_TYPE_INFO};
        String[] names = {"content"};
        RowTypeInfo typeInfo = new RowTypeInfo(types, names);
        DataStream<Row> dataStream = env.fromCollection(data).returns(typeInfo);

        // step 3. Convert from DataStream to Table and execute the json getter function
        Table tempView = tableEnv.fromDataStream(dataStream).as("content");
        tableEnv.createTemporaryView("temp_view", tempView);

        org.apache.inlong.sort.protocol.transformation.function.JsonGetterFunction jsonGetterFunction =
                new org.apache.inlong.sort.protocol.transformation.function.JsonGetterFunction(
                        new FieldInfo("content",
                                new StringFormatInfo()),
                        new StringConstantParam("name"));

        String sqlQuery = String.format("SELECT %s as content FROM temp_view", jsonGetterFunction.format());
        Table outputTable = tableEnv.sqlQuery(sqlQuery);
        // step 4. Get function execution result and parse it
        DataStream<Row> resultSet = tableEnv.toAppendStream(outputTable, Row.class);
        List<String> result = new ArrayList<>();

        for (CloseableIterator<String> it = resultSet.map(s -> s.getField(0).toString()).executeAndCollect(); it
                .hasNext();) {
            String next = it.next();
            result.add(next);
        }

        // step 5. Whether the comparison results are as expected
        String expect = "abc";
        Assert.assertEquals(expect, result.get(0));
    }

}
