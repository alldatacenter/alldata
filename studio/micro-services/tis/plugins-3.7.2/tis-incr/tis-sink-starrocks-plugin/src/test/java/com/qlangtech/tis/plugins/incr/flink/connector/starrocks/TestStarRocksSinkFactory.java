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

package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;

import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.IResultRows;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.datax.BasicDorisStarRocksWriter;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.starrocks.DataXStarRocksWriter;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.TabSinkFunc;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.test.TISEasyMock;
import com.starrocks.connector.flink.table.sink.StarRocksSinkSemantic;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-12 09:54
 **/
public class TestStarRocksSinkFactory extends BaseStarRocksTestCase implements TISEasyMock {
    private static final Logger log = LoggerFactory.getLogger(TestStarRocksSinkFactory.class);
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

//    @Test
//    public void testGetConfigOption() {
//        String desc = StarRocksSinkFactory.desc("sinkSemantic");
//        Assert.assertNotNull(desc);
//    }
//
//    @Test
//    public void testDescriptorsJSONGenerate() {
//        StarRocksSinkFactory sinkFactory = new StarRocksSinkFactory();
//        DescriptorsJSON descJson = new DescriptorsJSON(sinkFactory.getDescriptor());
//
//        JsonUtil.assertJSONEqual(StarRocksSinkFactory.class, "starrocks-sink-factory.json"
//                , descJson.getDescriptorsJSON(), (m, e, a) -> {
//                    Assert.assertEquals(m, e, a);
//                });
//
//    }

    @Test
    public void testStartRocksWrite() throws Exception {

        /**
         CREATE TABLE `totalpayinfo` (
         `id` varchar(32) NULL COMMENT "",
         `entity_id` varchar(10) NULL COMMENT "",
         `num` int(11) NULL COMMENT "",
         `create_time` bigint(20) NULL COMMENT "",
         `update_time` DATETIME   NULL,
         `update_date` DATE       NULL,
         `start_time`  DATETIME   NULL
         ) ENGINE=OLAP
         UNIQUE KEY(`id`)
         DISTRIBUTED BY HASH(`id`) BUCKETS 10
         PROPERTIES (
         "replication_num" = "1",
         "in_memory" = "false",
         "storage_format" = "DEFAULT"
         );
         * */

        String dataXName = "testDataX";

        String tableName = "totalpayinfo";
        String colEntityId = "entity_id";
        String colNum = "num";
        String colId = "id";
        String colCreateTime = "create_time";
        String updateTime = "update_time";
        String updateDate = "update_date";
        String starTime = "start_time";

        DataxProcessor dataxProcessor = mock("dataxProcessor", DataxProcessor.class);
        String tabSql = tableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX;
        File ddlDir = folder.newFolder("ddl");
        DataxProcessor.processorGetter = (name) -> {
            return dataxProcessor;
        };
        EasyMock.expect(dataxProcessor.getDataxCreateDDLDir(null)).andReturn(ddlDir);


        IDataxReader dataxReader = mock("dataxReader", IDataxReader.class);
        List<ISelectedTab> selectedTabs = Lists.newArrayList();
        SelectedTab totalpayinfo = mock(tableName, SelectedTab.class);
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

        cm = new CMeta();
        cm.setName(updateTime);
        cm.setType(new DataType(Types.TIMESTAMP));
        cols.add(cm);

        cm = new CMeta();
        cm.setName(updateDate);
        cm.setType(new DataType(Types.DATE));
        cols.add(cm);

        cm = new CMeta();
        cm.setName(starTime);
        cm.setType(new DataType(Types.TIMESTAMP));
        cols.add(cm);

        EasyMock.expect(totalpayinfo.getCols()).andReturn(cols).anyTimes();


        selectedTabs.add(totalpayinfo);
        EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs);

        EasyMock.expect(dataxProcessor.getReader(null)).andReturn(dataxReader);

        StarRocksSourceFactory sourceFactory = createSourceFactory();

        DataXStarRocksWriter dataXWriter = new DataXStarRocksWriter() {
            @Override
            public Separator getSeparator() {
                return new BasicDorisStarRocksWriter.Separator() {
                    @Override
                    public String getColumnSeparator() {
                        return COL_SEPARATOR_DEFAULT;
                    }

                    @Override
                    public String getRowDelimiter() {
                        return ROW_DELIMITER_DEFAULT;
                    }
                };
            }

            @Override
            public StarRocksSourceFactory getDataSourceFactory() {
                return sourceFactory;
            }
        };

        dataXWriter.dataXName = dataXName;
        dataXWriter.autoCreateTable = true;

        DataxWriter.dataxWriterGetter = (xName) -> {
            Assert.assertEquals(dataXName, xName);
            return dataXWriter;
        };

        // EasyMock.expect(dataXWriter.getDataSourceFactory()).andReturn(sourceFactory);

        //   dataXWriter.initWriterTable(tableName, Collections.singletonList("jdbc:mysql://192.168.28.201:9030/tis"));

        EasyMock.expect(dataxProcessor.getWriter(null)).andReturn(dataXWriter);

        StarRocksSinkFactory sinkFactory = new StarRocksSinkFactory();
//        sinkFactory.columnSeparator = "x01";
//        sinkFactory.rowDelimiter = "x02";
        sinkFactory.sinkSemantic = StarRocksSinkSemantic.AT_LEAST_ONCE.getName();
        sinkFactory.sinkBatchFlushInterval = 2000l;
        sinkFactory.sinkMaxRetries = 0l;
        // sinkFactory.sinkBatchMaxRows=1l;

        System.out.println("sinkFactory.sinkBatchFlushInterval:" + sinkFactory.sinkBatchFlushInterval);

        Map<String, TableAlias> aliasMap = new HashMap<>();
        TableAlias tab = new TableAlias(tableName);
        aliasMap.put(tableName, tab);
        EasyMock.expect(dataxProcessor.getTabAlias()).andReturn(new TableAliasMapper(aliasMap));

        this.replay();

        CreateTableSqlBuilder.CreateDDL createDDL = dataXWriter.generateCreateDDL(new IDataxProcessor.TableMap(totalpayinfo));
        Assert.assertNotNull("createDDL can not be empty", createDDL);
        log.info("create table ddl:\n{}", createDDL);
        FileUtils.write(new File(ddlDir, tabSql), createDDL.getDDLScript(), TisUTF8.get());

        Map<TableAlias, TabSinkFunc<RowData>> sinkFunction = sinkFactory.createSinkFunction(dataxProcessor);
        String pkVal = "88888888887";
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DTO d = new DTO();
        d.setEventType(DTO.EventType.ADD);
        d.setTableName(tableName);
        Map<String, Object> after = Maps.newHashMap();
        after.put(colEntityId, "334556");
        after.put(colNum, "5");
        after.put(colId, pkVal);
        after.put(colCreateTime, "20211113115959");
        after.put(updateTime, "2021-12-17T09:21:20Z");
        after.put(starTime, "2021-12-18 09:21:20");
        after.put(updateDate, "2021-12-9");
        d.setAfter(after);
        Assert.assertEquals(1, sinkFunction.size());
        for (Map.Entry<TableAlias, TabSinkFunc<RowData>> entry : sinkFunction.entrySet()) {
            DTOStream sourceStream = DTOStream.createDispatched(tableName);
            ReaderSource<DTO> readerSource = ReaderSource.createDTOSource("testStreamSource",env.fromElements(new DTO[]{d}));

            readerSource.getSourceStream(env, new Tab2OutputTag<>(Collections.singletonMap(entry.getKey(), sourceStream)));

            entry.getValue().add2Sink(sourceStream);

            // env.fromElements(new DTO[]{d}).addSink(entry.getValue());
            break;
        }

        env.execute("testJob");
        Thread.sleep(10000);

        sourceFactory.visitFirstConnection((conn) -> {
            // boolean findVal = false;
            try (Statement statement = conn.getConnection().createStatement()) {

                //+ " where " + colId + "='" + pkVal + "'"

                try (ResultSet resultSet
                             = statement.executeQuery(
                        createDDL.getSelectAllScript())) {
                    if (resultSet.next()) {
                        IResultRows.printRow(resultSet);
                        String actualPkVal = resultSet.getString(colId);
                        Assert.assertEquals(pkVal, actualPkVal);
                        //  findVal = true;
                        System.out.println("have find a record of " + colId + ":" + actualPkVal);
                    } else {
                        Assert.fail("must find starrock record with colId:" + pkVal);
                    }
                }
            }

        });

        this.verifyAll();
    }


}
