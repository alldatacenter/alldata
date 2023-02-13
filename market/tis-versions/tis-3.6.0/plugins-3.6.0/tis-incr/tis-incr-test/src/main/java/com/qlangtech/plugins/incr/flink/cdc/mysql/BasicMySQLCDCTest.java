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

package com.qlangtech.plugins.incr.flink.cdc.mysql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.*;
import com.qlangtech.plugins.incr.flink.cdc.source.TestTableRegisterFlinkSourceHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.MQConsumeException;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.test.TISEasyMock;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.CloseableIterator;
import org.junit.Assert;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-28 14:25
 **/
public abstract class BasicMySQLCDCTest extends MySqlSourceTestBase implements TISEasyMock {
    //private static final Logger LOG = LoggerFactory.getLogger(TestFlinkCDCMySQLSourceFactory.class);


    @Before
    public void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
    }

//    ImmutableMap.Builder<String, CDCTestSuitParams> builder = ImmutableMap.builder();
//        builder.put(tabStu, suitParamBuilder(tabStu) //.setIncrColumn("id")
//                .setTabName(tabStu).build());
//
//        builder.put(tabBase, suitParamBuilder(tabStu)
//    //.setIncrColumn("update_time")
//                .setTabName(tabBase) //
//                .build());
//
//        builder.put(tabInstanceDetail, suitParamBuilder(tabStu)
//    //.setIncrColumn("modify_time")
//                .setTabName(tabInstanceDetail).build());

    static Map<String, String> tab2IncrCol = Maps.newHashMap();

    static {
        tab2IncrCol.put(tabStu, "id");
        tab2IncrCol.put(tabBase, "update_time");
        tab2IncrCol.put(tabInstanceDetail, "modify_time");
        tab2IncrCol.put(fullTypes, "timestamp_c");
    }


    @Override
    protected final CDCTestSuitParams.Builder suitParamBuilder(String tabName) {
        CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder builder = CDCTestSuitParams.chunjunBuilder();//.Builder.ChunjunSuitParamsBuilder();
        builder.setIncrColumn(
                Objects.requireNonNull(tab2IncrCol.get(tabName), "tab:" + tabName + " relevant incr cols can not be null"));
        return builder;
    }

//    protected abstract TestRow.ValProcessor rewriteExpectValProcessor(TestRow.ValProcessor valProcess);
//
//    protected abstract TestRow.ValProcessor rewriteActualValProcessor(String tabName, TestRow.ValProcessor valProcess);


    // @Test
    public void testStuBinlogConsume() throws Exception {


        MQListenerFactory mysqlCDCFactory = createMySQLCDCFactory();

        CDCTestSuitParams suitParam = tabParamMap.get(tabStu);// createStuSuitParams();


        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParam) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return createDataSource(dataxName);
                //return MySqlContainer.createMySqlDataSourceFactory(dataxName, MYSQL_CONTAINER);
            }

            @Override
            protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
                    , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener) throws Exception {
                //  super.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);
                imqListener.start(dataxName, dataxReader, Collections.singletonList(tab), null);
                Thread.sleep(1000);

//                CREATE TABLE `stu` (
//                        `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '',
//                        `` varchar(20) NOT NULL COMMENT '',
//                        `school` varchar(20) NOT NULL COMMENT '',
//                        `nickname` varchar(20) NOT NULL COMMENT '',
//                        `age` int(11) NOT NULL COMMENT '',
//                        `class_num` int(11) NOT NULL COMMENT '班级人数',
//                        `score` decimal(4,2) NOT NULL COMMENT '成绩',
//                        `phone` bigint(20) NOT NULL COMMENT '电话号码',
//                        `email` varchar(64) DEFAULT NULL COMMENT '',
//                        `ip` varchar(32) DEFAULT NULL COMMENT '',
//                        `address` text COMMENT '',
//                        PRIMARY KEY (`id`)
//)              ENGINE=InnoDB AUTO_INCREMENT=1100002 DEFAULT CHARSET=utf8;

                Map<String, RowValsExample.RowVal> vals = Maps.newHashMap();
                vals.put("id", RowValsExample.RowVal.$(1100001l));
                vals.put("name", RowValsExample.RowVal.$("doTun"));
                vals.put("school", RowValsExample.RowVal.$("beida"));
                vals.put("nickname", RowValsExample.RowVal.$("jasper"));
                vals.put("age", RowValsExample.RowVal.$(81));
                vals.put("class_num", RowValsExample.RowVal.$(26));
                vals.put("score", RowValsExample.RowVal.decimal(4554, 2));
                vals.put("phone", RowValsExample.RowVal.$(14597415152l));
                vals.put("email", RowValsExample.RowVal.$("xxx@hotmail.com"));
                vals.put("ip", RowValsExample.RowVal.$("192.192.192.192"));
                vals.put("address", RowValsExample.RowVal.$("极乐世界f座 630103"));
                TestRow stuRow = new TestRow(RowKind.INSERT, new RowValsExample(vals));


                BasicDataSourceFactory dataSourceFactory = (BasicDataSourceFactory) dataxReader.getDataSourceFactory();
                Assert.assertNotNull("dataSourceFactory can not be null", dataSourceFactory);
                dataSourceFactory.visitFirstConnection((conn) -> {

                    insertTestRow(conn, stuRow);

//                    Statement statement = conn.createStatement();
//                    statement.execute("INSERT INTO `stu` (`id`,`name`,`school`,`nickname`,`age`,`class_num`,`score`,`phone`,`email`,`ip`,`address`)\n" +
//                            "VALUES (1100001,'doTun','beida','jasper',81,26,45.54,14597415152,'xxx@hotmail.com','192.192.192.192','极乐世界f座 630103');");
//                    statement.close();
                });

                sleepForAWhile();
                CloseableIterator<Row> snapshot = consumerHandle.getRowSnapshot(tabName);
                waitForSnapshotStarted(snapshot);
                List<AssertRow> rows = fetchRows(snapshot, 1, stuRow, false);
                Object pkVal = null;
                for (AssertRow rr : rows) {
                    pkVal = rr.getObj("id");
                    System.out.println("------------" + pkVal);
                    // assertTestRow(tabName, RowKind.INSERT, consumerHandle, stuRow, rr);
                    assertInsertRow(stuRow, rr);
                }
                Assert.assertEquals(String.valueOf(new Long(1100001)), pkVal);
            }

            @Override
            protected String getColEscape() {
                return "`";
            }
        };

        cdcTestSuit.startTest(mysqlCDCFactory);

    }


//    private static CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder chunjunSuitParamBuilder() {
//        return new CDCTestSuitParams.Builder.ChunjunSuitParamsBuilder();
//    }

    protected abstract MQListenerFactory createMySQLCDCFactory();


    // @Test
    public void testBinlogConsume() throws Exception {

        MQListenerFactory mysqlCDCFactory = createMySQLCDCFactory();

        CDCTestSuitParams params = tabParamMap.get(tabBase);
        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(params) {

            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return createDataSource(dataxName);
                // return MySqlContainer.createMySqlDataSourceFactory(dataxName, MYSQL_CONTAINER);
            }

            @Override
            protected String getColEscape() {
                return "`";
            }
        };

        cdcTestSuit.startTest(mysqlCDCFactory);

    }

    //protected abstract void overwriteSelectedTab(CUDCDCTestSuit cdcTestSuit, String tabName, BasicDataSourceFactory dataSourceFactory, SelectedTab tab);

    // @Test
    public void testBinlogConsumeWithDataStreamRegisterTable() throws Exception {
        MQListenerFactory mysqlCDCFactory = createMySQLCDCFactory();
        // mysqlCDCFactory.startupOptions = "latest";
        // final String tabName = "base";
        CDCTestSuitParams suitParams = tabParamMap.get(tabBase); //new CDCTestSuitParams("base");
        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParams) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return createDataSource(dataxName);// MySqlContainer.createMySqlDataSourceFactory(dataxName, MYSQL_CONTAINER);
            }

//            protected SelectedTab createSelectedTab(String tabName, BasicDataSourceFactory dataSourceFactory) {
//                SelectedTab tab = super.createSelectedTab(tabName, dataSourceFactory);
//                overwriteSelectedTab(this, tabName, dataSourceFactory, tab);
//                return tab;
//            }

            @Override
            protected String getColEscape() {
                return "`";
            }

            @Override
            protected IResultRows createConsumerHandle(String tabName, TISSinkFactory sinkFuncFactory) {
                TestTableRegisterFlinkSourceHandle sourceHandle = new TestTableRegisterFlinkSourceHandle(tabName, cols);
                sourceHandle.setSinkFuncFactory(sinkFuncFactory);
                return sourceHandle;
            }
        };

        cdcTestSuit.startTest(mysqlCDCFactory);

    }

    /**
     * 测试 instancedetail
     *
     * @throws Exception
     */
    // @Test
    public void testBinlogConsumeWithDataStreamRegisterInstaneDetailTable() throws Exception {
        MQListenerFactory mysqlCDCFactory = createMySQLCDCFactory();
        //  mysqlCDCFactory.startupOptions = "latest";


        CDCTestSuitParams suitParams = tabParamMap.get(tabInstanceDetail);
        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParams) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return createDataSource(dataxName);
                // return MySqlContainer.createMySqlDataSourceFactory(dataxName, MYSQL_CONTAINER);
            }

//            protected SelectedTab createSelectedTab(String tabName, BasicDataSourceFactory dataSourceFactory) {
//                SelectedTab tab = super.createSelectedTab(tabName, dataSourceFactory);
//                overwriteSelectedTab(this, tabName, dataSourceFactory, tab);
//                return tab;
//            }

            @Override
            protected String getColEscape() {
                return "`";

            }

            @Override
            protected IResultRows createConsumerHandle(String tabName, TISSinkFactory sinkFuncFactory) {
                TestTableRegisterFlinkSourceHandle sourceHandle = new TestTableRegisterFlinkSourceHandle(tabName, cols);
                sourceHandle.setSinkFuncFactory(sinkFuncFactory);
                return sourceHandle;
            }

            @Override
            protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
                    , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener)
                    throws MQConsumeException, InterruptedException {
                // super.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);

                List<ISelectedTab> tabs = Collections.singletonList(tab);

                List<TestRow> exampleRows = Lists.newArrayList();
                exampleRows.add(this.parseTestRow(RowKind.INSERT, BasicMySQLCDCTest.class, tabName + "/insert1.txt"));

                Assert.assertEquals(1, exampleRows.size());
                imqListener.start(dataxName, dataxReader, tabs, createProcess());

                Thread.sleep(1000);
                CloseableIterator<Row> snapshot = consumerHandle.getRowSnapshot(tabName);
                BasicDataSourceFactory dataSourceFactory = (BasicDataSourceFactory) dataxReader.getDataSourceFactory();
                Assert.assertNotNull("dataSourceFactory can not be null", dataSourceFactory);
                dataSourceFactory.visitFirstConnection((conn) -> {
                    startProcessConn(conn);

                    for (TestRow t : exampleRows) {
                        RowValsExample vals = t.vals;
                        final String insertBase
                                = "insert into " + createTableName(tabName) + "("
                                + cols.stream().filter((c) -> vals.notNull(c.getName())).map((col) -> getColEscape() + col.getName() + getColEscape()).collect(Collectors.joining(" , ")) + ") " +
                                "values(" +
                                cols.stream().filter((c) -> vals.notNull(c.getName()))
                                        .map((col) -> "?")
                                        .collect(Collectors.joining(" , ")) + ")";

                        PreparedStatement statement = conn.prepareStatement(insertBase);
                        AtomicInteger ci = new AtomicInteger();
                        cols.stream().filter((c) -> vals.notNull(c.getName())).forEach((col) -> {
                            col.getType().accept(new DataType.TypeVisitor<Void>() {
                                @Override
                                public Void bigInt(DataType type) {
                                    try {
                                        statement.setLong(ci.incrementAndGet(), Long.parseLong(vals.getString(col.getName())));
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void doubleType(DataType type) {
                                    try {
                                        statement.setDouble(ci.incrementAndGet(), Double.parseDouble(vals.getString(col.getName())));
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void dateType(DataType type) {
                                    try {
                                        statement.setDate(ci.incrementAndGet(), java.sql.Date.valueOf(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    return null;
                                }

                                @Override
                                public Void timestampType(DataType type) {

                                    try {
                                        statement.setTimestamp(ci.incrementAndGet(), java.sql.Timestamp.valueOf(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    return null;
                                }

                                @Override
                                public Void bitType(DataType type) {
                                    try {
                                        statement.setByte(ci.incrementAndGet(), Byte.parseByte(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    return null;
                                }

                                @Override
                                public Void blobType(DataType type) {
                                    try {
                                        try (InputStream input = new ByteArrayInputStream(vals.getString(col.getName()).getBytes(TisUTF8.get()))) {
                                            statement.setBlob(ci.incrementAndGet(), input);
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void varcharType(DataType type) {
                                    try {
                                        statement.setString(ci.incrementAndGet(), (vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                    return null;
                                }

                                @Override
                                public Void intType(DataType type) {
                                    try {
                                        statement.setInt(ci.incrementAndGet(), Integer.parseInt(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void floatType(DataType type) {
                                    try {
                                        statement.setFloat(ci.incrementAndGet(), Float.parseFloat(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void decimalType(DataType type) {
                                    try {
                                        statement.setBigDecimal(ci.incrementAndGet(), BigDecimal.valueOf(Double.parseDouble(vals.getString(col.getName()))));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void timeType(DataType type) {
                                    try {
                                        statement.setTime(ci.incrementAndGet(), java.sql.Time.valueOf(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void tinyIntType(DataType dataType) {
                                    try {
                                        statement.setShort(ci.incrementAndGet(), Short.parseShort(vals.getString(col.getName())));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void smallIntType(DataType dataType) {
                                    tinyIntType(dataType);
                                    return null;
                                }
                            });
                        });


                        Assert.assertEquals(1, executePreparedStatement(conn, statement));

                        statement.close();
                        sleepForAWhile();

                        System.out.println("wait to show insert rows");
                        waitForSnapshotStarted(snapshot);

                        List<AssertRow> rows = fetchRows(snapshot, 1, t, false);
                        for (AssertRow rr : rows) {
                            System.out.println("------------" + rr.getObj("instance_id"));
                            // assertTestRow(tabName, RowKind.INSERT, consumerHandle, t, rr);
                            assertInsertRow(t, rr);
                        }

                    }
                });
            }
        };

        cdcTestSuit.startTest(mysqlCDCFactory);
    }


}
