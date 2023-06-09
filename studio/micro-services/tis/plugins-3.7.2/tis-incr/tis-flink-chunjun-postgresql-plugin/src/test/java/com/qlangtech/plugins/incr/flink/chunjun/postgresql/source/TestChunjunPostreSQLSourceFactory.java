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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.source;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.*;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.test.TISEasyMock;
import com.ververica.cdc.connectors.postgres.PostgresTestBase;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.types.RowKind;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-27 14:31
 **/
public class TestChunjunPostreSQLSourceFactory extends PostgresTestBase implements TISEasyMock {

    private static final String tabNameFull_types = "full_types";
    private static final String tabNameFull_types_pk = "id";
    private static final String key_bytea_c = "bytea_c";
    private static final String key_timestamp6_c = "timestamp6_c";
    private static final int BIG_DECIMAL_SCALA = 5;
    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    @Before
    public void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        initializePostgresTable("column_type_test");
    }

//    @Test
//    public void testTime() throws Exception {
//        RowValsExample.RowVal time = RowValsExample.RowVal.time(("9:00:22"));
//        time.getExpect();
//        Time t = (Time) time.call();
//
//        System.out.println(time.getAssertActual(SqlDateTimeUtils.unixTimeToLocalTime((int) t.getTime())));
//    }


    @Test
    public void testPullCDC() throws Exception {
        System.out.println("====");

        CDCTestSuitParams suitParams = //
                (new CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder())
                        .setIncrColumn(key_timestamp6_c)
                        .setTabName(tabNameFull_types).build();

        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParams) {

//            @Override
//            protected TestRow.ValProcessor getExpectValProcessor() {
//                return (rowVals, key, val) -> {
//                    if ("time_c".equals(key)) {
//                        return "time_c";
//                    }
//                    if ("timestamp3_c".equals(key) || "timestamp6_c".equals(key)) {
//                        return ((Timestamp) val).toLocalDateTime();
//                    }
//                    if (key_bytea_c.equals(key)) {
//                        return new String((byte[]) val);
//                    } else {
//                        return val;
//                    }
//                };
//            }
//            @Override
//            protected TestRow.ValProcessor getActualValProcessor(String tabName, IResultRows consumerHandle) {
//                return (rowVals, key, val) -> {
//
//                    if ("time_c".equals(key)) {
//                        return "time_c";
//                    }
//                    if ("timestamp3_c".equals(key) || "timestamp6_c".equals(key)) {
//                        return ((LocalDateTime) val);
//                    }
//
//                    if (val instanceof BigDecimal) {
//                        return ((BigDecimal) val).setScale(BIG_DECIMAL_SCALA);
//                    }
//                    try {
//                        if (key_bytea_c.equals(key)) {
//                            byte[] buffer = (byte[]) val;
//                            // buffer.reset();
//                            return new String(buffer);
//                        } else {
//                            return consumerHandle.deColFormat(tabName, key, val);
//                        }
//                    } catch (Exception e) {
//                        throw new RuntimeException("colKey:" + key + ",val:" + val, e);
//                    }
//                };
//            }

            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName,boolean useSplitTabStrategy) {
                return (BasicDataSourceFactory) TestChunjunPostreSQLSourceFactory.this.createPgSourceFactory(dataxName);
            }

//            @Override
//            protected SelectedTab createSelectedTab(String tabName, BasicDataSourceFactory dataSourceFactory) {
//                SelectedTab selectedTab = super.createSelectedTab(tabName, dataSourceFactory);
//                SelectedTabPropsExtends incrTabExtend = new SelectedTabPropsExtends();
//                RunInterval polling = new RunInterval();
//                polling.useMaxFunc = true;
//                polling.incrColumn = getPrimaryKeyName(selectedTab);
//                polling.pollingInterval = 4999;
//                incrTabExtend.polling = polling;
//                selectedTab.setIncrSourceProps(incrTabExtend);
//                return selectedTab;
//            }

//            @Override
//            protected String getPrimaryKeyName() {
//                return tabNameFull_types_pk;
//            }

            protected List<TestRow> createExampleTestRows() throws Exception {
                List<TestRow> exampleRows = Lists.newArrayList();
                Date now = new Date();
                TestRow row = null;
                Map<String, RowValsExample.RowVal> vals = null;
                int insertCount = 1;

//                CREATE TABLE full_types (
//                        id INTEGER NOT NULL,
//                        bytea_c BYTEA,
//                        small_c SMALLINT,
//                        int_c INTEGER,
//                        big_c BIGINT,
//                        real_c REAL,
//                        double_precision DOUBLE PRECISION,
//                        numeric_c NUMERIC(10, 5),
//                        decimal_c DECIMAL(10, 1),
//                        boolean_c BOOLEAN,
//                        text_c TEXT,
//                        char_c CHAR,
//                        character_c CHARACTER(3),
//                        character_varying_c CHARACTER VARYING(20),
//                        timestamp3_c TIMESTAMP(3),
//                        timestamp6_c TIMESTAMP(6),
//                        date_c DATE,
//                        time_c TIME(0),
//                        default_numeric_c NUMERIC,
//                        PRIMARY KEY (id)
//                );
//
//                ALTER TABLE full_types REPLICA IDENTITY FULL;
//
//                INSERT INTO full_types VALUES (
//                        1, '2', 32767, 65535, 2147483647, 5.5, 6.6, 123.12345, 404.4443, true,
//                        'Hello World', 'a', 'abc', 'abcd..xyz',  '2020-07-17 18:00:22.123', '2020-07-17 18:00:22.123456',
//                        '2020-07-17', '18:00:22', 500);

                for (int i = 1; i <= insertCount; i++) {
                    vals = Maps.newHashMap();
                    vals.put(tabNameFull_types_pk, RowValsExample.RowVal.$(i));
                    vals.put(key_bytea_c, RowValsExample.RowVal.stream("bytea_c_val"));
                    vals.put("small_c", RowValsExample.RowVal.$((short) 2));
                    vals.put("int_c", RowValsExample.RowVal.$(32768));
                    vals.put("big_c", RowValsExample.RowVal.$(2147483648l));
                    vals.put("real_c", RowValsExample.RowVal.$(5.5f));
                    vals.put("double_precision", RowValsExample.RowVal.$(6.6d));
                    vals.put("numeric_c", RowValsExample.RowVal.decimal(12312345, BIG_DECIMAL_SCALA));
                    vals.put("decimal_c", RowValsExample.RowVal.decimal(4044443, BIG_DECIMAL_SCALA));
                    vals.put("boolean_c", RowValsExample.RowVal.$(true));
                    vals.put("text_c", RowValsExample.RowVal.$("Hello moto"));
                    vals.put("char_c", RowValsExample.RowVal.$("b"));
                    vals.put("character_c", RowValsExample.RowVal.$("abf"));
                    vals.put("character_varying_c", RowValsExample.RowVal.$("abcd..xyzkkkkk"));
                    vals.put("timestamp3_c", parseTimestamp("2022-07-29 18:00:22"));
                    vals.put(key_timestamp6_c, parseTimestamp("2020-07-17 18:00:22"));
                    vals.put("date_c", parseDate("2020-07-17"));
                    vals.put("time_c", RowValsExample.RowVal.time(("9:00:22"), true));
                    vals.put("default_numeric_c", RowValsExample.RowVal.decimal(500, 0) // BigDecimal.valueOf(500).setScale(BIG_DECIMAL_SCALA)
                    );

                    row = new TestRow(RowKind.INSERT, new RowValsExample(vals));
                    row.idVal = i;
                    exampleRows.add(row);
                }

                // 执行三条更新
//                row = exampleRows.get(3);
//                row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
//                    String newVal = "update#" + ovals.getString(keyCol_text);
//                    statement.setString(index, newVal);
//                    return newVal;
//                });
//                row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
//                    String v = "2012-11-13 11:11:35";
//                    statement.setTimestamp(index, parseTimestamp(v));
//                    return v;
//                });
//
//                row = exampleRows.get(4);
//                row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
//                    String v = "update#" + ovals.getString(keyCol_text);
//                    statement.setString(index, v);
//                    return v;
//                });
//                row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
//                    String v = "2012-11-13 11:11:35";
//                    statement.setTimestamp(index, parseTimestamp(v));
//                    return v;
//                });
//
//                row = exampleRows.get(0);
//                row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
//                    String v = "update#" + ovals.getString(keyCol_text);
//                    statement.setString(index, v);
//                    return v;
//                });
//                row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
//                    String v = "2012-11-12 11:11:35";
//                    statement.setTimestamp(index, parseTimestamp(v));
//                    return v;
//                });
//
//                // 执行两条删除
//                row = exampleRows.get(1);
//                row.willbeDelete = true;
//
//                row = exampleRows.get(3);
//                row.willbeDelete = true;
                return exampleRows;
            }

            @Override
            protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
                    , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener) throws Exception {
                super.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);
                // imqListener.start(dataxName, dataxReader, Collections.singletonList(tab), null);
                Thread.sleep(1000);


//                BasicDataSourceFactory dataSourceFactory = (BasicDataSourceFactory) dataxReader.getDataSourceFactory();
//                Assert.assertNotNull("dataSourceFactory can not be null", dataSourceFactory);
//                dataSourceFactory.visitFirstConnection((conn) -> {
//
//                    Statement statement = conn.createStatement();
//                    statement.execute("INSERT INTO `stu` (`id`,`name`,`school`,`nickname`,`age`,`class_num`,`score`,`phone`,`email`,`ip`,`address`)\n" +
//                            "VALUES (1100001,'doTun','beida','jasper',81,26,45.54,14597415152,'xxx@hotmail.com','192.192.192.192','极乐世界f座 630103');");
//                    statement.close();
//                });

//                sleepForAWhile();
//                CloseableIterator<Row> snapshot = consumerHandle.getRowSnapshot(tabName);
//                waitForSnapshotStarted(snapshot);
//                List<TestRow> rows = fetchRows(snapshot, 1, false);
//                for (TestRow rr : rows) {
//                    System.out.println("------------" + rr.getInt("id"));
//                    // assertTestRow(tabName, RowKind.UPDATE_AFTER, consumerHandle, exceptRow, rr);
//
//                }
            }

//            @Override
//            protected String getColEscape() {
//                return StringUtils.EMPTY;
//            }
        };


        ChunjunPostgreSQLSourceFactory pgListener = new ChunjunPostgreSQLSourceFactory();

        cdcTestSuit.startTest(pgListener);


    }


}
