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

package com.qlangtech.plugins.incr.flink.cdc;

import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.mysql.MySqlSourceTestBase;
import com.qlangtech.plugins.incr.flink.cdc.source.TestBasicFlinkSourceHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.CloseableIterator;
import org.junit.Assert;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * CDC增量监听测试套件
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-19 09:27
 **/
public abstract class CUDCDCTestSuit {

    protected final CDCTestSuitParams suitParam;
    private final String tabName;
    private final Optional<String> splitTabSuffix;

    final Date now = new Date();
    private final Calendar calendar;
    private final AtomicInteger timeGetterCount = new AtomicInteger();

    public CUDCDCTestSuit(CDCTestSuitParams suitParam) {
        this(suitParam, Optional.empty());
    }

    public CUDCDCTestSuit(CDCTestSuitParams suitParam, Optional<String> splitTabSuffix) {
        this.suitParam = suitParam;
        this.tabName = suitParam.tabName;
        this.splitTabSuffix = splitTabSuffix;
        calendar = Calendar.getInstance();
        calendar.setTime(now);
    }

    protected final TargetResName dataxName = new TargetResName("x");
    static String keyCol_text = "col_text";
    public static String keyStart_time = "start_time";
    public static String key_update_time = "update_time";
    static String keyBaseId = "base_id";
    String keyColBlob = "col_blob";

    protected List<ColMeta> cols;

//    public List<FlinkCol> cols = Lists.newArrayList(
//            new FlinkCol(keyBaseId, DataTypes.INT())
////            , new FlinkCol(keyStart_time, DataTypes.TIMESTAMP())
////            , new FlinkCol("update_date", DataTypes.DATE())
////            , new FlinkCol("update_time", DataTypes.TIMESTAMP())
//
//            , new FlinkCol(keyStart_time, DataTypes.STRING())
//            , new FlinkCol("update_date", DataTypes.STRING())
//            , new FlinkCol("update_time", DataTypes.STRING())
//
//            , new FlinkCol("price", DataTypes.DECIMAL(5, 2))
//            , new FlinkCol("json_content", DataTypes.STRING())
//            // , new FlinkCol(keyColBlob, DataTypes.STRING())
//            , new FlinkCol(keyColBlob, DataTypes.BYTES(), FlinkCol.Bytes())
//            , new FlinkCol(keyCol_text, DataTypes.STRING()));


    protected static final ThreadLocal<SimpleDateFormat> timeFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    protected static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };


    public void startTest(MQListenerFactory cdcFactory) throws Exception {


        BasicDataXRdbmsReader dataxReader = createDataxReader(dataxName, tabName);

        //  replay();
        List<SelectedTab> selectedTabs = dataxReader.getSelectedTabs();
        Optional<SelectedTab> firstSelectedTab
                = selectedTabs.stream().filter((t) -> tabName.equals(t.name)).findFirst();
        Assert.assertTrue("firstSelectedTab:" + tabName + " must be present", firstSelectedTab.isPresent());


        ISelectedTab tab = firstSelectedTab.get();

        this.cols = Lists.newArrayList();
        HdfsColMeta cMeta = null;
        int colIndex = 1;
        for (CMeta c : tab.getCols()) {
            cMeta = new HdfsColMeta(c.getName(), c.isNullable(), c.isPk(), c.getType());
            cols.add(new ColMeta(colIndex++, cMeta));
        }


        IResultRows consumerHandle = getTestBasicFlinkSourceHandle(dataxReader, tabName);

        cdcFactory.setConsumerHandle(consumerHandle.getConsumerHandle());

        IMQListener<JobExecutionResult> imqListener = cdcFactory.create();


        this.verfiyTableCrudProcess(tabName, dataxReader, tab, consumerHandle, imqListener);


        consumerHandle.cancel();
    }

    public final String getPrimaryKeyName(ISelectedTab tab) {

        for (CMeta col : tab.getCols()) {
            if (col.isPk()) {
                return getColEscape() + col.getName() + getColEscape();
            }
        }

        throw new IllegalStateException("can not find primary key in:"
                + tab.getCols().stream().map((m) -> m.getName()).collect(Collectors.joining(",")));

        // return "base_id";
    }

    private BasicDataSourceFactory dataSourceFactory;

    public final void visitConn(final BasicDataSourceFactory.IConnProcessor conn) {
        Objects.requireNonNull(dataSourceFactory, "dataSourceFactory can not be null").visitFirstConnection((c) -> {
            startProcessConn(c);
            conn.vist(c);
        });
    }

    protected void verfiyTableCrudProcess(String tabName, BasicDataXRdbmsReader dataxReader
            , ISelectedTab tab, IResultRows consumerHandle, IMQListener<JobExecutionResult> imqListener)
            throws Exception {
//        File file = new File("full_types.xml");
//        XmlFile tabStore = new XmlFile(file.getAbsoluteFile(), "test");
//        tabStore.write(tab, Sets.newHashSet());

        List<ISelectedTab> tabs = Collections.singletonList(tab);

        List<TestRow> exampleRows = createExampleTestRows();

        DataxProcessor process = createProcess();

        imqListener.start(dataxName, dataxReader, tabs, process);

        Thread.sleep(4000);


        CloseableIterator<Row> snapshot = consumerHandle.getRowSnapshot(tabName);
        //insertCount
        this.dataSourceFactory = (BasicDataSourceFactory) dataxReader.getDataSourceFactory();
        Assert.assertNotNull("dataSourceFactory can not be null", dataSourceFactory);
//        dataSourceFactory.visitFirstConnection((conn) -> {
//                    startProcessConn(conn);
        // PreparedStatement statement = null;
        try {
            // 执行添加
            System.out.println("start to insert");
            for (TestRow r : exampleRows) {
                visitConn((conn) -> {
                    insertTestRow(conn.getConnection(), r);
                });
                sleepForAWhile();

                System.out.println("wait to show insert rows");
                waitForSnapshotStarted(snapshot);

                List<AssertRow> rows = fetchRows(snapshot, 1, r, false);
                for (AssertRow rr : rows) {
                    System.out.println("------------" + rr.getObj(this.getPrimaryKeyName(tab)));
                    assertTestRow(tabName, Optional.of(new ExpectRowGetter(RowKind.INSERT, false)), consumerHandle, r, rr);
                }
                // System.out.println("########################");

            }
            System.out.println("start to test update");

            Optional<AssertRow> find = null;
            // 执行更新
            for (TestRow exceptRow : exampleRows) {
                if (!exceptRow.execUpdate()) {
                    continue;
                }

                List<Map.Entry<String, RowValsUpdate.UpdatedColVal>> cols = exceptRow.getUpdateValsCols();


                visitConn((c) -> {
                    Connection conn = c.getConnection();
                    try {
                        String updateSql = String.format("UPDATE " + getColEscape() + createTableName(tabName) + getColEscape() + " set %s WHERE " + getPrimaryKeyName(tab) + "=%s"
                                , cols.stream().map((e) -> getColEscape() + e.getKey() + getColEscape() + " = ?").collect(Collectors.joining(","))
                                , Objects.requireNonNull(exceptRow.getIdVal(), "idVal can not be null"));
                        try (PreparedStatement updateStatement = conn.prepareStatement(updateSql)) {
                            int colIndex = 1;
                            for (Map.Entry<String, RowValsUpdate.UpdatedColVal> col : cols) {
                                col.getValue().setPrepColVal(updateStatement, colIndex++, exceptRow.vals);
                            }
                            Assert.assertTrue(updateSql, executePreparedStatement(conn, updateStatement) > 0);
                        }
                        conn.commit();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                //  statement.close();
                sleepForAWhile();

                waitForSnapshotStarted(snapshot);
                int fetchRowSize = this.suitParam.updateRowKind.size();

                List<AssertRow> rows = fetchRows(snapshot, fetchRowSize, exceptRow, false);
                Assert.assertEquals("rowSize must be", fetchRowSize, rows.size());
                for (RowKind eventKind : this.suitParam.updateRowKind) {
                    find = rows.stream().filter((r) -> r.kind == eventKind).findFirst();
                    Assert.assertTrue("eventKind:" + eventKind + " must be find ", find.isPresent());

                    assertTestRow(tabName, Optional.of(new ExpectRowGetter(eventKind, true))
                            , consumerHandle, exceptRow, find.get());
                }

//                            for (AssertRow rr : rows) {
//                                //System.out.println("------------" + rr.getInt(keyBaseId));
//                                // RowKind.UPDATE_AFTER
//                                assertTestRow(tabName, Optional.of(new ExpectRowGetter(this.suitParam.updateRowKind, true))
//                                        , consumerHandle, exceptRow, rr);
//
//                            }
            }


            System.out.println("start to test delete");
            // 执行删除
            this.visitConn((c) -> {
                Connection conn = c.getConnection();
                for (TestRow r : exampleRows) {
                    if (!r.execDelete()) {
                        continue;
                    }

                    String deleteSql = String.format("DELETE FROM " + getColEscape() + createTableName(tabName) + getColEscape() + " WHERE " + getPrimaryKeyName(tab) + "=%s", r.getIdVal());

                    try (Statement statement1 = conn.createStatement()) {
                        Assert.assertTrue(deleteSql, executeStatement(conn, statement1, (deleteSql)) > 0);
                        conn.commit();
                    }

                    sleepForAWhile();
                    waitForSnapshotStarted(snapshot);
                    List<AssertRow> rows = fetchRows(snapshot, 1, r, true);
                    for (AssertRow rr : rows) {
                        //System.out.println("------------" + rr.getInt(keyBaseId));
                        assertTestRow(tabName, Optional.of(new ExpectRowGetter(RowKind.DELETE, true)), consumerHandle, r, rr);

                    }
                }
            });


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // }

    }


    protected DataxProcessor createProcess() {
        DataxProcessor processor = new DataxProcessor() {
            @Override
            public StoreResourceType getResType() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Application buildApp() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IDataxGlobalCfg getDataXGlobalCfg() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isRDBMS2RDBMS(IPluginContext pluginCtx) {
                return true;
            }

            @Override
            public String identityValue() {
                throw new UnsupportedOperationException();
            }
        };
        processor.setTableMaps(Lists.newArrayList(new TableAlias(this.tabName)));
        return processor;
    }

    protected void insertTestRow(Connection conn, TestRow r) throws SQLException {

        final String insertBase = createInsertScript(tabName, r);

        try (PreparedStatement statement = conn.prepareStatement(insertBase)) {

            for (ColMeta col : this.cols) {
                col.setTestVal(statement, r);
            }
            Assert.assertEquals(1, executePreparedStatement(conn, statement));
            System.out.println("insert:" + r.getValsList(Optional.empty(), cols).stream().collect(Collectors.joining(",")));
        }
        conn.commit();
    }


    private String createInsertScript(String tabName, TestRow r) {

        return "insert into " + getColEscape() + createTableName(tabName) + getColEscape() + "("
                + cols.stream().map((col) -> getColEscape() + col.getName() + getColEscape()).collect(Collectors.joining(" , ")) + ") " +
                "values(" +
                cols.stream().map((col) -> {
                    RowValsExample.RowVal v = r.vals.getV(col.getName());
                    if (v.sqlParamDecorator != null) {
                        return v.sqlParamDecorator.get();
                    }
                    return "?";
                }).collect(Collectors.joining(" , ")) + ")";
    }

    protected List<TestRow> createExampleTestRows() throws Exception {
        List<TestRow> exampleRows = Lists.newArrayList();
        Date now = null;
        TestRow row = null;
        Map<String, RowValsExample.RowVal> vals = null;

        if (!MySqlSourceTestBase.tabBase.equals(this.tabName)) {
            throw new IllegalStateException(
                    "tab:" + this.tabName + " must be " + MySqlSourceTestBase.tabBase);
        }


        int insertCount = 5;

        // setup.sql 中已经插入了baseid 为1的记录，为了避免重复，这里id从2开始插入
        for (int i = 2; i <= insertCount + 1; i++) {
            //  if (i > 1) {
            now = this.getTime();
            // }
            vals = Maps.newHashMap();
            vals.put(keyBaseId, RowValsExample.RowVal.$(i));
            vals.put(keyStart_time, parseTimestamp(timeFormat.get().format(now)));
            vals.put("update_date", parseDate(dateFormat.get().format(now)));
            vals.put(key_update_time, parseTimestamp(timeFormat.get().format(now)));
            vals.put("price", RowValsExample.RowVal.decimal(199, 2));
            vals.put("json_content", RowValsExample.RowVal.json("{\"name\":\"baisui#" + i + "\"}"));
            vals.put("col_blob", RowValsExample.RowVal.stream("Hello world"));
            vals.put(keyCol_text, RowValsExample.RowVal.$("我爱北京天安门" + i));
            row = new TestRow(RowKind.INSERT, new RowValsExample(vals));
            row.idVal = i;
            exampleRows.add(row);
        }

        // 执行三条更新
        row = exampleRows.get(3);
        row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
            String newVal = "update#" + ovals.getString(keyCol_text);
            statement.setString(index, newVal);
            return RowValsExample.RowVal.$(newVal);
        });
        row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
            String v = "2012-11-13 11:11:35";
            RowValsExample.RowVal val = parseTimestamp(v);
            statement.setTimestamp(index, val.getVal());
            return val;
        });
        row.updateVals.put(key_update_time, (statement, index, ovals) -> {
            String v = timeFormat.get().format(this.getTime());
            RowValsExample.RowVal val = parseTimestamp(v);
            statement.setTimestamp(index, val.getVal());
            return val;
        });


        row = exampleRows.get(4);
        row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
            String v = "update#" + ovals.getString(keyCol_text);
            statement.setString(index, v);
            return RowValsExample.RowVal.$(v);
        });
        row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
            String v = "2012-11-13 11:11:35";
            RowValsExample.RowVal rowVal = parseTimestamp(v);
            statement.setTimestamp(index, rowVal.getVal());
            return rowVal;
        });
        row.updateVals.put(key_update_time, (statement, index, ovals) -> {
            String v = timeFormat.get().format(this.getTime());
            RowValsExample.RowVal rowVal = parseTimestamp(v);
            statement.setTimestamp(index, rowVal.getVal());
            return rowVal;
        });

        row = exampleRows.get(0);
        row.updateVals.put(keyCol_text, (statement, index, ovals) -> {
            String v = "update#" + ovals.getString(keyCol_text);
            statement.setString(index, v);
            return RowValsExample.RowVal.$(v);
        });
        row.updateVals.put(keyStart_time, (statement, index, ovals) -> {
            String v = "2012-11-12 11:11:35";
            RowValsExample.RowVal rowVal = parseTimestamp(v);
            statement.setTimestamp(index, rowVal.getVal());
            return rowVal;//RowValsExample.RowVal.$(v);
        });
        row.updateVals.put(key_update_time, (statement, index, ovals) -> {
            String v = timeFormat.get().format(this.getTime());
            RowValsExample.RowVal rowVal = parseTimestamp(v);
            statement.setTimestamp(index, rowVal.getVal());
            return rowVal;
        });

        if (this.suitParam.shallTestDeleteProcess) {
            // 执行两条删除
            row = exampleRows.get(1);
            row.willbeDelete = true;

            row = exampleRows.get(3);
            row.willbeDelete = true;
        }
        return exampleRows;
    }

    private Date getTime() {
        this.calendar.add(Calendar.SECOND, timeGetterCount.getAndIncrement());
        return this.calendar.getTime();
    }

    protected String createTableName(String tabName) {
        return tabName + (this.splitTabSuffix.isPresent() ? this.splitTabSuffix.get() : StringUtils.EMPTY);
    }

    protected final String getColEscape() {
        Optional<String> escapeChar = this.dataSourceFactory.getEscapeChar();// StringUtils.EMPTY;
        return escapeChar.isPresent() ? escapeChar.get() : StringUtils.EMPTY;
    }

    protected int executePreparedStatement(Connection connection, PreparedStatement statement) throws SQLException {
        return statement.executeUpdate();
    }

    protected int executeStatement(Connection connection, Statement statement, String sql) throws SQLException {
        return statement.executeUpdate(sql);
    }

    protected void startProcessConn(DataSourceMeta.JDBCConnection conn) throws SQLException {
        conn.getConnection().setAutoCommit(false);
    }

    protected IResultRows getTestBasicFlinkSourceHandle(BasicDataXRdbmsReader dataxReader, String tabName) {
        IResultRows consumerHandle = createConsumerHandle(dataxReader, tabName);
        return consumerHandle;
    }

    private IResultRows createConsumerHandle(BasicDataXRdbmsReader dataxReader, String tabName) {
        // TestBasicFlinkSourceHandle sourceHandle = new TestBasicFlinkSourceHandle(tabName);
        TISSinkFactory sinkFuncFactory = new StubSinkFactory();
        sinkFuncFactory.dataXName = dataxName.getName();

        return createConsumerHandle(dataxReader, tabName, sinkFuncFactory);
    }

    private class StubSinkFactory extends TISSinkFactory implements IStreamTableMeataCreator.ISinkStreamMetaCreator {
        @Override
        public IStreamTableMeta getStreamTableMeta(String tableName) {
            return new IStreamTableMeta() {
                @Override
                public List<IColMetaGetter> getColsMeta() {
                    if (CollectionUtils.isEmpty(cols)) {
                        throw new IllegalStateException("cols can not be null");
                    }
                    return cols.stream().map(c -> c.meta).collect(Collectors.toList());
                }
            };
        }

        @Override
        public ICompileAndPackage getCompileAndPackageManager() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<TableAlias, SinkFunction<DTO>> createSinkFunction(IDataxProcessor dataxProcessor) {
            return Collections.emptyMap();
        }
    }


    protected IResultRows createConsumerHandle(BasicDataXRdbmsReader dataxReader, String tabName, TISSinkFactory sinkFuncFactory) {
        TestBasicFlinkSourceHandle sourceHandle = new TestBasicFlinkSourceHandle(tabName);
        sourceHandle.setSinkFuncFactory(sinkFuncFactory);
        return sourceHandle;
    }

    private BasicDataXRdbmsReader createDataxReader(TargetResName dataxName, String tabName) {
        BasicDataSourceFactory dataSourceFactory = createDataSourceFactory(dataxName, this.splitTabSuffix.isPresent());


        BasicDataXRdbmsReader dataxReader = new BasicDataXRdbmsReader() {
            @Override
            protected RdbmsReaderContext createDataXReaderContext(String jobName, SelectedTab tab, IDataSourceDumper dumper) {
                return null;
            }

            @Override
            public DataSourceFactory getDataSourceFactory() {
                return dataSourceFactory;
            }
        };

        SelectedTab baseTab = createSelectedTab(tabName, dataSourceFactory);
        dataxReader.selectedTabs = Collections.singletonList(baseTab);
        return dataxReader;
    }

    protected final SelectedTab createSelectedTab(String tabName, BasicDataSourceFactory dataSourceFactory) {
        try {
            return TestSelectedTab.createSelectedTab(EntityName.parse(tabName), dataSourceFactory, (tab) -> {
                if (suitParam.overwriteSelectedTab != null) {
                    suitParam.overwriteSelectedTab.apply(this, tabName, dataSourceFactory, tab);
                }
            });
        } catch (TableNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    protected abstract BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName, boolean useSplitTabStrategy);


    protected void assertInsertRow(
            TestRow expect, AssertRow actual) {
        assertTestRow(null, Optional.of(new ExpectRowGetter(RowKind.INSERT, false)), null, expect, actual);
    }

    protected void assertTestRow(String tabName, Optional<ExpectRowGetter> expectKind
            , IResultRows consumerHandle, TestRow expect, AssertRow actual) {
        try {
            assertEqualsInOrder(
                    expect.getValsList(expectKind, cols) //
                    , actual.getValsList(cols));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected RowValsExample.RowVal parseTimestamp(String timeLiterial) {
        try {
            Timestamp t = new Timestamp(timeFormat.get().parse(timeLiterial).getTime());

            return RowValsExample.RowVal.timestamp(t);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected RowValsExample.RowVal parseDate(String timeLiterial) {
        try {
            java.sql.Date d = new java.sql.Date(dateFormat.get().parse(timeLiterial).getTime());
            return RowValsExample.RowVal.date(d);
//            return new RowValsExample.RowVal(d) {
//                @Override
//                public String getExpect() {
//                    return String.valueOf(d.getTime());
//                }
//            };
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    protected void sleepForAWhile() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }


    protected static void waitForSnapshotStarted(CloseableIterator<Row> iterator) {
        try {
            System.out.println("star wait ---");
            while (!iterator.hasNext()) {
                System.out.println("waitForSnapshotStarted");
                Thread.sleep(100);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<AssertRow> fetchRows(Iterator<Row> iter, int size, TestRow exampleRow, boolean deleteRow) {
        //List<Map.Entry<String, RowValsExample.RowVal>> cols = exampleRow.vals.getCols();
        List<AssertRow> rows = new ArrayList<>(size);
        Objects.requireNonNull(exampleRow, "param exampleRow can not be null");

        while (size > 0 && iter.hasNext()) {
            Row row = iter.next();
            //System.out.println("=========" + row.getField(keyBaseId) + ",detail:" + row.toString());
            // ignore rowKind marker
            AssertRow.AssertVals vals = new AssertRow.AssertVals();

            for (Map.Entry<String, RowValsExample.RowVal> e : exampleRow.vals.getCols()) {
                vals.put(e.getKey(), () -> e.getValue().getAssertActual(row.getField(e.getKey())));
            }

//            for (String key : row.getFieldNames(true)) {
//                vals.put(key, row.getField(key));
//            }
            rows.add(new AssertRow(row.getKind(), vals));
            size--;
        }
        return rows;
    }

    public static void assertEqualsInAnyOrder(List<String> expected, List<String> actual) {
        assertTrue(expected != null && actual != null);
        assertEqualsInOrder(
                expected.stream().sorted().collect(Collectors.toList()),
                actual.stream().sorted().collect(Collectors.toList()));
    }

    public static void assertEqualsInOrder(List<String> expected, List<String> actual) {
        assertTrue(expected != null && actual != null);
        assertEquals(expected.size(), actual.size());
        assertArrayEquals(expected.toArray(new String[0]), actual.toArray(new String[0]));
    }


    /**
     * 构建测试样本
     *
     * @param kind
     * @param path
     * @return
     */
    protected TestRow parseTestRow(RowKind kind, Class<?> clazz, String path) {
        return new TestRow(kind, com.qlangtech.tis.extension.impl.IOUtils.loadResourceFromClasspath(
                clazz, path, true, (input) -> {
                    Map<String, RowValsExample.RowVal> vals = Maps.newHashMap();
                    String colName = null;
                    String colValue = null;
                    LineIterator it = null;
                    String line = null;
                    it = IOUtils.lineIterator(input, TisUTF8.get());
                    while (it.hasNext()) {
                        line = it.nextLine();
                        colName = StringUtils.trimToEmpty(StringUtils.substringBefore(line, ":"));
                        colValue = StringUtils.trimToEmpty(StringUtils.substringAfter(line, ":"));
                        if (!"null".equalsIgnoreCase(colValue)) {
                            vals.put(colName, RowValsExample.RowVal.$(colValue));
                        }
                    }
                    return new RowValsExample(vals);
                }));
    }
}
