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

package com.qlangtech.plugins.incr.flink.chunjun.doris.sink;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.IResultRows;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.ReplaceType;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.test.TISEasyMock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.test.util.AbstractTestBase;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-17 16:07
 **/
public abstract class TestFlinkSinkExecutor extends AbstractTestBase implements TISEasyMock {


    protected static String dataXName = "testDataX";

    static final String tableName = "totalpayinfo";
    protected static final String dbName = "tis";

    String colEntityId = "entity_id";
    String colNum = "num";
    static protected String colId = "id";
    String colCreateTime = "create_time";
    protected String updateTime = "update_time";
    String updateDate = "update_date";
    static String starTime = "start_time";

    String pk = "88888888887";
    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    static {
        System.setProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY, "logback-test.xml");
    }

    protected abstract BasicDataSourceFactory getDsFactory();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void startClearMocks() {
        this.clearMocks();
    }

    @Ignore
    @Test
    public void test() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DTO d = createDTO(DTO.EventType.ADD);
        DTO update = createDTO(DTO.EventType.UPDATE_AFTER, (after) -> {
            after.put(colNum, 999);
        });

        // env.fromElements(new DTO[]{d, update}).addSink(new PrintSinkFunction<>());

        DTOStream sourceStream = DTOStream.createDispatched(tableName);
//
        ReaderSource<DTO> readerSource = ReaderSource.createDTOSource("testStreamSource", env.fromElements(new DTO[]{d, update}));
//
        readerSource.getSourceStream(env
                , new Tab2OutputTag<>(Collections.singletonMap(new TableAlias(tableName), sourceStream)));
//
//
        //  dtoStream.addSink(new PrintSinkFunction<>());
        sourceStream.getStream().addSink(new PrintSinkFunction<>());

        // env.fromElements(new DTO[]{d}).addSink(entry.getValue());

        env.execute("testJob");

        Thread.sleep(5000);

    }

    private int updateNumVal = 999;

    protected DTO[] createTestDTO() {

        DTO add = createDTO(DTO.EventType.ADD);
        final DTO updateBefore = createDTO(DTO.EventType.UPDATE_BEFORE, (after) -> {
            after.put(colNum, updateNumVal);
            after.put(updateTime, "2021-12-17 09:21:22");
        });
        final DTO updateAfter = updateBefore.colone();
        updateAfter.setEventType(DTO.EventType.UPDATE_AFTER);

        final DTO delete = updateBefore.colone();
        delete.setEventType(DTO.EventType.DELETE);
        return new DTO[]{add, updateAfter, delete};
    }

    /**
     * 尝试使用Stream API流程测试
     *
     * @throws Exception
     */
    protected void testSinkSync() throws Exception {
        testSinkSync((dataxProcessor, sinkFactory, env, selectedTab) -> {

            Map<TableAlias, TabSinkFunc<RowData>> sinkFunction = sinkFactory.createSinkFunction(dataxProcessor);
            //int updateNumVal = 999;
            Assert.assertEquals(1, sinkFunction.size());
            for (Map.Entry<TableAlias, TabSinkFunc<RowData>> entry : sinkFunction.entrySet()) {

                Pair<DTOStream, ReaderSource<DTO>> sourceStream = createReaderSource(env, entry.getKey());

                entry.getValue().add2Sink(sourceStream.getKey());

                //  sourceStream.getStream().addSink();

                // entry.getValue().add2Sink(sourceStream.addStream(env.fromElements(new DTO[]{d, update})));
                // env.fromElements(new DTO[]{d}).addSink(entry.getValue());
                break;
            }

            env.execute("testJob");

        });
    }

    // @Test
    protected void testSinkSync(IStreamScriptRun streamScriptRun) throws Exception {


        //  System.out.println("logger.getClass():" + logger.getClass());

        /**
         建表需要将update_time作为uniqueKey的一部分，不然更新会有问题
         CREATE TABLE `totalpayinfo` (
         `id` varchar(32) NULL COMMENT "",
         `update_time` DATETIME   NULL,
         `entity_id` varchar(10) NULL COMMENT "",
         `num` int(11) NULL COMMENT "",
         `create_time` bigint(20) NULL COMMENT "",
         `update_date` DATE       NULL,
         `start_time`  DATETIME   NULL
         ) ENGINE=OLAP
         UNIQUE KEY(`id`,`update_time`)
         DISTRIBUTED BY HASH(`id`) BUCKETS 10
         PROPERTIES (
         "replication_num" = "1"
         );
         * */

        try {

            //   String[] colNames = new String[]{colEntityId, colNum, colId, colCreateTime, updateTime, updateDate, starTime};
            SelectedTab totalpayInfo = createSelectedTab();
            // tableName = totalpayInfo.getName();
            DataxProcessor dataxProcessor = mock("dataxProcessor", DataxProcessor.class);

            File ddlDir = folder.newFolder("ddl");
            String tabSql = tableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX;


            EasyMock.expect(dataxProcessor.getDataxCreateDDLDir(null)).andReturn(ddlDir);

            DataxProcessor.processorGetter = (name) -> {
                return dataxProcessor;
            };
            IDataxReader dataxReader = createDataxReader();
            List<ISelectedTab> selectedTabs = Lists.newArrayList();


//            EasyMock.expect(sinkExt.getCols()).andReturn(metaCols).times(3);
            selectedTabs.add(totalpayInfo);
            EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs).anyTimes();

            EasyMock.expect(dataxProcessor.getReader(null)).andReturn(dataxReader).anyTimes();

            // BasicDataSourceFactory dsFactory = MySqlContainer.createMySqlDataSourceFactory(new TargetResName(dataXName), MYSQL_CONTAINER);
            BasicDataXRdbmsWriter dataXWriter = createDataXWriter();


            dataXWriter.autoCreateTable = true;
            dataXWriter.dataXName = dataXName;
            // dataXWriter.maxBatchRows = 100;
            DataxWriter.dataxWriterGetter = (xName) -> {
                Assert.assertEquals(dataXName, xName);
                return dataXWriter;
            };

            // Assert.assertTrue("autoCreateTable must be true", dataXWriter.autoCreateTable);
            CreateTableSqlBuilder.CreateDDL createDDL = dataXWriter.generateCreateDDL(new IDataxProcessor.TableMap(totalpayInfo));
            Assert.assertNotNull("createDDL can not be empty", createDDL);
            log.info("create table ddl:\n{}", createDDL);
            FileUtils.write(new File(ddlDir, tabSql), createDDL.getDDLScript(), TisUTF8.get());

            // EasyMock.expect(dataXWriter.getDataSourceFactory()).andReturn(sourceFactory);

            //   dataXWriter.initWriterTable(tableName, Collections.singletonList("jdbc:mysql://192.168.28.201:9030/tis"));

            EasyMock.expect(dataxProcessor.getWriter(null)).andReturn(dataXWriter).anyTimes();

            ChunjunSinkFactory sinkFactory = getSinkFactory();
            sinkFactory.setKey(new KeyedPluginStore.Key(null, dataXName, null));
            sinkFactory.batchSize = 100;
            sinkFactory.flushIntervalMills = 100000;
            sinkFactory.semantic = "at-least-once";
            sinkFactory.parallelism = 1;
            TISSinkFactory.stubGetter = (pn) -> {
                return sinkFactory;
            };

            Map<String, TableAlias> aliasMap = new HashMap<>();
            TableAlias tab = new TableAlias(tableName);
            aliasMap.put(tableName, tab);
            EasyMock.expect(dataxProcessor.getTabAlias()).andReturn(new TableAliasMapper(aliasMap)).anyTimes();

            this.replay();

            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


            streamScriptRun.runStream(dataxProcessor, sinkFactory, env, totalpayInfo);
            Thread.sleep(9000);


            // DBConfig dbConfig = this.getDsFactory().getDbConfig();

//            String[] jdbcUrls = new String[1];
//            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
//                jdbcUrls[0] = jdbcUrl;
//            });

            this.getDsFactory().visitFirstConnection((conn) -> {
                try (Statement statement = conn.createStatement()) {
                    // + " where id='" + pk + "'"
                    try (ResultSet resultSet = statement.executeQuery(createDDL.getSelectAllScript())) {
                        if (resultSet.next()) {
                            IResultRows.printRow(resultSet);
                            assertResultSetFromStore(resultSet);
                        } else {
                            Assert.fail("have not find row with id=" + pk);
                        }
                    }
                }
            });

//            try (Connection conn = this.getDsFactory().getConnection(jdbcUrls[0])) {
//
//            }

//            DBConfig dbConfig = dsFactory.getDbConfig();
//            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
//                try (Connection conn = dsFactory.getConnection(jdbcUrl)) {
//
//
//
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
            this.verifyAll();


        } catch (Throwable e) {
            Thread.sleep(14000);
            throw new RuntimeException(e);
        }
    }


    interface IStreamScriptRun {

        void runStream(DataxProcessor dataxProcessor
                , ChunjunSinkFactory sinkFactory, StreamExecutionEnvironment env, SelectedTab selectedTab) throws Exception;
    }


    protected Pair<DTOStream, ReaderSource<DTO>> createReaderSource(StreamExecutionEnvironment env, TableAlias tableAlia) {
        DTOStream sourceStream = DTOStream.createDispatched(tableAlia.getFrom());
        ReaderSource<DTO> readerSource = ReaderSource.createDTOSource("testStreamSource"
                , env.fromElements(this.createTestDTO()).setParallelism(1));
        readerSource.getSourceStream(env
                , new Tab2OutputTag<>(Collections.singletonMap(tableAlia, sourceStream)));
        return Pair.of(sourceStream, readerSource);
    }

    protected void assertResultSetFromStore(ResultSet resultSet) throws SQLException {
        Assert.assertEquals(updateNumVal, resultSet.getInt(colNum));
    }

    protected DataxReader createDataxReader() {
        return mock("dataxReader", DataxReader.class);
    }


    protected SelectedTab createSelectedTab() {
        SinkTabPropsExtends sinkExt = new SinkTabPropsExtends();
        sinkExt.tabName = tableName;


        //  EasyMock.expect(sinkExt.tabName).andReturn(tableName).times(2);
        //InsertType updateMode = new InsertType();

        UpdateMode updateMode = createIncrMode();
        // EasyMock.expect(sinkExt.getIncrMode()).andReturn(updateMode);
        sinkExt.incrMode = updateMode;
        sinkExt.uniqueKey = getUniqueKey();
        List<CMeta> metaCols = Lists.newArrayList();
        CMeta cm = new CMeta();
        cm.setName(colEntityId);
        cm.setType(new DataType(Types.VARCHAR, "VARCHAR", 6));
        metaCols.add(cm);

        cm = new CMeta();
        cm.setName(colNum);
        cm.setType(new DataType(Types.INTEGER));
        metaCols.add(cm);

        cm = new CMeta();
        cm.setName(colId);
        cm.setType(new DataType(Types.VARCHAR, "VARCHAR", 32));
        cm.setPk(true);
        metaCols.add(cm);

        cm = new CMeta();
        cm.setName(colCreateTime);
        cm.setType(new DataType(Types.BIGINT, "bigint", 8));
        metaCols.add(cm);

        cm = createUpdateTime();
        metaCols.add(cm);

        cm = new CMeta();
        cm.setName(updateDate);
        cm.setType(new DataType(Types.DATE));
        metaCols.add(cm);

        cm = new CMeta();
        cm.setName(starTime);
        cm.setType(new DataType(Types.TIMESTAMP));
        metaCols.add(cm);

        SelectedTab totalpayInfo = new SelectedTab() {
            @Override
            public List<CMeta> getCols() {
                return metaCols;
            }
        };
        totalpayInfo.setIncrSinkProps(sinkExt);
        totalpayInfo.name = tableName;
        return totalpayInfo;
    }

    protected ArrayList<String> getUniqueKey() {
        return Lists.newArrayList(colId, updateTime);
    }

    protected CMeta createUpdateTime() {
        CMeta cm;
        cm = new CMeta();
        cm.setName(updateTime);
        // cm.setPk(true);
        cm.setType(new DataType(Types.TIMESTAMP));
        return cm;
    }

    @NotNull
    protected UpdateMode createIncrMode() {
        ReplaceType updateMode = new ReplaceType();
        //  updateMode.updateKey = Lists.newArrayList(colId, updateTime);
        return updateMode;
    }

    protected abstract ChunjunSinkFactory getSinkFactory();

    protected abstract BasicDataXRdbmsWriter createDataXWriter();

    private DTO createDTO(DTO.EventType eventType, Consumer<Map<String, Object>>... consumer) {
        DTO d = new DTO();
        d.setEventType(eventType);
        d.setTableName(tableName);
        Map<String, Object> after = Maps.newHashMap();
        after.put(colEntityId, "334556");
        after.put(colNum, 5);
        after.put(colId, pk);
        after.put(colCreateTime, 20211113115959l);
        //  after.put(updateTime, "2021-12-17T09:21:20Z");
        after.put(updateTime, "2021-12-17 09:21:20");
        after.put(starTime, "2021-12-18 09:21:20");
        after.put(updateDate, "2021-12-09");
        d.setAfter(after);
        if (eventType != DTO.EventType.ADD) {
            d.setBefore(Maps.newHashMap(after));
            for (Consumer<Map<String, Object>> c : consumer) {
                c.accept(after);
            }
        }
        return d;
    }
}
