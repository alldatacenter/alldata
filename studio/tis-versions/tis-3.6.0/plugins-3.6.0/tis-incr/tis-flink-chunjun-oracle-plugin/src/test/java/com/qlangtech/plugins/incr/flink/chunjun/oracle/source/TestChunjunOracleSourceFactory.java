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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.*;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer;
import com.qlangtech.tis.plugins.incr.flink.chunjun.offset.ScanAll;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.CloseableIterator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.*;

import static com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer.drop_column_type_test;
import static com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer.sqlfile_column_type_test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-24 15:33
 **/
public class TestChunjunOracleSourceFactory {
    static BasicDataSourceFactory oracleDS;
    private static final String tabNameFull_types = "full_types";
    private static final String tabNameFull_types_pk = "id";
    private static final String key_timestamp6_c = "timestamp6_c";
    private static final String key_bytea_c = "bytea_c";
    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();
    private static final int BIG_DECIMAL_SCALA = 5;


    @BeforeClass
    public static void initialize() throws Exception {
        //TestChunjunOracleSinkFactory.initialize();
        OracleDSFactoryContainer.initialize();
        oracleDS = Objects.requireNonNull(OracleDSFactoryContainer.oracleDS, "oracleDS can not be null");
        OracleDSFactoryContainer.initializeOracleTable(sqlfile_column_type_test, "insert_full_types");

//        List<String> tables = oracleDS.getTablesInDB();
//        String full_types = "full_types";
//        Optional<String> find = tables.stream().filter((tab) -> {
//            return full_types.equals(StringUtils.substringAfter(tab, "."));
//        }).findFirst();
//        Assert.assertTrue("table must present:" + full_types, find.isPresent());
    }

    @Test
    public void testRecordPullWithHistoryRecord() throws Exception {
        OracleDSFactoryContainer.initializeOracleTable(drop_column_type_test, sqlfile_column_type_test, "insert_full_types");
        CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder chunParamBuilder
                = (CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder) CDCTestSuitParams.chunjunBuilder()
                .setIncrColumn(key_timestamp6_c)
                .setTabName(tabNameFull_types);
        CDCTestSuitParams params = chunParamBuilder.setStartLocation(new ScanAll()).build();

        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(params) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return oracleDS;
            }

            @Override
            protected String getColEscape() {
                return "\"";
            }

            @Override
            protected List<TestRow> createExampleTestRows() throws Exception {
                List<TestRow> exampleRows = Lists.newArrayList();
                TestRow row = null;
                Map<String, RowValsExample.RowVal> vals = null;
                int insertCount = 1;

                for (int i = 1; i <= insertCount; i++) {
                    vals = Maps.newHashMap();
                    vals.put(tabNameFull_types_pk, RowValsExample.RowVal.$((long) i));
                    vals.put(key_timestamp6_c, parseTimestamp("2020-07-17 18:00:22"));
                    row = new TestRow(RowKind.INSERT, new RowValsExample(vals));
                    row.idVal = i;
                    exampleRows.add(row);
                }
                return exampleRows;
            }

            @Override
            protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
                    , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener) throws Exception {
                //   super.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);
                imqListener.start(dataxName, dataxReader, Collections.singletonList(tab), createProcess());
                CloseableIterator<Row> snapshot = consumerHandle.getRowSnapshot(tabName);
                waitForSnapshotStarted(snapshot);
                while (snapshot.hasNext()) {
                    Row row = snapshot.next();
                    Assert.assertEquals(new Long(100), row.getFieldAs(tabNameFull_types_pk));
                    return;
                }
                Assert.fail("has not get history record");
                //Thread.sleep(1000);
            }


        };

        ChunjunOracleSourceFactory oracleListener = new ChunjunOracleSourceFactory();

        cdcTestSuit.startTest(oracleListener);

    }


    @Test
    public void testRecordPull() throws Exception {

        // 由于initializeOracleTable过程中额外对数据库中添加了一条历史数据，并且starLocation中采用了LatestLocation
        // 一次消费过程中verify过程中只应该得到刚插入系统的
        CDCTestSuitParams params //
                = CDCTestSuitParams.chunjunBuilder()
                .setIncrColumn(key_timestamp6_c)
                .setTabName(tabNameFull_types)
                .build();

        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(params) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return oracleDS;
            }


            @Override
            protected String getColEscape() {
                return "\"";
            }


            @Override
            protected List<TestRow> createExampleTestRows() throws Exception {
                List<TestRow> exampleRows = Lists.newArrayList();
                Date now = new Date();
                TestRow row = null;
                Map<String, RowValsExample.RowVal> vals = null;
                int insertCount = 1;

                for (int i = 1; i <= insertCount; i++) {
                    vals = Maps.newHashMap();
                    vals.put(tabNameFull_types_pk, RowValsExample.RowVal.$((long) i));
                    vals.put(key_bytea_c, RowValsExample.RowVal.stream("bytea_c_val"));
                    vals.put("small_c", RowValsExample.RowVal.$(2l));
                    vals.put("int_c", RowValsExample.RowVal.$(32768l));
                    vals.put("big_c", RowValsExample.RowVal.$(2147483648l));
                    vals.put("real_c", RowValsExample.RowVal.$(5.5f));
                    vals.put("double_precision", RowValsExample.RowVal.$(6.6f));
                    vals.put("numeric_c", RowValsExample.RowVal.decimal(12312345, BIG_DECIMAL_SCALA));
                    vals.put("decimal_c", RowValsExample.RowVal.decimal(4044443, BIG_DECIMAL_SCALA));
                    vals.put("boolean_c", RowValsExample.RowVal.$(1));
                    vals.put("text_c", RowValsExample.RowVal.$("Hello moto"));
                    vals.put("char_c", RowValsExample.RowVal.$("b"));
                    vals.put("character_c", RowValsExample.RowVal.$("abf"));
                    vals.put("character_varying_c", RowValsExample.RowVal.$("abcd..xyzkkkkk"));
                    vals.put("timestamp3_c", parseTimestamp("2022-07-29 18:00:22"));
                    vals.put(key_timestamp6_c, parseTimestamp("2020-07-17 18:00:22"));
                    vals.put("date_c", (parseDate("2020-07-17")));
                    vals.put("time_c", parseTimestamp("1970-01-01 18:00:22"));
                    vals.put("default_numeric_c", RowValsExample.RowVal.decimal(500, 0));

                    row = new TestRow(RowKind.INSERT, new RowValsExample(vals));
                    row.updateVals.put(key_timestamp6_c, (statement, index, ovals) -> {
                        RowValsExample.RowVal newt = parseTimestamp("2020-07-18 18:00:22");
                        statement.setTimestamp(index, newt.getVal());
                        return newt;
                    });
                    row.updateVals.put("small_c", (statement, index, ovals) -> {
                        RowValsExample.RowVal nsmall = RowValsExample.RowVal.$(2l);
                        statement.setLong(index, nsmall.getVal());
                        return nsmall;
                    });
                    row.idVal = i;
                    exampleRows.add(row);
                }
                return exampleRows;
            }

            @Override
            protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
                    , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener) throws Exception {
                super.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);
                // imqListener.start(dataxName, dataxReader, Collections.singletonList(tab), null);
                Thread.sleep(1000);
            }


        };

        ChunjunOracleSourceFactory oracleListener = new ChunjunOracleSourceFactory();

        cdcTestSuit.startTest(oracleListener);

    }


}
