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
package com.qlangtech.tis.runtime.module.action;

import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.biz.dal.dao.*;
import com.qlangtech.tis.manage.biz.dal.pojo.*;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.manage.spring.TISDataSourceFactory;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.action.UploadJarAction.ConfigContentGetter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统初始化
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月17日
 */
public class SysInitializeAction   //extends BasicModule
{

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(SysInitializeAction.class);

  public static final int DEPARTMENT_DEFAULT_ID = 2;
  private static final int DEPARTMENT_ROOT_ID = 1;
  public static final int TEMPLATE_APPLICATION_DEFAULT_ID = 1;

  public static final String ADMIN_ID = "9999";
  public static final String ADMIN_NAME = "admin";

  public static final String APP_NAME_TEMPLATE = "search4template";
  private static final Pattern PATTERN_ZK_ADDRESS = Pattern.compile("([^/]+)(/.+)$");


  public static boolean isSysInitialized() {
    final File sysInitializedToken = getSysInitializedTokenFile();
    return sysInitializedToken.exists();
  }

  static File getSysInitializedTokenFile() {
    return new File(Config.getDataDir(), "system_initialized_token");
  }

  private static void touchSysInitializedToken() throws Exception {
    final File sysInitializedToken = getSysInitializedTokenFile();
    //return sysInitializedToken.exists();
    FileUtils.touch(sysInitializedToken);
  }

  /**
   * 执行系统初始化，数据库和zk节点初始化
   *
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      throw new IllegalArgumentException("args.length must big than 0");
    }
    File tisConsoleSqlFile = new File(args[0]);
    if (!tisConsoleSqlFile.exists()) {
      throw new IllegalStateException("tisConsoleSqlFile:" + tisConsoleSqlFile.getAbsolutePath() + " is not exist");
    }
    final String dbType = args[1];
    Config.TisDbConfig dbCfg = Config.getDbCfg();
    TISDataSourceFactory.SystemDBInit dsProcess = null;

    try {
      dsProcess = TISDataSourceFactory.createDataSource(dbType, dbCfg, false, true);
      TISDataSourceFactory.systemDBInitThreadLocal.set(dsProcess);
      try (Connection conn = dsProcess.getDS().getConnection()) {
        try (Statement statement = conn.createStatement()) {

          // 初始化TIS数据库
          logger.info("init '" + dbCfg.dbname + "' db and initialize the tables");
          boolean containTisConsole = dsProcess.dbTisConsoleExist(dbCfg, statement);
          List<String> executeSqls = Lists.newArrayList();
          if (!containTisConsole) {
            boolean execSuccess = false;
            try {
              dsProcess.createSysDB(dbCfg, statement);

              for (String sql : convert2BatchSql(dsProcess, tisConsoleSqlFile)) {
                try {
                  if (dsProcess.shallSkip(sql)) {
                    continue;
                  }
                  executeSqls.add(sql);
                  statement.execute(sql);
                  //  statement.addBatch(sql);
                } catch (SQLException e) {
                  logger.error(sql, e);
                  throw e;
                }
              }
              // statement.executeBatch();
              // FileUtils.forceDelete(tisConsoleSqlFile);
              execSuccess = true;
            } catch (SQLException e) {
              throw new RuntimeException(executeSqls.stream().collect(Collectors.joining("\n")), e);
            } finally {
              if (!execSuccess) {
                dsProcess.dropDB(dbCfg, statement);
              }
            }
          }
        }
      }

      Objects.requireNonNull(dsProcess, "dataSource can not be null");
      systemDataInitialize();
    } finally {
      try {
        dsProcess.close();
      } catch (Throwable e) {
      }
    }
  }


  public static void systemDataInitialize() throws Exception {
    SysInitializeAction initAction = new SysInitializeAction();
    //ClassPathXmlApplicationContext tis.application.context.xml src/main/resources/tis.application.mockable.context.xml
    ApplicationContext appContext = new ClassPathXmlApplicationContext(
      "classpath:/tis.application.context.xml", "classpath:/tis.application.mockable.context.xml");
    appContext.getAutowireCapableBeanFactory().autowireBeanProperties(
      initAction, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
    initAction.doInit();
  }

  /**
   * 将sql脚本文件转成jdbc batchsql 允许的sql格式
   *
   * @param tisConsoleSqlFile
   * @return
   * @throws Exception
   */
  private static List<String> convert2BatchSql(TISDataSourceFactory.SystemDBInit dataSource, File tisConsoleSqlFile) throws Exception {
    LineIterator lineIt = FileUtils.lineIterator(tisConsoleSqlFile, TisUTF8.getName());
    List<String> batchs = Lists.newArrayList();
    StringBuffer result = new StringBuffer();
    String line = null;
    while (lineIt.hasNext()) {
      line = StringUtils.trimToEmpty(lineIt.nextLine());
      if (StringUtils.startsWith(line, "/*") //
        || StringUtils.startsWith(line, "--") //
        || StringUtils.startsWith(line, "#") //
        || StringUtils.startsWith(line, "//")) {
        continue;
      }

      result.append(line).append(" ");

      if (StringUtils.endsWith(line, ";")) {
        batchs.add(StringUtils.trimToEmpty(dataSource.processSql(result)));
        result = new StringBuffer();
      }

    }
    // String convertSql = result.toString();
    File targetFile = new File(tisConsoleSqlFile.getParent(), tisConsoleSqlFile.getName() + ".convert");
    FileUtils.write(targetFile
      , batchs.stream().collect(Collectors.joining("\n")), TisUTF8.get(), false);
    return batchs;
  }

  public void doInit() throws Exception {

    // final File sysInitializedToken = new File(Config.getDataDir(), "system_initialized_token");
    if (isSysInitialized()) {
      throw new IllegalStateException("tis has initialized:" + getSysInitializedTokenFile().getAbsolutePath());
    }

//    logger.info("needZkInit:{}", needZkInit);
//
//    if (needZkInit && !initializeZkPath(Config.getZKHost())) {
//      // 初始化ZK失败
//      logger.warn("ZkInit falid,zkAddress:{}", Config.getZKHost());
//      return;
//    }

    UsrDptRelationCriteria c = new UsrDptRelationCriteria();
    c.createCriteria().andUsrIdEqualTo(ADMIN_ID);
    if (this.getUsrDptRelationDAO().countByExample(c) > 0) {
      touchSysInitializedToken();
      //this.addActionMessage("系统已经完成初始化，请继续使用");
      throw new IllegalStateException("system has initialized successful,shall not initialize again");
      //return;
    }
    // 添加一个系统管理员
    this.getUsrDptRelationDAO().addAdminUser();

    this.initializeDepartment();

    this.initializeAppAndSchema();
    touchSysInitializedToken();
  }

  private IUsrDptRelationDAO getUsrDptRelationDAO() {
    return this.getDaoContext().getUsrDptRelationDAO();
  }

  public void initializeAppAndSchema() throws IOException {
    this.getApplicationDAO().deleteByPrimaryKey(TEMPLATE_APPLICATION_DEFAULT_ID);
    SnapshotCriteria snapshotQuery = new SnapshotCriteria();
    snapshotQuery.createCriteria().andAppidEqualTo(TEMPLATE_APPLICATION_DEFAULT_ID);
    this.getSnapshotDAO().deleteByExample(snapshotQuery);

    ServerGroupCriteria serverGroupQuery = new ServerGroupCriteria();
    serverGroupQuery.createCriteria().andAppIdEqualTo(TEMPLATE_APPLICATION_DEFAULT_ID);
    this.getServerGroupDAO().deleteByExample(serverGroupQuery);

    // 添加初始化模板配置
    Application app = new Application();
    // app.setAppId(TEMPLATE_APPLICATION_DEFAULT_ID);
    app.setProjectName(APP_NAME_TEMPLATE);
    app.setDptId(DEPARTMENT_DEFAULT_ID);
    app.setDptName("default");
    app.setIsDeleted("N");
    app.setManager(ADMIN_NAME);
    app.setUpdateTime(new Date());
    app.setCreateTime(new Date());
    app.setRecept(ADMIN_NAME);

    Integer newAppId = this.getApplicationDAO().insertSelective(app);
    if (newAppId != TEMPLATE_APPLICATION_DEFAULT_ID) {
      throw new IllegalStateException("newAppId:" + newAppId + " must equal with " + TEMPLATE_APPLICATION_DEFAULT_ID);
    }

    app.setAppId(TEMPLATE_APPLICATION_DEFAULT_ID);
    this.initializeSchemaConfig(app);
  }

//  // 初始化ZK内容
//  public boolean initializeZkPath(String zkHost) {
//
//    Matcher matcher = PATTERN_ZK_ADDRESS.matcher(zkHost);
//    if (!matcher.matches()) {
//      throw new IllegalStateException("zk address " + zkHost + " is not match " + PATTERN_ZK_ADDRESS);
//    }
//
//    final String zkServer = matcher.group(1);
//    String zkSubDir = StringUtils.trimToEmpty(matcher.group(2));
//    logger.info("zkServer:{},zkSubDir:{}", zkServer, zkSubDir);
//
//    if (StringUtils.endsWith(zkSubDir, "/")) {
//      zkSubDir = StringUtils.substring(zkSubDir, 0, zkSubDir.length() - 1);
//    }
//
//    ZooKeeper zk = null;
//    StringBuffer buildLog = new StringBuffer();
//    String createPath = null;
//    List<String> createPaths = Lists.newArrayList();
//    try {
////      final Watcher watcher = new Watcher() {
////        @Override
////        public void process(WatchedEvent event) {
////          logger.info(event.getType() + "," + event.getState() + "," + event.getPath());
////        }
////      };
//      zk = this.createZK(zkServer); //new ZooKeeper(zkServer, 50000, watcher);
//      zk.getChildren("/", false);
//      buildLog.append("create zkServer ").append(zkServer);
//      createPath = zkSubDir + "/tis";
//
//      ITISCoordinator coordinator = getCoordinator(zk);
//      logger.info("guaranteeExist:{}", createPath);
//      createPaths.add(createPath);
//      ZkUtils.guaranteeExist(coordinator, createPath);
//      buildLog.append(",path1:").append(createPath);
//      createPath = zkSubDir + "/tis-lock/dumpindex";
//      createPaths.add(createPath);
//      ZkUtils.guaranteeExist(coordinator, createPath);
//      buildLog.append(",path2:").append(createPath);
////      createPath = zkSubDir + "/configs/" + CoreAction.DEFAULT_SOLR_CONFIG;
////      createPaths.add(createPath);
////      ZkUtils.guaranteeExist(coordinator, createPath);
////      buildLog.append(",path3:").append(createPath);
//      logger.info(buildLog.toString());
//
//
//    } catch (Throwable e) {
//      throw new IllegalStateException("zk address:" + zkServer + " can not connect Zookeeper server", e);
//    } finally {
//      try {
//        zk.close();
//      } catch (Throwable e) {
//
//      }
//    }
//
//    try {
//      Thread.sleep(10000);
//    } catch (InterruptedException e) {
//
//    }
//
//    try {
//      zk = this.createZK(zkServer);
//      for (String p : createPaths) {
//        if (zk.exists(p, false) == null) {
//          throw new TisException("create path:" + p + " must be exist");
//        }
//      }
//    } catch (TisException e) {
//      throw e;
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    } finally {
//      try {
//        zk.close();
//      } catch (InterruptedException e) {
//      }
//    }
//
//
//    return true;
//  }

//  private ZooKeeper createZK(String zkServer) throws IOException {
//    final Watcher watcher = new Watcher() {
//      @Override
//      public void process(WatchedEvent event) {
//        logger.info(event.getType() + "," + event.getState() + "," + event.getPath());
//      }
//    };
//    return new ZooKeeper(zkServer, 50000, watcher);
//  }

//  private static ITISCoordinator getCoordinator(ZooKeeper zooKeeper) throws Exception {
//    ITISCoordinator coordinator = null;
//    coordinator = new AdapterTisCoordinator() {
//      @Override
//      public List<String> getChildren(String zkPath, Watcher watcher, boolean b) {
//        try {
//          return zooKeeper.getChildren(zkPath, watcher);
//        } catch (Exception e) {
//          throw new RuntimeException(e);
//        }
//      }
//
//      @Override
//      public boolean exists(String path, boolean watch) {
//        try {
//          return zooKeeper.exists(path, watch) != null;
//        } catch (Exception e) {
//          throw new RuntimeException(path, e);
//        }
//      }
//
//      @Override
//      public void create(String path, byte[] data, boolean persistent, boolean sequential) {
//
//        CreateMode createMode = null;
//        if (persistent) {
//          createMode = sequential ? CreateMode.PERSISTENT_SEQUENTIAL : CreateMode.PERSISTENT;
//        } else {
//          createMode = sequential ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.EPHEMERAL;
//        }
//        try {
//          zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
//        } catch (Exception e) {
//          throw new RuntimeException("path:" + path, e);
//        }
//      }
//
//      @Override
//      public byte[] getData(String zkPath, Watcher o, Stat stat, boolean b) {
//        try {
//          return zooKeeper.getData(zkPath, o, stat);
//        } catch (Exception e) {
//          throw new RuntimeException(e);
//        }
//      }
//    };
//    return coordinator;
//  }
//
//  /**
//   * 百岁add
//   * copy from org.apache.solr.cloud.ZkController
//   * Create the zknodes necessary for a cluster to operate
//   *
//   * @param zkClient a SolrZkClient
//   * @throws KeeperException      if there is a Zookeeper error
//   * @throws InterruptedException on interrupt
//   */
//  public static void createClusterZkNodes(SolrZkClient zkClient)
//    throws KeeperException, InterruptedException, IOException {
//    ZkCmdExecutor cmdExecutor = new ZkCmdExecutor(zkClient.getZkClientTimeout());
//    cmdExecutor.ensureExists(ZkStateReader.LIVE_NODES_ZKNODE, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.COLLECTIONS_ZKNODE, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.ALIASES, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_AUTOSCALING_EVENTS_PATH, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_AUTOSCALING_TRIGGER_STATE_PATH, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_AUTOSCALING_NODE_ADDED_PATH, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_AUTOSCALING_NODE_LOST_PATH, zkClient);
//    byte[] emptyJson = "{}".getBytes(StandardCharsets.UTF_8);
//    cmdExecutor.ensureExists(ZkStateReader.CLUSTER_STATE, emptyJson, CreateMode.PERSISTENT, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_SECURITY_CONF_PATH, emptyJson, CreateMode.PERSISTENT, zkClient);
//    cmdExecutor.ensureExists(ZkStateReader.SOLR_AUTOSCALING_CONF_PATH, emptyJson, CreateMode.PERSISTENT, zkClient);
//    // bootstrapDefaultConfigSet(zkClient);
//  }


  void initializeSchemaConfig(Application app) throws IOException {
    Snapshot snap = new Snapshot();
    snap.setCreateTime(new Date());
    snap.setCreateUserId(9999l);
    snap.setCreateUserName("admin");
    snap.setUpdateTime(new Date());

    snap.setAppId(app.getAppId());
    try (InputStream schemainput = this.getClass().getResourceAsStream("/solrtpl/schema.xml.tpl")) {
      ConfigContentGetter schema = new ConfigContentGetter(ConfigFileReader.FILE_SCHEMA,
        IOUtils.toString(schemainput, BasicModule.getEncode()));
      snap = UploadJarAction.processFormItem(this.getDaoContext(), schema, snap);
    }
    try (InputStream solrconfigInput = this.getClass().getResourceAsStream("/solrtpl/solrconfig.xml.tpl")) {
      ConfigContentGetter solrConfig = new ConfigContentGetter(ConfigFileReader.FILE_SOLR,
        IOUtils.toString(solrconfigInput, BasicModule.getEncode()));
      snap = UploadJarAction.processFormItem(this.getDaoContext(), solrConfig, snap);
    }
    snap.setPreSnId(-1);
    Integer snapshotId = this.getSnapshotDAO().insertSelective(snap);

    GroupAction.createGroup(RunEnvironment.getSysRuntime(), AddAppAction.FIRST_GROUP_INDEX, app.getAppId(),
      snapshotId, this.getServerGroupDAO());
  }


  void initializeDepartment() {

    this.getDepartmentDAO().deleteByPrimaryKey(DEPARTMENT_DEFAULT_ID);
    this.getDepartmentDAO().deleteByPrimaryKey(DEPARTMENT_ROOT_ID);

    // 初始化部门
    Department dpt = new Department();
    //dpt.setDptId(1);
    dpt.setLeaf(false);
    dpt.setGmtCreate(new Date());
    dpt.setGmtModified(new Date());
    dpt.setName("tis");
    dpt.setFullName("/tis");
    dpt.setParentId(-1);
    Integer dptId = this.getDepartmentDAO().insertSelective(dpt);

    dpt = new Department();
    // dpt.setDptId(DEPARTMENT_DEFAULT_ID);
    dpt.setLeaf(true);
    dpt.setGmtCreate(new Date());
    dpt.setGmtModified(new Date());
    dpt.setName("default");
    dpt.setFullName("/tis/default");
    dpt.setParentId(dptId);
    dptId = this.getDepartmentDAO().insertSelective(dpt);
    if (dptId != DEPARTMENT_DEFAULT_ID) {
      throw new IllegalStateException("dptId:" + dptId + " must equal with:" + DEPARTMENT_DEFAULT_ID);
    }
  }

  public IApplicationDAO getApplicationDAO() {
    return getDaoContext().getApplicationDAO();
  }

  public IServerGroupDAO getServerGroupDAO() {
    return getDaoContext().getServerGroupDAO();
  }

  public ISnapshotDAO getSnapshotDAO() {
    return getDaoContext().getSnapshotDAO();
  }

  public IUploadResourceDAO getUploadResourceDAO() {
    return getDaoContext().getUploadResourceDAO();
  }

  public IDepartmentDAO getDepartmentDAO() {
    return getDaoContext().getDepartmentDAO();
  }

  private RunContextGetter daoContextGetter;

  private RunContext getDaoContext() {
    return daoContextGetter.get();
  }

  @Autowired
  public final void setRunContextGetter(RunContextGetter daoContextGetter) {
    this.daoContextGetter = daoContextGetter;
  }
}
