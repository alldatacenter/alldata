/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.controller.CatalogControllerTest;
import com.netease.arctic.ams.server.controller.LoginControllerTest;
import com.netease.arctic.ams.server.controller.OptimizerControllerTest;
import com.netease.arctic.ams.server.controller.TableControllerTest;
import com.netease.arctic.ams.server.controller.TerminalControllerTest;
import com.netease.arctic.ams.server.handler.impl.ArcticTableMetastoreHandler;
import com.netease.arctic.ams.server.handler.impl.OptimizeManagerHandler;
import com.netease.arctic.ams.server.maintainer.TestCommandParser;
import com.netease.arctic.ams.server.maintainer.TestGetMaintainerConfig;
import com.netease.arctic.ams.server.maintainer.command.TestAnalyzeCall;
import com.netease.arctic.ams.server.maintainer.command.TestOptimizeCall;
import com.netease.arctic.ams.server.maintainer.command.TestRepairCall;
import com.netease.arctic.ams.server.maintainer.command.TestShowCall;
import com.netease.arctic.ams.server.maintainer.command.TestTableCall;
import com.netease.arctic.ams.server.maintainer.command.TestUseCall;
import com.netease.arctic.ams.server.optimize.OptimizeService;
import com.netease.arctic.ams.server.optimize.SupportHiveTestGroup;
import com.netease.arctic.ams.server.optimize.TableOptimizeItemTest;
import com.netease.arctic.ams.server.optimize.TestExpireFileCleanSupportIceberg;
import com.netease.arctic.ams.server.optimize.TestExpiredFileClean;
import com.netease.arctic.ams.server.optimize.TestIcebergFullOptimizeCommit;
import com.netease.arctic.ams.server.optimize.TestIcebergFullOptimizePlan;
import com.netease.arctic.ams.server.optimize.TestIcebergMinorOptimizeCommit;
import com.netease.arctic.ams.server.optimize.TestIcebergMinorOptimizePlan;
import com.netease.arctic.ams.server.optimize.TestMajorOptimizeCommit;
import com.netease.arctic.ams.server.optimize.TestMajorOptimizePlan;
import com.netease.arctic.ams.server.optimize.TestMinorOptimizeCommit;
import com.netease.arctic.ams.server.optimize.TestMinorOptimizePlan;
import com.netease.arctic.ams.server.optimize.TestOptimizeService;
import com.netease.arctic.ams.server.optimize.TestOrphanFileClean;
import com.netease.arctic.ams.server.optimize.TestOrphanFileCleanSupportIceberg;
import com.netease.arctic.ams.server.service.MetaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.TestArcticTransactionService;
import com.netease.arctic.ams.server.service.TestDDLTracerService;
import com.netease.arctic.ams.server.service.TestFileInfoCacheService;
import com.netease.arctic.ams.server.service.TestOptimizerService;
import com.netease.arctic.ams.server.service.impl.AdaptHiveService;
import com.netease.arctic.ams.server.service.impl.ArcticTransactionService;
import com.netease.arctic.ams.server.service.impl.CatalogMetadataService;
import com.netease.arctic.ams.server.service.impl.ContainerMetaService;
import com.netease.arctic.ams.server.service.impl.DDLTracerService;
import com.netease.arctic.ams.server.service.impl.FileInfoCacheService;
import com.netease.arctic.ams.server.service.impl.JDBCMetaService;
import com.netease.arctic.ams.server.service.impl.OptimizeQueueService;
import com.netease.arctic.ams.server.service.impl.OptimizerService;
import com.netease.arctic.ams.server.service.impl.PlatformFileInfoService;
import com.netease.arctic.ams.server.service.impl.TableBlockerService;
import com.netease.arctic.ams.server.service.impl.TestTableBlockerService;
import com.netease.arctic.ams.server.service.impl.TrashCleanServiceTest;
import com.netease.arctic.ams.server.util.DerbyTestUtil;
import com.netease.arctic.ams.server.utils.CatalogUtil;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import com.netease.arctic.ams.server.utils.SequenceNumberFetcherTest;
import com.netease.arctic.ams.server.utils.ThreadPool;
import com.netease.arctic.ams.server.utils.SequenceNumberFetcherTest;
import com.netease.arctic.ams.server.utils.UnKeyedTableUtilTest;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.HMSMockServer;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.PartitionSpec;
import org.assertj.core.util.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static com.netease.arctic.ams.server.util.DerbyTestUtil.get;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Suite.class)
@Suite.SuiteClasses({
    TestGetMaintainerConfig.class,
    TestCommandParser.class,
    TestAnalyzeCall.class,
    TestOptimizeCall.class,
    TestRepairCall.class,
    TestShowCall.class,
    TestTableCall.class,
    TestUseCall.class,
    CatalogControllerTest.class,
    OptimizerControllerTest.class,
    TableControllerTest.class,
    TerminalControllerTest.class,
    TestDDLTracerService.class,
    LoginControllerTest.class,
    TestExpiredFileClean.class,
    TestMajorOptimizeCommit.class,
    TestMajorOptimizePlan.class,
    TestMinorOptimizeCommit.class,
    TestMinorOptimizePlan.class,
    TestIcebergFullOptimizePlan.class,
    TestIcebergMinorOptimizePlan.class,
    TestIcebergFullOptimizeCommit.class,
    TestIcebergMinorOptimizeCommit.class,
    TestExpireFileCleanSupportIceberg.class,
    TestOrphanFileCleanSupportIceberg.class,
    TestOrphanFileClean.class,
    TestFileInfoCacheService.class,
    TestTableBlockerService.class,
    TrashCleanServiceTest.class,
    SupportHiveTestGroup.class,
    TestArcticTransactionService.class,
    TestOptimizerService.class,
    UnKeyedTableUtilTest.class,
    TestOptimizeService.class,
    SequenceNumberFetcherTest.class,
    TableOptimizeItemTest.class
})
@PrepareForTest({
    CatalogLoader.class,
    JDBCSqlSessionFactoryProvider.class,
    ArcticMetaStore.class,
    ServiceContainer.class,
    CatalogUtil.class,
    MetaService.class,
    ArcticCatalog.class,
    ArcticTable.class,
    PartitionSpec.class,
    FileInfoCacheService.class,
    CatalogMetadataService.class,
    OptimizeManagerHandler.class,
    AdaptHiveService.class,
    PlatformFileInfoService.class,
    HiveTableUtil.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class AmsTestBase {

  private static final File testTableBaseDir = new File("/tmp");
  private static final File testBaseDir = new File("unit_test_base_tmp");
  public static ArcticTableMetastoreHandler amsHandler;
  protected static HMSMockServer hms;
  public static ArcticCatalog catalog;
  public static ArcticCatalog icebergCatalog;
  public static final String AMS_TEST_CATALOG_NAME = "ams_test_catalog";

  public static final String AMS_TEST_DB_NAME = "ams_test_db";
  public static final String CATALOG_CONTROLLER_UNITTEST_NAME = "unit_test";

  public static final String AMS_TEST_ICEBERG_CATALOG_NAME = "ams_test_iceberg_catalog";
  public static final String AMS_TEST_ICEBERG_DB_NAME = "ams_test_iceberg_db";

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void beforeAllTest() throws Exception {
    System.setProperty("HADOOP_USER_NAME", System.getProperty("user.name"));
    FileUtils.deleteQuietly(testBaseDir);
    FileUtils.deleteQuietly(testTableBaseDir);
    tempFolder.create();
    testBaseDir.mkdirs();
    testTableBaseDir.mkdir();

    try {
      DerbyTestUtil.deleteIfExists(DerbyTestUtil.path + "mydb1");
    } catch (IOException e) {
      e.printStackTrace();
    }

    mockStatic(JDBCSqlSessionFactoryProvider.class);
    when(JDBCSqlSessionFactoryProvider.get()).thenAnswer((Answer<SqlSessionFactory>) invocation ->
        get());
    DerbyTestUtil derbyTestUtil = new DerbyTestUtil();
    derbyTestUtil.createTestTable();
    mockStatic(ArcticMetaStore.class);
    when(ArcticMetaStore.getSystemSettingFromYaml()).thenAnswer((Answer<LinkedHashMap<String,Object>>) x ->
            new LinkedHashMap<String, Object>(){{
              put("a", "b");
              put("a", "b");
            }});
    mockStatic(ServiceContainer.class);
    mockStatic(CatalogMetadataService.class);

    //set config
    com.netease.arctic.ams.server.config.Configuration configuration =
        new com.netease.arctic.ams.server.config.Configuration();
    configuration.setString(ArcticMetaStoreConf.DB_TYPE, "derby");
    configuration.setString("arctic.ams.terminal.local.spark.sql.session.timeZone", "UTC");
    ArcticMetaStore.conf = configuration;

    //mock service
    FileInfoCacheService fileInfoCacheService = new FileInfoCacheService();
    when(ServiceContainer.getFileInfoCacheService()).thenReturn(fileInfoCacheService);
    DDLTracerService ddlTracerService = new DDLTracerService();
    when(ServiceContainer.getDdlTracerService()).thenReturn(ddlTracerService);
    CatalogMetadataService catalogMetadataService = new CatalogMetadataService();
    when(ServiceContainer.getCatalogMetadataService()).thenReturn(catalogMetadataService);
    AdaptHiveService adaptHiveService = new AdaptHiveService();
    when(ServiceContainer.getAdaptHiveService()).thenReturn(adaptHiveService);
    TableBlockerService tableBlockerService = new TableBlockerService(configuration);
    when(ServiceContainer.getTableBlockerService()).thenReturn(tableBlockerService);
    JDBCMetaService metaService = new JDBCMetaService();
    when(ServiceContainer.getMetaService()).thenReturn(metaService);
    PlatformFileInfoService platformFileInfoService = new PlatformFileInfoService();
    when(ServiceContainer.getPlatformFileInfoService()).thenReturn(platformFileInfoService);

    //mock handler
    amsHandler = new ArcticTableMetastoreHandler(ServiceContainer.getMetaService());
    when(ServiceContainer.getTableMetastoreHandler()).thenReturn(amsHandler);

    ArcticTransactionService arcticTransactionService = new ArcticTransactionService();
    when(ServiceContainer.getArcticTransactionService()).thenReturn(arcticTransactionService);

    OptimizeQueueService optimizeQueueService = new OptimizeQueueService();
    when(ServiceContainer.getOptimizeQueueService()).thenReturn(optimizeQueueService);
    OptimizerService optimizerService = new OptimizerService();
    when(ServiceContainer.getOptimizerService()).thenReturn(optimizerService);

    OptimizeService optimizeService = new OptimizeService();
    when(ServiceContainer.getOptimizeService()).thenReturn(optimizeService);

    ContainerMetaService containerMetaService = new ContainerMetaService();
    when(ServiceContainer.getContainerMetaService()).thenReturn(containerMetaService);

    //create
    createCatalog();
    createCatalogForCatalogController();
    createIcebergCatalog();
  }

  private static void createCatalogForCatalogController() {
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
            CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
            CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE, "");

    Map<String, String> authConfig = new HashMap<>();
    authConfig.put(
            CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
            CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    authConfig.put(
            CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
            System.getProperty("user.name"));

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put(CatalogMetaProperties.KEY_WAREHOUSE, "/tmp");
    CatalogMeta catalogMeta = new CatalogMeta(CATALOG_CONTROLLER_UNITTEST_NAME, CATALOG_TYPE_HADOOP,
            storageConfig, authConfig, catalogProperties);
    List<CatalogMeta> catalogMetas = Lists.newArrayList(catalogMeta);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMetas);
  }

  private static void createCatalog() throws IOException {
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE, "");

    Map<String, String> authConfig = new HashMap<>();
    authConfig.put(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    authConfig.put(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        System.getProperty("user.name"));

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put(CatalogMetaProperties.KEY_WAREHOUSE, tempFolder.newFolder().getPath());
    CatalogMeta catalogMeta = new CatalogMeta(AMS_TEST_CATALOG_NAME, CATALOG_TYPE_HADOOP,
        storageConfig, authConfig, catalogProperties);
    List<CatalogMeta> catalogMetas = Lists.newArrayList(catalogMeta);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMetas);
    catalog = CatalogLoader.load(amsHandler, AMS_TEST_CATALOG_NAME);
    catalog.createDatabase(AMS_TEST_DB_NAME);
  }

  public static void createIcebergCatalog() throws IOException {
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, MockArcticMetastoreServer.getHadoopSite());
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, MockArcticMetastoreServer.getHadoopSite());

    Map<String, String> authConfig = new HashMap<>();
    authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    authConfig.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        System.getProperty("user.name"));

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put(CatalogProperties.WAREHOUSE_LOCATION, tempFolder.newFolder().getPath());
    catalogProperties.put(CatalogMetaProperties.TABLE_FORMATS, "iceberg");

    CatalogMeta catalogMeta = new CatalogMeta(AMS_TEST_ICEBERG_CATALOG_NAME, CATALOG_TYPE_HADOOP,
        storageConfig, authConfig, catalogProperties);
    List<CatalogMeta> catalogMetas = Lists.newArrayList(catalogMeta);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMetas);
    icebergCatalog = CatalogLoader.load(amsHandler, AMS_TEST_ICEBERG_CATALOG_NAME);
    icebergCatalog.createDatabase(AMS_TEST_ICEBERG_DB_NAME);
  }

  @AfterClass
  public static void afterAllTest() {
    FileUtils.deleteQuietly(testBaseDir);
    FileUtils.deleteQuietly(testTableBaseDir);
    testBaseDir.mkdirs();

    try {
      DerbyTestUtil.deleteIfExists(DerbyTestUtil.path + "mydb1");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
