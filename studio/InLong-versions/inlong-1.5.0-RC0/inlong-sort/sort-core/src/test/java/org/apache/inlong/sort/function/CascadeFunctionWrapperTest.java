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
import org.apache.inlong.sort.protocol.transformation.CascadeFunction;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.function.CascadeFunctionWrapper;
import org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFunction;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link CascadeFunctionWrapper}
 */
public class CascadeFunctionWrapperTest extends AbstractTestBase {

    /**
     * Test for CascadeFunctionWrapper
     *
     * @throws Exception The exception may throw when test CascadeFunctionWrapper
     */
    @Test
    public void testCascadeFunctionWrapper() throws Exception {
        // step 0. Initialize the execution environment
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        // step 1. Register custom function of REGEXP_REPLACE_FIRST
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE_FIRST",
                RegexpReplaceFirstFunction.class);
        // step 2. Generate test data and convert to DataStream
        List<Row> data = new ArrayList<>();
        data.add(Row.of("aabbccaabbcc"));
        TypeInformation<?>[] types = {
                BasicTypeInfo.STRING_TYPE_INFO};
        String[] names = {"f1"};
        RowTypeInfo typeInfo = new RowTypeInfo(types, names);
        List<CascadeFunction> functions = new ArrayList<>();
        functions.add(new org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFirstFunction(
                new FieldInfo("f1", new StringFormatInfo()),
                new StringConstantParam("aa"), new StringConstantParam("apache")));
        functions.add(new RegexpReplaceFunction(
                new FieldInfo("f1", new StringFormatInfo()),
                new StringConstantParam("bb"), new StringConstantParam("inlong")));
        functions.add(new RegexpReplaceFunction(
                new FieldInfo("f1", new StringFormatInfo()),
                new StringConstantParam("cc"), new StringConstantParam("etl")));
        CascadeFunctionWrapper wrapper = new CascadeFunctionWrapper(functions);
        DataStream<Row> ds = env.fromCollection(data).returns(typeInfo);
        // step 3. Convert from DataStream to Table and execute the REGEXP_REPLACE_FIRST function
        Table tempView = tableEnv.fromDataStream(ds).as("f1");
        tableEnv.createTemporaryView("temp_view", tempView);
        String sqlQuery = String.format("SELECT %s as a FROM temp_view", wrapper.format());
        Table outputTable = tableEnv.sqlQuery(sqlQuery);
        DataStream<Row> resultSet = tableEnv.toAppendStream(outputTable, Row.class);
        // step 4. Get function execution result and parse it
        List<String> r = new ArrayList<>();
        for (CloseableIterator<String> it = resultSet.map(s -> s.getField(0).toString()).executeAndCollect(); it
                .hasNext();) {
            String next = it.next();
            r.add(next);
        }
        // step 5. Whether the comparison results are as expected
        String expect = "apacheinlongetlaainlongetl";
        Assert.assertEquals(expect, r.get(0));
    }
}
