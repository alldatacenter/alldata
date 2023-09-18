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

package com.qlangtech.tis.plugins.incr.flink.chunjun.starrocks;

import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.dtstack.chunjun.connector.starrocks.streamload.StarRocksStreamLoadVisitor;
import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.cdc.SourceChannel;
import com.qlangtech.plugins.incr.flink.cdc.source.TestTableRegisterFlinkSourceHandle;
import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.datax.doris.DataXDorisWriter;
import com.qlangtech.tis.plugin.datax.starrocks.DataXStarRocksWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.script.ChunjunSqlType;
import com.qlangtech.tis.plugins.incr.flink.chunjun.starrocks.sink.ChunjunStarRocksSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.starrocks.BaseStarRocksTestCase;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.TISTableEnvironment;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-12 10:29
 **/
public class TestChunjunStarRocksSinkFactory extends TestFlinkSinkExecutor {
    protected static StarRocksSourceFactory dsFactory;

//            new DockerComposeContainer(new File("src/test/resources/compose-doris-test.yml"))
//                    .withExposedService(DORIS_FE_SERVICE, DORIS_FE_PORT)
//                    .withExposedService(DORIS_FE_SERVICE, DORIS_FE_LOAD_PORT)
//                    .withExposedService(DORIS_BE_SERVICE, DORIS_BE_PORT)
//                    .withExposedService(DORIS_BE_SERVICE, DORIS_BE_LOAD_PORT);

    @BeforeClass
    public static void initialize() throws Exception {
        StarRocksStreamLoadVisitor.diableStarRockBeRedirectable();
        BaseStarRocksTestCase.initialize();
        dsFactory = BaseStarRocksTestCase.createSourceFactory();
    }

    @AfterClass
    public static void stop() {
        BaseStarRocksTestCase.stop();
    }


    /**
     * 使用SQL引擎来执行同步
     *
     * @throws Exception
     */
    @Test
    public void testSinkSyncWithSQL() throws Exception {

        // AtomicInteger httpPutCount = getHttpPutCount();
        super.testSinkSync((dataxProcessor, sinkFactory, env, selectedTab) -> {
            /**
             * ==================================================
             */
            TestTableRegisterFlinkSourceHandle tableRegisterHandle = new TotalpayRegisterFlinkSourceHandle(selectedTab);
            tableRegisterHandle.setSinkFuncFactory(sinkFactory);
            tableRegisterHandle.setSourceStreamTableMeta((tab) -> {
                return () -> {
                    return selectedTab.getCols().stream()
                            .map((c) -> new HdfsColMeta(
                                    c.getName(), c.isNullable(), c.isPk(), c.getType())).collect(Collectors.toList());
                };
            });

            List<ReaderSource> sourceFuncts = Lists.newArrayList();
            dataxProcessor.getTabAlias().forEach((key, val) -> {
                Pair<DTOStream, ReaderSource<DTO>> sourceStream = createReaderSource(env, val, false);
                sourceFuncts.add(sourceStream.getRight());
            });

            SourceChannel sourceChannel = new SourceChannel(sourceFuncts);
            sourceChannel.setFocusTabs(Collections.singletonList(selectedTab), dataxProcessor.getTabAlias(), DTOStream::createDispatched);
            tableRegisterHandle.consume(new TargetResName(dataXName), sourceChannel, dataxProcessor);
            /**
             * ===========================================
             */
        });
        // Assert.assertEquals("httpPutCount must be 1", 1, httpPutCount.get());
    }

    // assertResultSetFromStore(resultSet);


    @Override
    protected void assertResultSetFromStore(ResultSet resultSet) throws SQLException {
        super.assertResultSetFromStore(resultSet);
    }

    private static class TotalpayRegisterFlinkSourceHandle extends TestTableRegisterFlinkSourceHandle {
        private final SelectedTab selectedTab;

        public TotalpayRegisterFlinkSourceHandle(SelectedTab tab) {
            super(tab.getName(), Collections.emptyList());
            this.selectedTab = tab;
        }

        @Override
        protected void executeSql(TISTableEnvironment tabEnv) {
            super.executeSql(tabEnv);

            String cols = selectedTab.getCols().stream().map((c) -> c.getName()).collect(Collectors.joining(","));
            //  String cols = colId + "," + starTime;
            // String targetCols = selectedTab.getCols().stream().map((c) -> "cast("+ c.getName()+" AS string)").collect(Collectors.joining(","));
            tabEnv.insert("INSERT INTO " + tableName + "(" + cols + ") SELECT "
                    + cols + " FROM " + tableName + IStreamIncrGenerateStrategy.IStreamTemplateData.KEY_STREAM_SOURCE_TABLE_SUFFIX);
        }

        protected final Boolean shallRegisterSinkTable() {
            return true;
        }

        @Override
        protected String getSinkTypeName() {
            return ChunjunSqlType.getTableSinkTypeName(IEndTypeGetter.EndType.StarRocks);
        }
    }


    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return dsFactory;
    }

    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        ChunjunStarRocksSinkFactory starRocksSinkFactory = new ChunjunStarRocksSinkFactory();
        ChunjunSqlType chunjunSqlType = new ChunjunSqlType();
        starRocksSinkFactory.scriptType = chunjunSqlType;
        return starRocksSinkFactory;
    }

    @Override
    protected BasicDataXRdbmsWriter createDataXWriter() {

        DataXStarRocksWriter dataXWriter = new DataXStarRocksWriter() {
            @Override
            public StarRocksSourceFactory getDataSourceFactory() {
                return Objects.requireNonNull(dsFactory, "dsFactory can not be null");
            }
        };

        dataXWriter.loadProps = DataXDorisWriter.getDftLoadProps();
        return dataXWriter;

    }


//    @BeforeClass
//    public static void initializeDorisDB() throws Exception {
//
//
//        Assert.assertNotNull(environment);
//
//        feServiceHost = environment.getServiceHost(DORIS_FE_SERVICE, DORIS_FE_PORT);
//        int jdbcPort = environment.getServicePort(DORIS_FE_SERVICE, DORIS_FE_PORT);
//        int loadPort = environment.getServicePort(DORIS_FE_SERVICE, DORIS_FE_LOAD_PORT);
//        System.out.println(feServiceHost + ":" + jdbcPort + "_" + loadPort);
//
//        // 客户端会向fe请求到be的地址，然后直接向be发送数据
//        FeRestService.backendRequestStub = () -> {
//            BackendRow backend = new BackendRow();
//            backend.setAlive(true);
//            backend.setIP(environment.getServiceHost(DORIS_BE_SERVICE, DORIS_BE_LOAD_PORT));
//            backend.setHttpPort(String.valueOf(environment.getServicePort(DORIS_BE_SERVICE, DORIS_BE_LOAD_PORT)));
//            return backend;
//        };
//
//
////        String beHost = environment.getServiceHost(DORIS_BE_SERVICE, DORIS_BE_PORT);
////        System.out.println("beHost:"+beHost);
//        Optional<ContainerState> containerByServiceName = environment.getContainerByServiceName(DORIS_BE_SERVICE);
//        Assert.assertTrue(containerByServiceName.isPresent());
//        //  System.out.println(containerByServiceName.get().);
//        final String beContainerName = containerByServiceName.get().getContainerInfo().getName();
//
//        String colName = null;
//        dsFactory = getSourceFactory(feServiceHost, jdbcPort, loadPort);
//        try (Connection conn = dsFactory.getConnection(
//                dsFactory.buidJdbcUrl(null, feServiceHost, null))) {
//
//            try (Statement statement = conn.createStatement()) {
////                System.out.println("beContainerName:" + beContainerName);
////                Thread.sleep(1000000);
//                statement.execute("ALTER SYSTEM ADD BACKEND \"" + StringUtils.substringAfter(beContainerName, "/") + ":" + DORIS_BE_PORT + "\"");
//            }
//            Thread.sleep(10000);
//            try (Statement statement = conn.createStatement()) {
//                try (ResultSet result = statement.executeQuery("SHOW PROC '/backends'")) {
//                    ResultSetMetaData metaData = result.getMetaData();
//                    if (result.next()) {
//                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
//                            colName = metaData.getColumnName(i);
//                            System.out.println(colName + ":" + result.getString(colName) + " ");
//                        }
//                        Assert.assertTrue("be node must be alive", result.getBoolean("Alive"));
//
//                    } else {
//                        Assert.fail("must has backend node");
//                    }
//                }
//                statement.execute("create database if not exists " + dbName);
//                statement.execute( //
//                        "create table if not exists " + dbName + ".test_table(\n" +
//                                "       name varchar(100),\n" +
//                                "       value float\n" +
//                                ")\n" +
//                                "ENGINE=olap\n" +
//                                "UNIQUE KEY(name)\n" +
//                                "DISTRIBUTED BY HASH(name)\n" +
//                                "PROPERTIES(\"replication_num\" = \"1\")");
//                statement.execute("insert into " + dbName + ".test_table values (\"nick\", 1), (\"nick2\", 3)");
//
//                dsFactory.dbName = dbName;
//            }
//            Assert.assertNotNull(conn);
//        }
//    }


}
