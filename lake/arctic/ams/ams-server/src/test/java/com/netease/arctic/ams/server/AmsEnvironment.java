package com.netease.arctic.ams.server;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.util.DerbyTestUtil;
import com.netease.arctic.catalog.CatalogTestHelpers;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.local.LocalOptimizer;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AmsEnvironment {
  private static final Logger LOG = LoggerFactory.getLogger(AmsEnvironment.class);
  private LocalOptimizer optimizer;
  private final String rootPath;
  private static final String DEFAULT_ROOT_PATH = "/tmp/arctic_integration";

  public static void main(String[] args) {
    AmsEnvironment amsEnvironment = new AmsEnvironment();
    amsEnvironment.start();
    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    amsEnvironment.stop();
  }

  public AmsEnvironment() {
    this(DEFAULT_ROOT_PATH);
  }

  public AmsEnvironment(String rootPath) {
    this.rootPath = rootPath;
    LOG.info("ams environment root path: " + rootPath);
  }

  public String getAmsUrl() {
    return "thrift://127.0.0.1:" + ArcticMetaStore.conf.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT);
  }

  public void start() {
    clear();
    startAms();
    startLocalOptimizer();
    LOG.info("ams environment started");
  }

  public void stop() {
    stopLocalOptimizer();
    stopAms();
    clear();
    LOG.info("ams environment stopped");
  }
  
  private void clear() {
    try {
      DerbyTestUtil.deleteIfExists(rootPath);
    } catch (IOException e) {
      LOG.warn("delete derby db failed", e);
    }
  }

  private static int randomPort() {
    // create a random port between 14000 - 18000
    int port = new Random().nextInt(4000);
    return port + 14000;
  }


  private void startAms() {
    String path = this.getClass().getClassLoader().getResource("").getPath();
    outputToFile(rootPath + "/conf/config.yaml", getAmsConfig());
    System.setProperty(ArcticMetaStoreConf.ARCTIC_HOME.key(), rootPath);
    System.setProperty("derby.init.sql.dir", path + "../classes/sql/derby/");
    AtomicBoolean amsExit = new AtomicBoolean(false);

    Thread amsRunner = new Thread(() -> {
      int retry = 10;
      try {
        while (true) {
          try {
            LOG.info("start ams");
            System.setProperty(ArcticMetaStoreConf.THRIFT_BIND_PORT.key(), randomPort() + "");
            // when AMS is successfully running, this thread will wait here
            ArcticMetaStore.main(new String[] {});
            break;
          } catch (TTransportException e) {
            if (e.getCause() instanceof BindException) {
              LOG.error("start ams failed", e);
              if (retry-- < 0) {
                throw e;
              } else {
                Thread.sleep(1000);
              }
            } else {
              throw e;
            }
          } catch (Throwable e) {
            throw e;
          }
        }
      } catch (Throwable t) {
        LOG.error("start ams failed", t);
      } finally {
        amsExit.set(true);
      }
    }, "ams-runner");
    amsRunner.start();

    while (true) {
      if (amsExit.get()) {
        LOG.error("ams exit");
        break;
      }
      if (ArcticMetaStore.isStarted()) {
        LOG.info("ams start");
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        LOG.warn("interrupt ams");
        amsRunner.interrupt();
        break;
      }
    }
  }

  public List<TableIdentifier> refreshTables() {
    List<TableIdentifier> tableIdentifiers = ServiceContainer.getOptimizeService().refreshAndListTables();
    ServiceContainer.getOptimizeService()
        .checkOptimizeCheckTasks(ArcticMetaStore.conf.getLong(ArcticMetaStoreConf.OPTIMIZE_CHECK_STATUS_INTERVAL));
    return tableIdentifiers;
  }

  public void syncTableFileCache(TableIdentifier tableIdentifier, String tableType) {
    ServiceContainer.getFileInfoCacheService().syncTableFileInfo(tableIdentifier.buildTableIdentifier(), tableType);
  }

  public void createIcebergHadoopCatalog(String catalogName, String warehouseDir) {
    Map<String, String> properties = Maps.newHashMap();
    createDirIfNotExist(warehouseDir);
    properties.put("warehouse", warehouseDir);
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(catalogName,
        CatalogMetaProperties.CATALOG_TYPE_HADOOP, properties, TableFormat.ICEBERG);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMeta);
  }

  public void createMixedIcebergCatalog(String catalogName, String warehouseDir) {
    Map<String, String> properties = Maps.newHashMap();
    createDirIfNotExist(warehouseDir);
    properties.put("warehouse", warehouseDir);
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(catalogName,
        CatalogMetaProperties.CATALOG_TYPE_AMS, properties, TableFormat.MIXED_ICEBERG);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMeta);
  }

  public void createMixedHiveCatalog(String catalogName, Configuration hiveConfiguration) {
    Map<String, String> properties = Maps.newHashMap();
    CatalogMeta catalogMeta = CatalogTestHelpers.buildHiveCatalogMeta(catalogName,
        properties, hiveConfiguration, TableFormat.MIXED_HIVE);
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMeta);
  }

  private void createDirIfNotExist(String warehouseDir) {
    try {
      Files.createDirectories(Paths.get(warehouseDir));
    } catch (IOException e) {
      LOG.error("failed to create iceberg warehouse dir {}", warehouseDir, e);
      throw new RuntimeException(e);
    }
  }

  private void stopAms() {
    ArcticMetaStore.shutDown();
    LOG.info("ams stop");
  }

  private void startLocalOptimizer() {
    OptimizerConfig config = new OptimizerConfig();
    config.setExecutorParallel(1);
    config.setOptimizerId("1");
    config.setAmsUrl(getAmsUrl());
    config.setQueueId(1);
    config.setHeartBeat(1000);
    optimizer = new LocalOptimizer();
    optimizer.init(config);
    LOG.info("local optimizer start");
  }
  
  private void stopLocalOptimizer() {
    if (optimizer != null) {
      optimizer.release();
    }
    LOG.info("local optimizer stop");
  }
  
  private static void outputToFile(String fileName, String content) {
    try {
      FileUtils.writeStringToFile(new File(fileName), content);
    } catch (IOException e) {
      LOG.error("output to file failed", e);
    }
  }
  
  private String getAmsConfig() {
    return "ams:\n" +
        "  arctic.ams.server-host.prefix: \"127.\"\n" +
        // "  arctic.ams.server-host: 127.0.0.1\n" +
        "  arctic.ams.thrift.port: 1360 # useless in test, System.getProperty(\"arctic.ams.thrift.port\") is used\n" +
        "  arctic.ams.http.port: 1730\n" +
        "  arctic.ams.optimize.check.thread.pool-size: 1\n" +
        "  arctic.ams.optimize.commit.thread.pool-size: 1\n" +
        "  arctic.ams.expire.thread.pool-size: 1\n" +
        "  arctic.ams.orphan.clean.thread.pool-size: 1\n" +
        "  arctic.ams.optimize.check-status.interval: 3000\n" +
        "  arctic.ams.file.sync.thread.pool-size: 1\n" +
        "  arctic.ams.support.hive.sync.thread.pool-size: 1\n" +
        "  # derby config.sh\n" +
        "  arctic.ams.mybatis.ConnectionDriverClassName: org.apache.derby.jdbc.EmbeddedDriver\n" +
        "  arctic.ams.mybatis.ConnectionURL: jdbc:derby:" + rootPath + "/derby;create=true\n" +
        "  arctic.ams.database.type: derby\n" +
        "  # mysql config.sh\n" +
        // "  arctic.ams.mybatis.ConnectionURL: jdbc:mysql://localhost:3306/arctic_opensource_local?useUnicode=true" +
        // "&characterEncoding=UTF8&autoReconnect=true&useAffectedRows=true&useSSL=false\n" +
        // "  arctic.ams.mybatis.ConnectionDriverClassName: com.mysql.jdbc.Driver\n" +
        // "  arctic.ams.mybatis.ConnectionUserName: ndc\n" +
        // "  arctic.ams.mybatis.ConnectionPassword: ndc\n" +
        // "  arctic.ams.database.type: mysql" +
        "\n" +
        "# extension properties for like system\n" +
        "extension_properties:\n" +
        "#test.properties: test\n" +
        "\n" +
        "containers:\n" +
        "  # arctic optimizer container config.sh\n" +
        "  - name: localContainer\n" +
        "    type: local\n" +
        "    properties:\n" +
        "      hadoop_home: /opt/hadoop\n" +
        "\n" +
        "\n" +
        "\n" +
        "optimize_group:\n" +
        "  - name: default\n" +
        "    # container name, should equal with the name that containers config.sh\n" +
        "    scheduling_policy: balanced\n" +
        "    container: localContainer\n" +
        "    properties:\n" +
        "      # unit MB\n" +
        "      memory: 1024\n";
  }

}
