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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hudi.HudiWriter;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataXPluginMeta;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.common.*;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl.NonePartitionKeyGenerator;
import com.qlangtech.tis.plugin.datax.hudi.partition.OffPartition;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.IPluginContext;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.Startables;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-24 10:20
 **/

public class TestDataXHudiWriter {

    // private static final String targetTableName ="";
    private static final Logger logger = LoggerFactory.getLogger(TestDataXHudiWriter.class);
    private static final Integer taskId = 999;
    final String jdbcUrl = "jdbcurl";

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    // private static File dataDir;
    @Rule
    public TemporaryFolder caseFolder = new TemporaryFolder();


    //protected static final int DEFAULT_PARALLELISM = 4;
    protected static final MySqlContainer MYSQL_CONTAINER
            = MySqlContainer.createMysqlContainer("docker/server-gtids/my.cnf"
            , "docker/column_type_test.sql");
//            (MySqlContainer)
//                    new MySqlContainer()
//                            .withConfigurationOverride("docker/server-gtids/my.cnf")
//                            .withSetupSQL("docker/column_type_test.sql")
//                            .withDatabaseName("flink-test")
//                            .withUsername("flinkuser")
//                            .withPassword("flinkpw")
//                            .withLogConsumer(new TISLoggerConsumer(logger));

    @BeforeClass
    public static void start() throws Exception {
        // System.setProperty(Config.KEY_LOG_DIR, com.qlangtech.tis.manage.common.Config.getLogDir().getAbsolutePath());
        CenterResource.setNotFetchFromCenterRepository();
//        dataDir = folder.newFolder("data");
//        Config.setDataDir(dataDir.getAbsolutePath());
        logger.info("Starting containers...");
        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
        logger.info("Containers are started.");


        System.setProperty(DataxUtils.EXEC_TIMESTAMP, String.valueOf(HudiWriter.timestamp));
    }


    // @Ignore
    @Test
    public void testRealDumpFullTypesTable() throws Exception {
        // System.setProperty(DataxUtils.EXEC_TIMESTAMP)
        System.out.println(Config.getAssembleHost());
        // System.setProperty(IRemoteTaskTrigger.KEY_DELTA_STREM_DEBUG, "true");
        String tableFullTypes = "full_types";
        TargetResName dbName = new TargetResName("hudi-data-test-mysql-ds");

        Optional<Context> context = Optional.of(new DefaultContext());
        BasicDataSourceFactory dsFactory = (BasicDataSourceFactory)
                MYSQL_CONTAINER.createMySqlDataSourceFactory(dbName);// MySqlContainer.createMySqlDataSourceFactory(dbName, MYSQL_CONTAINER);
        TIS.getDataSourceFactoryPluginStore(PostedDSProp.parse(dbName.getName()))
                .setPlugins(IPluginContext.namedContext(dbName.getName()), context
                        , Collections.singletonList(new Descriptor.ParseDescribable(dsFactory)));

        //   DataxMySQLReader
        Descriptor mySQLDesc = TIS.get().getDescriptor("DataxMySQLReader");

        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("dbName", dbName.getName());
        formData.addProp("splitPk", String.valueOf(true));
        formData.addProp("fetchSize", String.valueOf(2000));
        formData.addProp("template", "template");

        Descriptor.ParseDescribable<Describable> parseDescribable
                = mySQLDesc.newInstance(HdfsFileSystemFactoryTestUtils.testDataXName.getName(), formData);

        BasicDataXRdbmsReader dataxReader = parseDescribable.getInstance();
        dataxReader.template = IOUtils.loadResourceFromClasspath(dataxReader.getClass(), "mysql-reader-tpl.json");
        dataxReader.dataXName = HdfsFileSystemFactoryTestUtils.testDataXName.getName();
        Assert.assertNotNull("dataxReader can not be null", dataxReader);
        List<ColumnMetaData> tabMeta = dataxReader.getTableMetadata(EntityName.parse(tableFullTypes));
        Assert.assertTrue(CollectionUtils.isNotEmpty(tabMeta));

        List<String> fullTypesCols = tabMeta.stream().map((col) -> col.getName())
                .filter((col) -> !"time_c".equals(col) && !"big_decimal_c".equals(col))
                .collect(Collectors.toList());
        Optional<ColumnMetaData> pk = tabMeta.stream().filter((col) -> col.isPk()).findFirst();
        Assert.assertTrue("pk must present", pk.isPresent());

        // save the plugin
        KeyedPluginStore<DataxReader> pluginStore = DataxReader.getPluginStore(null, HdfsFileSystemFactoryTestUtils.testDataXName.getName());
        pluginStore.setPlugins(null, Optional.empty(), Collections.singletonList(new Descriptor.ParseDescribable(dataxReader)));

        HudiSelectedTab hudiTab = new HudiSelectedTab();
        hudiTab.name = tableFullTypes;
        // hudiTab. = new OffPartition();
        //FieldValBasedPartition pt = new FieldValBasedPartition();
        // SimpleKeyGenerator simpleKeyGenerator = new SimpleKeyGenerator();
//        simpleKeyGenerator.partitionPathField = "tiny_c";
//        simpleKeyGenerator.recordField = pk.get().getKey();
//        simpleKeyGenerator.partition = new OffPartition();
        // pt.setKeyGenerator(simpleKeyGenerator);
        NonePartitionKeyGenerator nonePartition = new NonePartitionKeyGenerator();
        nonePartition.recordFields = Lists.newArrayList(pk.get().getKey());
        nonePartition.partition = new OffPartition();
        // pt.partitionPathField = "tiny_c";
        hudiTab.keyGenerator = nonePartition;// .partition = pt;

        hudiTab.setCols(fullTypesCols);
        // hudiTab.recordField = Lists.newArrayList(pk.get().getKey());
        hudiTab.sourceOrderingField = pk.get().getKey();
        List<HudiSelectedTab> tabs = Collections.singletonList(hudiTab);
        dataxReader.selectedTabs = tabs;
//        List<Descriptor.ParseDescribable<HudiSelectedTab>> dlist = Lists.newArrayList();
//        dlist.add(new Descriptor.ParseDescribable<HudiSelectedTab>(tabs));
//        pluginStore.setPlugins(null, Optional.empty(), null);
        tabs = dataxReader.getSelectedTabs();

        IExecChainContext execContext = EasyMock.createMock("execContext", IExecChainContext.class);
        // EasyMock.expect(execContext.getBoolean(HudiDumpPostTask.KEY_DELTA_STREM_DEBUG)).andReturn(true);

        EasyMock.expect(execContext.getTaskId()).andReturn(taskId).anyTimes();
        EasyMock.expect(execContext.getIndexName()).andReturn(HdfsFileSystemFactoryTestUtils.testDataXName.getName()).anyTimes();
        EasyMock.expect(execContext.getPartitionTimestampWithMillis()).andReturn((HudiWriter.timestamp)).anyTimes();
        DataxProcessor processor = EasyMock.mock("dataxProcessor", DataxProcessor.class);
        DataxProcessor.processorGetter = (name) -> processor;
        File dataxCfgDir = caseFolder.newFolder("dataxCfgDir");
        //  EasyMock.expect(processor.getDataxCfgDir(null)).andReturn(dataxCfgDir);

        DataXCfgGenerator.GenerateCfgs genCfgs = new DataXCfgGenerator.GenerateCfgs(dataxCfgDir);
        genCfgs.setGenTime(1);
        genCfgs.setGroupedChildTask(
                Collections.singletonMap(tableFullTypes
                        , Lists.newArrayList(new DataXCfgGenerator.DBDataXChildTask(jdbcUrl, "dbId", tableFullTypes + "_0"))));
        genCfgs.write2GenFile(dataxCfgDir);


        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);
        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg).anyTimes();


        DataXHudiWriter dataxWriter = HudiTest.createDataXHudiWriter(Optional.empty());
        // HudiTest hudiTest = createDataXWriter();
        // save the writer
        DataxWriter.getPluginStore(null, HdfsFileSystemFactoryTestUtils.testDataXName.getName())
                .setPlugins(null, Optional.empty(), Collections.singletonList(new Descriptor.ParseDescribable(dataxWriter)));

        EasyMock.replay(processor, dataxGlobalCfg, execContext);
        IRemoteTaskTrigger preExecuteTask = dataxWriter.createPreExecuteTask(execContext, hudiTab);
        Assert.assertNotNull(preExecuteTask);
//        DataxWriter dataXWriter
//            , IDataxProcessor.TableMap tableMap
//            , IDataxProcessor processor, IDataxReader dataXReader
        preExecuteTask.run();

        // IDataxProcessor processor, DataxReader dataXReader, IDataxWriter dataxWriter, String dataXName

        final String dataXReaderCfg = ReaderTemplate.generateReaderCfg(
                processor, dataxReader, HdfsFileSystemFactoryTestUtils.testDataXName.getName());
        IReaderPluginMeta readerPluginMeta = new IReaderPluginMeta() {
            @Override
            public String getReaderJsonCfgContent() {
                return dataXReaderCfg;
            }

            @Override
            public IDataXPluginMeta.DataXMeta getDataxMeta() {
                return dataxReader.getDataxMeta();
            }
        };

        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap(hudiTab);


        IWriterPluginMeta writerMeta = new IWriterPluginMeta() {
            @Override
            public IDataXPluginMeta.DataXMeta getDataxMeta() {
                return dataxWriter.getDataxMeta();
            }

            @Override
            public Configuration getWriterJsonCfg() {
                try {
                    return Configuration.from(WriterTemplate.generateWriterCfg(dataxWriter, tableMap, processor));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };


        IRemoteTaskTrigger preExec = dataxWriter.createPreExecuteTask(execContext, hudiTab);
        Assert.assertNotNull("preExec can not be null", preExec);
        preExec.run();

        WriterTemplate.realExecuteDump( readerPluginMeta, writerMeta);

        // MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(123));

        JobCommon.setMDC(123);
        HudiDumpPostTask postTask = (HudiDumpPostTask) dataxWriter.createPostTask(execContext, hudiTab, genCfgs);
        Assert.assertNotNull("postTask can not be null", postTask);
        postTask.run();

        EasyMock.verify(processor, dataxGlobalCfg, execContext);
    }


    @Ignore
    @Test
    public void testRealDump() throws Exception {


//        MDC.put(JobCommon.KEY_COLLECTION
//                , HdfsFileSystemFactoryTestUtils.testDataXName.getName());
//        MDC.put(JobCommon.KEY_TASK_ID, "123");

        JobCommon.setMDC(123, HdfsFileSystemFactoryTestUtils.testDataXName.getName());
        HudiTest houseTest = HudiTest.createDataXWriter();


        // houseTest.writer.autoCreateTable = true;

        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);

        File dataXCfgDir = folder.newFolder();
        File createDDLDir = folder.newFolder();
        File createDDLFile = null;
        try {
            createDDLFile = new File(createDDLDir, HudiWriter.targetTableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX);
            FileUtils.write(createDDLFile
                    , com.qlangtech.tis.extension.impl.IOUtils.loadResourceFromClasspath(DataXHudiWriter.class
                            , "create_ddl_customer_order_relation.sql"), TisUTF8.get());

            DataXCfgGenerator.GenerateCfgs genCfg = new DataXCfgGenerator.GenerateCfgs(dataXCfgDir);
            genCfg.setGenTime(HudiWriter.timestamp);
            genCfg.setGroupedChildTask(Collections.singletonMap(WriterTemplate.TAB_customer_order_relation
                    , Lists.newArrayList(new DataXCfgGenerator.DBDataXChildTask(jdbcUrl, "dbId", WriterTemplate.TAB_customer_order_relation + "_0"))));
            genCfg.write2GenFile(dataXCfgDir);

            //   EasyMock.expect(dataXProcessor.getDataxCfgDir(null)).andReturn(dataXCfgDir);
            // EasyMock.expect(dataXProcessor.getDataxCreateDDLDir(null)).andReturn(createDDLDir);
            DataxWriter.dataxWriterGetter = (dataXName) -> {
                return houseTest.writer;
            };
            DataxProcessor.processorGetter = (dataXName) -> {
                Assert.assertEquals(HdfsFileSystemFactoryTestUtils.testDataXName.getName(), dataXName);
                return dataXProcessor;
            };


            IExecChainContext execContext = EasyMock.mock("execContext", IExecChainContext.class);
            EasyMock.expect(execContext.getPartitionTimestampWithMillis()).andReturn((HudiWriter.timestamp)).anyTimes();


            EasyMock.replay(dataXProcessor, execContext);

            IRemoteTaskTrigger preExecuteTask = houseTest.writer.createPreExecuteTask(execContext, houseTest.tab);
            Assert.assertNotNull("postTask can not be null", preExecuteTask);
            preExecuteTask.run();

            WriterTemplate.realExecuteDump(
                    WriterJson.path(HudiTest.hudi_datax_writer_assert_without_optional)
                            .addCfgSetter((cfg) -> {
                                //  cfg.set(cfgPathParameter + "." + DataxUtils.EXEC_TIMESTAMP, timestamp);
                                return cfg;
                            })
                    , houseTest.writer);


            // DataXHudiWriter hudiWriter = new DataXHudiWriter();
//            hudiWriter.dataXName = HdfsFileSystemFactoryTestUtils.testDataXName.getName();
//            hudiWriter.createPostTask(execContext, tab);
            //   MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(123));
            JobCommon.setMDC(123);
            HudiDumpPostTask postTask = (HudiDumpPostTask) houseTest.writer.createPostTask(execContext, houseTest.tab, genCfg);
            Assert.assertNotNull("postTask can not be null", postTask);

            postTask.run();

            // IHiveConnGetter hiveConnMeta = houseTest.writer.getHiveConnMeta();
//            try (IHiveMetaStore metaStoreClient = hiveConnMeta.createMetaStoreClient()) {
//                Assert.assertNotNull(metaStoreClient);
//                HiveTable table = metaStoreClient.getTable(hiveConnMeta.getDbName(), WriterTemplate.TAB_customer_order_relation);
//                Assert.assertNotNull(WriterTemplate.TAB_customer_order_relation + " can not be null", table);
//            }

            EasyMock.verify(dataXProcessor, execContext);
        } finally {
            //  FileUtils.deleteQuietly(createDDLFile);
        }
    }

    @Test
    public void testConfigGenerate() throws Exception {

        HudiTest forTest = HudiTest.createDataXWriter();
        WriterTemplate.valiateCfgGenerate("hudi-datax-writer-assert.json", forTest.writer, forTest.tableMap);
    }

//    @Test
//    public void testFlinkSqlTableDDLCreate() throws Exception {
//        FileSystemFactory fsFactory = EasyMock.createMock("fsFactory", FileSystemFactory.class);
//
//        ITISFileSystem fs = EasyMock.createMock("fileSystem", ITISFileSystem.class);
//        //  fs.getRootDir()
//        String child = "default/customer_order_relation";
//        String dataDir = "hudi";
//        IPath rootPath = new HdfsPath(HdfsFileSystemFactoryTestUtils.DEFAULT_HDFS_ADDRESS + "/user/admin");
//        IPath tabPath = new HdfsPath(rootPath, child);
//        IPath hudiDataPath = new HdfsPath(tabPath, dataDir);
//        EasyMock.expect(fs.getPath(rootPath, child)).andReturn(tabPath);
//        EasyMock.expect(fs.getPath(tabPath, dataDir)).andReturn(hudiDataPath);
//        EasyMock.expect(fs.getRootDir()).andReturn(rootPath);
//        EasyMock.expect(fsFactory.getFileSystem()).andReturn(fs);
//        HudiTest forTest = createDataXWriter(Optional.of(fsFactory));
//        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);
//        File dataXCfg = folder.newFile();
//        FileUtils.writeStringToFile(dataXCfg
//                , "{job:{content:[{\"writer\":"
//                        + IOUtils.loadResourceFromClasspath(
//                        this.getClass(), hudi_datax_writer_assert_without_optional)
//                        + "}]}}"
//                , TisUTF8.get());
//
//        List<File> dataXFiles = Lists.newArrayList(dataXCfg);
//
//        EasyMock.expect(dataXProcessor.getDataxCfgFileNames(null)).andReturn(dataXFiles);
//
//        DataxProcessor.processorGetter = (dataXName) -> {
//            Assert.assertEquals(HdfsFileSystemFactoryTestUtils.testDataXName.getName(), dataXName);
//            return dataXProcessor;
//        };
//        EasyMock.replay(dataXProcessor, fsFactory, fs);
////        IStreamTableCreator.IStreamTableMeta
////                streamTableMeta = forTest.writer.getStreamTableMeta(HudiWriter.targetTableName);
//
////        Assert.assertNotNull("streamTableMeta can not be null", streamTableMeta);
////        streamTableMeta.getColsMeta();
//
//        // System.out.println(streamTableMeta.createFlinkTableDDL());
//
////        DataXHudiWriter.HudiStreamTemplateData tplData
////                = (DataXHudiWriter.HudiStreamTemplateData) forTest.writer.decorateMergeData(
////                new TestStreamTemplateData(HdfsFileSystemFactoryTestUtils.testDataXName, HudiWriter.targetTableName));
////
////
////        StringBuffer createTabDdl = tplData.getSinkFlinkTableDDL(HudiWriter.targetTableName);
//
//
////        Assert.assertNotNull(createTabDdl);
////
////        System.out.println(createTabDdl);
//
//
//        EasyMock.verify(dataXProcessor, fsFactory, fs);
//    }

    //    @NotNull
//    private static FileSystemFactory createFileSystemFactory() {
//        HdfsFileSystemFactory hdfsFactory = new HdfsFileSystemFactory();
//        hdfsFactory.name = FS_NAME;
//        hdfsFactory.rootDir = "/user/admin";
//        hdfsFactory.hdfsAddress = "hdfs://daily-cdh201";
//        hdfsFactory.hdfsSiteContent
//                = IOUtils.loadResourceFromClasspath(TestDataXHudiWriter.class, "hdfsSiteContent.xml");
//        hdfsFactory.userHostname = true;
//        return hdfsFactory;
//    }

//    private static IHiveConnGetter createHiveConnGetter() {
//        Descriptor hiveConnGetter = TIS.get().getDescriptor("DefaultHiveConnGetter");
//        Assert.assertNotNull(hiveConnGetter);
//
//        // 使用hudi的docker运行环境 https://hudi.apache.org/docs/docker_demo#step-3-sync-with-hive
//        Descriptor.FormData formData = new Descriptor.FormData();
//        formData.addProp("name", "testhiveConn");
//        formData.addProp("hiveAddress", "hiveserver:10000");
//
//        formData.addProp("useUserToken", "true");
//        formData.addProp("dbName", "default");
//        formData.addProp("password", "hive");
//        formData.addProp("userName", "hive");
//        formData.addProp("metaStoreUrls","thrift://hiveserver:9083");
//
//
//        Descriptor.ParseDescribable<IHiveConnGetter> parseDescribable
//                = hiveConnGetter.newInstance(HdfsFileSystemFactoryTestUtils.testDataXName.getName(), formData);
//        Assert.assertNotNull(parseDescribable.instance);
//
//        Assert.assertNotNull(parseDescribable.instance);
//        return parseDescribable.instance;
//    }
}
