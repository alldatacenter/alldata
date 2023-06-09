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

package com.qlangtech.plugins.incr.flink.chunjun.clickhouse.sink;

import com.google.common.collect.Sets;
import com.qlangtech.plugins.incr.flink.cdc.IResultRows;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.datax.DataXClickhouseWriter;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.ds.clickhouse.ClickHouseDataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.sink.SinkTabPropsExtends;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.InsertType;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.test.TISEasyMock;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.types.RowKind;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.sql.*;
import java.util.*;


/**
 * 测试例子
 * https://github.com/testcontainers/testcontainers-java/blob/master/modules/clickhouse/src/test/java/org/testcontainers/junit/clickhouse/SimpleClickhouseTest.java
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-15 12:12
 **/
@RunWith(Parameterized.class)
public class TestChunjunClickhouseSinkFactory
        implements TISEasyMock {

    //    public void test() {
//        Path path = Paths.get("/tmp/tis-clickhouse-sink");
//        System.out.println(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS));
//    }
    static {
        System.setProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY, "logback-test.xml");
    }

    private final DockerImageName imageName;

    public TestChunjunClickhouseSinkFactory(DockerImageName imageName) {
        this.imageName = imageName;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{ //
                {ClickhouseTestImages.CLICKHOUSE_IMAGE},
                // {ClickhouseTestImages.YANDEX_CLICKHOUSE_IMAGE},
        };
    }

    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testDescriptorsJSONGenerate() {
        ChunjunClickhouseSinkFactory sinkFactory = new ChunjunClickhouseSinkFactory();
        DescriptorsJSON descJson = new DescriptorsJSON(sinkFactory.getDescriptor());

        JsonUtil.assertJSONEqual(ChunjunClickhouseSinkFactory.class, "clickhouse-sink-factory.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    Assert.assertEquals(m, e, a);
                });
    }

    @Test
    public void testCreateSinkFunction() throws Exception {

        try (ClickHouseContainer clickhouse = new ClickHouseContainer(this.imageName)) {
            clickhouse.start();

//            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");
//
//            int resultSetInt = resultSet.getInt(1);
//            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);


            String testDataX = "testDataX";
            String tableName = "totalpayinfo";
            // cols
            String colEntityId = "entity_id";
            String colNum = "num";
            String colId = "id";
            String colCreateTime = "create_time";

            Set<String> colNames = Sets.newHashSet(colEntityId, colNum, colId, colCreateTime);

            DataxProcessor dataxProcessor = mock("dataxProcessor", DataxProcessor.class);


            File ddlDir = folder.newFolder("ddl");

            FileUtils.write(new File(ddlDir, tableName + ".sql")
                    , IOUtils.loadResourceFromClasspath(
                            TestChunjunClickhouseSinkFactory.class, "totalpay-create-ddl.sql"), TisUTF8.get());

            EasyMock.expect(dataxProcessor.getDataxCreateDDLDir(null)).andReturn(ddlDir);

            IDataxReader dataxReader = mock("dataxReader", IDataxReader.class);

            List<ISelectedTab> selectedTabs = Lists.newArrayList();
            SelectedTab totalpayinfo = mock(tableName, SelectedTab.class);
            SinkTabPropsExtends sinkExt = new SinkTabPropsExtends();
            sinkExt.tabName = tableName;
            sinkExt.uniqueKey = Collections.singletonList(colId);
            // ReplaceType replaceMode = new ReplaceType();
            // replaceMode.updateKey = Collections.singletonList(colId);
            InsertType insertType = new InsertType();
            sinkExt.incrMode = insertType;
            EasyMock.expect(totalpayinfo.getIncrSinkProps()).andReturn(sinkExt);
            EasyMock.expect(totalpayinfo.getName()).andReturn(tableName).anyTimes();
            List<CMeta> cols = Lists.newArrayList();
            CMeta cm = new CMeta();
            cm.setName(colEntityId);
            cm.setType(new DataType(Types.VARCHAR, "VARCHAR", 6));
            cols.add(cm);

            cm = new CMeta();
            cm.setName(colNum);
            cm.setType(new DataType(Types.INTEGER));
            cols.add(cm);

            cm = new CMeta();
            cm.setName(colId);
            cm.setType(new DataType(Types.VARCHAR, "VARCHAR", 32));
            cm.setPk(true);
            cols.add(cm);

            cm = new CMeta();
            cm.setName(colCreateTime);
            cm.setType(new DataType(Types.BIGINT));
            cols.add(cm);

            EasyMock.expect(totalpayinfo.getCols()).andReturn(cols).anyTimes();
            selectedTabs.add(totalpayinfo);
            EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs).times(1);

            EasyMock.expect(dataxProcessor.getReader(null)).andReturn(dataxReader).anyTimes();


            //mock("dataXWriter", DataXClickhouseWriter.class);
            //  dataXWriter.initWriterTable(tableName, Collections.singletonList("jdbc:clickhouse://192.168.28.201:8123/tis"));


            final ClickHouseDataSourceFactory sourceFactory = getCKDSFactory(clickhouse);

            DataXClickhouseWriter dataXWriter = new DataXClickhouseWriter() {
                @Override
                public ClickHouseDataSourceFactory getDataSourceFactory() {
                    // return super.getDataSourceFactory();
                    return sourceFactory;
                }
            };
            dataXWriter.dataXName = testDataX;
            dataXWriter.autoCreateTable = true;

            //EasyMock.expect(dataXWriter.getDataSourceFactory()).andReturn(sourceFactory);

            EasyMock.expect(dataxProcessor.getWriter(null)).andReturn(dataXWriter);
            DataxProcessor.processorGetter = (name) -> {
                return dataxProcessor;
            };

            Map<String, TableAlias> aliasMap = new HashMap<>();
            TableAlias tab = new TableAlias(tableName);
            aliasMap.put(tableName, tab);
            EasyMock.expect(dataxProcessor.getTabAlias()).andReturn(new TableAliasMapper(aliasMap));

            this.replay();

            ChunjunClickhouseSinkFactory clickHouseSinkFactory = new ChunjunClickhouseSinkFactory();
            clickHouseSinkFactory.batchSize = 100;
            clickHouseSinkFactory.parallelism = 1;
            clickHouseSinkFactory.semantic = "at-least-once";

            Map<TableAlias, TabSinkFunc<RowData>>
                    sinkFuncs = clickHouseSinkFactory.createSinkFunction(dataxProcessor);
            Assert.assertTrue(sinkFuncs.size() > 0);

            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            env.setParallelism(1);

            GenericRowData d = new GenericRowData(RowKind.INSERT, 4);
            // d.setTableName(tableName);
            //  Map<String, Object> after = Maps.newHashMap();
            String colIdVal = "334556";
            d.setField(2, StringData.fromString(colIdVal));
            d.setField(0, StringData.fromString("123dsf124325253dsf123"));
            d.setField(1, 5);
            d.setField(3, 20211113115959l);
            //after.put(colNum, );
            // after.put(colId, "123dsf124325253dsf123");
            // after.put(colCreateTime, "20211113115959");
            // d.setAfter(after);

            Assert.assertEquals(1, sinkFuncs.size());
            DTOStream rowStream = DTOStream.createRowData();
            // rowStream.addStream(env.fromElements(new RowData[]{d}));
            for (Map.Entry<TableAlias, TabSinkFunc<RowData>> entry : sinkFuncs.entrySet()) {

                ReaderSource<RowData> readerSource = ReaderSource.createRowDataSource("testStreamSource", totalpayinfo
                        , env.fromElements(new RowData[]{d}));

                readerSource.getSourceStream(env, new Tab2OutputTag<>(Collections.singletonMap(entry.getKey(), rowStream)));

                // .addSink(entry.getValue().).name("clickhouse");
                entry.getValue().add2Sink(rowStream);
                break;
            }

            env.execute("testJob");

            Thread.sleep(5000);


            DBConfig dbConfig = sourceFactory.getDbConfig();
            String[] jdbcUrls = new String[2];
            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
                jdbcUrls[0] = jdbcUrl;
                jdbcUrls[1] = dbName;
            });

            try {
                try (DataSourceMeta.JDBCConnection conn = sourceFactory.getConnection(jdbcUrls[0])) {
                    Statement statement = conn.createStatement();
                    //+ " where id='" + colIdVal + "'"
                    ResultSet resultSet = statement.executeQuery("select * from " + jdbcUrls[1] + "." + tableName);
                    if (resultSet.next()) {
                        IResultRows.printRow(resultSet);
//                        StringBuffer rowDesc = new StringBuffer();
//                        for (String col : colNames) {
//                            Object obj = resultSet.getObject(col);
//                            rowDesc.append(col).append("=").append(obj).append("[").append((obj != null) ? obj.getClass().getSimpleName() : "").append("]").append(" , ");
//                        }
//                        System.out.println("test_output==>" + rowDesc.toString());
                        Assert.assertEquals(colIdVal, resultSet.getString(colId));
                    } else {
                        Assert.fail("have not find row with id=" + colIdVal);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            this.verifyAll();

        }
    }


    private ClickHouseDataSourceFactory getCKDSFactory(ClickHouseContainer clickhouse) {
        final ClickHouseDataSourceFactory sourceFactory = new ClickHouseDataSourceFactory();
        sourceFactory.userName = clickhouse.getUsername();
        sourceFactory.dbName = StringUtils.substringAfterLast(clickhouse.getJdbcUrl(), "/");
        sourceFactory.password = clickhouse.getPassword();
        sourceFactory.port = clickhouse.getMappedPort(ClickHouseContainer.HTTP_PORT);
        sourceFactory.nodeDesc = clickhouse.getHost();
        return sourceFactory;
    }
}
