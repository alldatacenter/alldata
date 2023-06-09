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
package com.qlangtech.tis.offline.module.manager.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.db.parser.DBConfigSuit;
import com.qlangtech.tis.git.GitUtils;
import com.qlangtech.tis.git.GitUtils.GitBranchInfo;
import com.qlangtech.tis.git.GitUtils.GitUser;
import com.qlangtech.tis.git.GitUtils.JoinRule;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.offline.module.action.OfflineDatasourceAction;
import com.qlangtech.tis.offline.pojo.TISDb;
import com.qlangtech.tis.offline.pojo.WorkflowPojo;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.util.IPluginContext;
import com.qlangtech.tis.workflow.dao.IWorkflowDAOFacade;
import com.qlangtech.tis.workflow.pojo.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * ds和wf中涉及到db的处理
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年7月25日
 */
public class OfflineManager {

  private IWorkflowDAOFacade workflowDAOFacade;

  public static DataxReader getDBDataxReader(IPluginContext pluginContext, String dbName) {
    KeyedPluginStore<DataxReader> pluginStore = DataxReader.getPluginStore(pluginContext, true, dbName);
    return pluginStore.getPlugin();
  }

  public void setComDfireTisWorkflowDAOFacade(IWorkflowDAOFacade comDfireTisWorkflowDAOFacade) {
    this.workflowDAOFacade = comDfireTisWorkflowDAOFacade;
  }

  /**
   * 取得可以用的数据流
   *
   * @return 工作流列表
   */
  public List<WorkFlow> getUsableWorkflow() {
    WorkFlowCriteria query = new WorkFlowCriteria();
    query.createCriteria();
    query.setOrderByClause("id desc");
    return this.workflowDAOFacade.getWorkFlowDAO().selectByExample(query, 1, 100);
  }

  /**
   * 通过DS名称取得 对应DataSource的DataXReader的DescriptorDisplayName
   *
   * @param dsName
   * @return
   */
  public DBDataXReaderDescName getDBDataXReaderDescName(String dsName) {
    DataSourceFactory dbPlugin = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(dsName), DbScope.DETAILED));
    DataSourceFactory.BaseDataSourceFactoryDescriptor descriptor
      = (DataSourceFactory.BaseDataSourceFactoryDescriptor) dbPlugin.getDescriptor();
    Optional<String> defaultDataXReaderDescName = descriptor.getDefaultDataXReaderDescName();
    return new DBDataXReaderDescName(defaultDataXReaderDescName, descriptor);
  }


  public interface IConnProcessor {

    public void vist(Connection conn) throws SQLException;
  }

//  /**
//   * @param table
//   * @param action
//   * @param context
//   * @param updateMode
//   * @param idempotent 可多次操作table添加幂等
//   * @return
//   * @throws Exception
//   */
//  public ProcessedTable addDatasourceTable(TISTable table, BasicModule action
//    , IPluginContext pluginContext, Context context, boolean updateMode, boolean idempotent) throws Exception {
//    final String tableName = table.getTableName();
//    // 检查db是否存在
//    final DatasourceDbCriteria dbCriteria = new DatasourceDbCriteria();
//    dbCriteria.createCriteria().andIdEqualTo(table.getDbId());
//    int sameDbCount = workflowDAOFacade.getDatasourceDbDAO().countByExample(dbCriteria);
//    if (sameDbCount < 1) {
//      action.addErrorMessage(context, "找不到这个数据库");
//      return null;
//    }
//    final int dbId = table.getDbId();
//   // DatasourceTableCriteria tableCriteria = new DatasourceTableCriteria();
//    int sameTableCount = 0;
//    if (updateMode) {
//      // 检查表是否存在，如不存在则为异常状态
////      tableCriteria.createCriteria().andIdEqualTo(table.getTabId()).andNameEqualTo(table.getTableName());
////      int findTable = workflowDAOFacade.getDatasourceTableDAO().countByExample(tableCriteria);
////      if (findTable < 1) {
////        throw new IllegalStateException("tabid:" + table.getTabId() + ",tabName:" + table.getTableName() + " is not exist in db");
////      }
//    } else {
//      // 检查表是否存在，如存在则退出
////      tableCriteria.createCriteria().andDbIdEqualTo(dbId).andNameEqualTo(tableName);//.andTableLogicNameEqualTo();
////      sameTableCount = workflowDAOFacade.getDatasourceTableDAO().countByExample(tableCriteria);
////      if (!idempotent && sameTableCount > 0) {
////        action.addErrorMessage(context, "该数据库下已经有了相同逻辑名的表:" + tableName);
////        return null;
////      }
//    }
//    // 更新DB的最新操作时间
//    DatasourceDb db = new DatasourceDb();
//    // db.setId(dbId);
//    db.setOpTime(new Date());
//    int dbUpdateRows = workflowDAOFacade.getDatasourceDbDAO().updateByExampleSelective(db, dbCriteria);
//    if (dbUpdateRows < 1) {
//      throw new IllegalStateException("db update faild");
//    }
//    // 添加表db
//    DatasourceTable dsTable = null;
//    Integer tableId;
//    if (updateMode) {
//      if ((tableId = table.getTabId()) == null) {
//        throw new IllegalStateException("update process tabId can not be null");
//      }
//      tableCriteria = new DatasourceTableCriteria();
//      // .andNameEqualTo(table.getTableName());
//      tableCriteria.createCriteria().andIdEqualTo(table.getTabId());
//      dsTable = new DatasourceTable();
//      dsTable.setOpTime(new Date());
//      workflowDAOFacade.getDatasourceTableDAO().updateByExampleSelective(dsTable, tableCriteria);
//      dsTable.setId(tableId);
//    } else if (idempotent && sameTableCount > 0) {
//      tableCriteria = new DatasourceTableCriteria();
//      tableCriteria.createCriteria().andDbIdEqualTo(dbId).andNameEqualTo(tableName);
//      for (DatasourceTable tab : workflowDAOFacade.getDatasourceTableDAO().selectByExample(tableCriteria)) {
//        dsTable = tab;
//      }
//    } else {
//      // sameTableCount < 0
//      dsTable = new DatasourceTable();
//      dsTable.setName(tableName);
////      dsTable.setTableLogicName(tableName);
//      dsTable.setDbId(dbId);
//      // dsTable.setGitTag(tableName);
//      // 标示是否有同步到线上去
//      // dsTable.setSyncOnline(new Byte("0"));
//      dsTable.setCreateTime(new Date());
//      dsTable.setOpTime(new Date());
//      tableId = workflowDAOFacade.getDatasourceTableDAO().insertSelective(dsTable);
//      dsTable.setId(tableId);
//    }
//    Objects.requireNonNull(dsTable, "dsTable can not be null");
//    db = workflowDAOFacade.getDatasourceDbDAO().loadFromWriteDB(dbId);
//    action.addActionMessage(context, "数据库表'" + tableName + "'" + (updateMode ? "更新" : "添加") + "成功");
//    table.setSelectSql(null);
//    table.setReflectCols(null);
//    table.setTabId(dsTable.getId());
//    action.setBizResult(context, table);
//    // return dsTable;
//    DataSourceFactoryPluginStore dbPlugin = TIS.getDataBasePluginStore(new PostedDSProp(db.getName()));
//    return new ProcessedTable(dbPlugin.saveTable(tableName), db, dsTable);
//  }

  public static class ProcessedTable {
    private final TableReflect tabReflect;
    private final DatasourceTable tabMeta;
    private final DatasourceDb db;

    public ProcessedTable(TableReflect tabReflect, DatasourceDb db, DatasourceTable tabMeta) {
      if (tabReflect == null) {
        throw new IllegalStateException("tabReflect  can not be null");
      }
      if (tabMeta == null) {
        throw new IllegalStateException("tabMeta  can not be null");
      }
      if (db == null) {
        throw new IllegalArgumentException("param db can not be null");
      }
      this.tabReflect = tabReflect;
      this.tabMeta = tabMeta;
      this.db = db;
    }

    public String getDBName() {
      return this.db.getName();
    }

    public String getName() {
      return tabMeta.getName();
    }

    public Integer getDbId() {
      return this.tabMeta.getDbId();
    }

    public Integer getId() {
      return this.tabMeta.getId();
    }

    public String getExtraSql() {
      return SqlTaskNodeMeta.processBigContent(tabReflect.getSql());
    }
  }

  /**
   * 更新或者添加门面数据库配置信息
   *
   * @param dbid
   * @param db
   * @param action
   * @param context
   */
  public void updateFacadeDBConfig(Integer dbid, TISDb db, BasicModule action, Context context) throws Exception {
    throw new UnsupportedOperationException();
//    if (dbid == null) {
//      throw new IllegalArgumentException("dbid can not be null");
//    }
//    DatasourceDb ds = workflowDAOFacade.getDatasourceDbDAO().loadFromWriteDB(dbid);
//    if (ds == null) {
//      throw new IllegalStateException("dbid:" + dbid + " relevant datasourceDB can not be null");
//    }
//    List<String> children = GitUtils.$().listDbConfigPath(ds.getName());
//    boolean isAdd = !children.contains(GitUtils.DB_CONFIG_META_NAME + DbScope.FACADE.getDBType());
//    if (StringUtils.isEmpty(db.getPassword())) {
//      if (isAdd) {
//        throw new IllegalStateException("db password can not be empty in add process");
//      } else {
//        // 更新流程
//        DBConfig dbConfig = GitUtils.$().getDbLinkMetaData(ds.getName(), DbScope.FACADE);
//        db.setPassword(dbConfig.getPassword());
//      }
//    }
//    db.setFacade(true);
//    if (!this.testDbConnection(db, action, context).valid) {
//      return;
//    }
//    String path = GitUtils.$().getDBConfigPath(ds.getName(), DbScope.FACADE);
//    GitUtils.$().processDBConfig(db, path, "edit db" + db.getDbName(), isAdd, true);
  }

  /**
   * 修改数据库配置 date: 8:33 PM 6/15/2017
   */
  public void editDatasourceDb(TISDb db, BasicModule action, Context context) throws Exception {
    // 看看db里有没有这条数据
    String dbName = db.getDbName();
    if (StringUtils.isBlank(db.getDbId())) {
      throw new IllegalArgumentException("dbid can not be null");
    }
    DatasourceDb d = workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(// .minSelectByExample(criteria);
      Integer.parseInt(db.getDbId()));
    if (d == null) {
      action.addErrorMessage(context, "db中找不到这个数据库");
      return;
    }
    GitUtils.$().updateDatabase(db, "edit db " + db.getDbName());
    action.addActionMessage(context, "数据库修改成功");
    action.setBizResult(context, db.getDbId());
  }

  // public void editDatasourceTable(TISTable table, BasicModule action, IPluginContext pluginContext, Context context) throws Exception {
//    String dbName = table.getDbName();
//    String tableLogicName = table.getTableName();
//    // 检查db是否存在
//    DatasourceDbCriteria dbCriteria = new DatasourceDbCriteria();
//    dbCriteria.createCriteria().andNameEqualTo(dbName);
//    List<DatasourceDb> dbList = workflowDAOFacade.getDatasourceDbDAO().minSelectByExample(dbCriteria);
//    if (CollectionUtils.isEmpty(dbList)) {
//      action.addErrorMessage(context, "找不到这个数据库");
//      return;
//    }
//    int dbId = dbList.get(0).getId();
//    // 检查表是否存在
//    DatasourceTableCriteria tableCriteria = new DatasourceTableCriteria();
//    tableCriteria.createCriteria().andDbIdEqualTo(dbId).andNameEqualTo(tableLogicName);
//    List<DatasourceTable> tableList = workflowDAOFacade.getDatasourceTableDAO().minSelectByExample(tableCriteria);
//    if (CollectionUtils.isEmpty(tableList)) {
//      action.addErrorMessage(context, "该数据库下没有表" + tableLogicName);
//      return;
//    }
//    int tableId = tableList.get(0).getId();
//    DataSourceFactoryPluginStore dbPlugin = TIS.getDataBasePluginStore(new PostedDSProp(dbName));
//    dbPlugin.saveTable(tableLogicName);
//    // update git
//    // String path = dbName + "/" + tableLogicName;
//    // GitUtils.$().createTableDaily(table, "edit table " + table.getTableLogicName());
//    OperationLog operationLog = new OperationLog();
//    operationLog.setUsrName(action.getLoginUserName());
//    operationLog.setUsrId(action.getUserId());
//    operationLog.setOpType("editDatasourceTable");
//    action.addActionMessage(context, "数据库表修改成功");
//    action.setBizResult(context, tableId);
  // }

  /**
   * description: 获取所有的工作流数据库 date: 2:30 PM 4/28/2017
   */
  public List<Option> getUsableDbNames() {
    DatasourceDbCriteria criteria = new DatasourceDbCriteria();
    criteria.createCriteria();
    List<DatasourceDb> dbList = workflowDAOFacade.getDatasourceDbDAO().selectByExample(criteria);
    List<Option> dbNameList = new LinkedList<>();
    for (DatasourceDb datasourceDb : dbList) {
      dbNameList.add(new Option(datasourceDb.getName(), String.valueOf(datasourceDb.getId())));
    }
    return dbNameList;
  }

  /**
   * description: 获取数据源表 date: 6:21 PM 5/18/2017
   */
//  public List<DatasourceTable> getDatasourceTables() {
//    DatasourceTableCriteria criteria = new DatasourceTableCriteria();
//    criteria.createCriteria();
//    List<DatasourceTable> tables = workflowDAOFacade.getDatasourceTableDAO().selectByExample(criteria);
//    DatasourceDbCriteria dbCriteria = new DatasourceDbCriteria();
//    dbCriteria.createCriteria();
//    List<DatasourceDb> dbs = workflowDAOFacade.getDatasourceDbDAO().selectByExample(dbCriteria);
//    Map<Integer, DatasourceDb> dbMap = new HashMap<>();
//    for (DatasourceDb datasourceDb : dbs) {
//      dbMap.put(datasourceDb.getId(), datasourceDb);
//    }
//    // 把所有表名统计一遍
//    Map<String, Integer> tableNameCntMap = new HashMap<>();
//    for (DatasourceTable datasourceTable : tables) {
//      String name = datasourceTable.getName();
//      if (tableNameCntMap.containsKey(name)) {
//        tableNameCntMap.put(name, tableNameCntMap.get(name) + 1);
//      } else {
//        tableNameCntMap.put(name, 1);
//      }
//    }
//    for (DatasourceTable datasourceTable : tables) {
//      String name = datasourceTable.getName();
//      if (tableNameCntMap.get(name) > 1) {
//        datasourceTable.setName(dbMap.get(datasourceTable.getDbId()).getName() + "/" + datasourceTable.getName());
//      }
//    }
//    Collections.sort(tables);
//    return tables;
//  }
  public void editWorkflow(WorkflowPojo pojo, BasicModule action, Context context) throws Exception {
    String name = pojo.getName();
    WorkFlowCriteria criteria = new WorkFlowCriteria();
    criteria.createCriteria().andNameEqualTo(name);
    List<WorkFlow> workflowList = workflowDAOFacade.getWorkFlowDAO().selectByExample(criteria);
    // 1、检测是否存在
    if (CollectionUtils.isEmpty(workflowList)) {
      action.addErrorMessage(context, "没有名字为" + name + "的工作流");
      return;
    }
    WorkFlow workFlow = workflowList.get(0);
    if (workFlow.getInChange().intValue() == 0) {
      action.addErrorMessage(context, "工作流不在变更中");
      return;
    }
    // 2、检测xml是否正确
    JoinRule task = pojo.getTask();
    if (task == null || StringUtils.isBlank(task.getContent())) {
      action.addErrorMessage(context, "脚本内容不能为空");
      return;
    }
    if (!isXmlValid(task)) {
      action.addErrorMessage(context, "XML解析失败，请检测XML格式");
      return;
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("name", name);
    // jsonObject.put("tables", StringUtils.join(pojo.getDependTableIds(), ","));
    jsonObject.put("task", pojo.getTask());
    try {
      GitUtils.$().updateWorkflowFile(name, name, jsonObject.toString(1), "update workflow " + name);
    } catch (Exception e) {
      action.addErrorMessage(context, "git配置文件修改失败");
      action.addErrorMessage(context, e.getMessage());
    }
  }

  /**
   * description: 获取所有的数据源 date: 7:43 PM 5/19/2017
   */
  public Collection<OfflineDatasourceAction.DatasourceDb> getDatasourceInfo() throws Exception {
    DatasourceDbCriteria criteria = new DatasourceDbCriteria();
    criteria.createCriteria();
    List<DatasourceDb> dbList = workflowDAOFacade.getDatasourceDbDAO().selectByExample(criteria);
    //DatasourceTableCriteria tableCriteria = new DatasourceTableCriteria();
    //tableCriteria.createCriteria();
    // List<DatasourceTable> tableList = workflowDAOFacade.getDatasourceTableDAO().selectByExample(tableCriteria);
    Map<Integer, OfflineDatasourceAction.DatasourceDb> dbsMap = new HashMap<>();
    OfflineDatasourceAction.DatasourceDb dsDb = null;
    for (DatasourceDb db : dbList) {
      if (StringUtils.isEmpty(db.getExtendClass())) {
        continue;
      }
      dsDb = new OfflineDatasourceAction.DatasourceDb(db.getId(), db.getExtendClass());
      dsDb.setName(db.getName());
      dbsMap.put(db.getId(), dsDb);
    }
//    for (DatasourceTable table : tableList) {
//      int dbId = table.getDbId();
//      if (dbsMap.containsKey(dbId)) {
//        OfflineDatasourceAction.DatasourceDb datasourceDb = dbsMap.get(dbId);
//        datasourceDb.addTable(table);
//      }
//    }
    return dbsMap.values();
  }

  public DBConfigSuit getDbConfig(IPluginContext pluginContext, DatasourceDb db) {
    Objects.requireNonNull(db, "instance of DatasourceDb can not be null");

    DBDataXReaderDescName dbDataXReaderDesc = this.getDBDataXReaderDescName(db.getName());
    DataxReader dbDataxReader = null;
    if (dbDataXReaderDesc.isSupportDataXReader()) {
      dbDataxReader = getDBDataxReader(pluginContext, db.getName());
    }
    DBConfigSuit dbSuit = new DBConfigSuit(db, dbDataXReaderDesc.isSupportDataXReader(), dbDataxReader != null);

    if (dbDataxReader != null) {
      List<ISelectedTab> selectedTabs = dbDataxReader.getSelectedTabs();
//      dbSuit.addTabs(selectedTabs.stream()
//        .map((t) -> t.getName()).collect(Collectors.toList()));
      dbSuit.addTabs(selectedTabs);
    }

    PostedDSProp dbProp = new PostedDSProp(DBIdentity.parseId(db.getName()), DbScope.DETAILED);

    DataSourceFactory dsPlugin = TIS.getDataBasePlugin(dbProp);

    //  DataSourceFactory dsPlugin = dbStore.getPlugin();
    dbSuit.setDetailed(dsPlugin);
    DataSourceFactory facadeStore
      = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(db.getName()), DbScope.FACADE), false);

    if ((dsPlugin = facadeStore) != null) {
      dbSuit.setFacade(dsPlugin);
    }
    return dbSuit;
  }

  public DBConfigSuit getDbConfig(IPluginContext pluginContext, Integer dbId) {
    return this.getDbConfig(pluginContext, getDB(dbId));
  }

  public DatasourceDb getDB(Integer dbId) {
    DatasourceDb db = workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(dbId);
    if (db == null) {
      throw new IllegalStateException("dbid:" + dbId + " can not find relevant db object in DB");
    }
    return db;
  }

//  public TISTable getTableConfig(IPluginContext pluginContext, Integer tableId) {
//    DatasourceTable tab = this.workflowDAOFacade.getDatasourceTableDAO().selectByPrimaryKey(tableId);
//    DatasourceDb db = this.workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(tab.getDbId());
//    DataSourceFactoryPluginStore dbPlugin = TIS.getDataBasePluginStore(new PostedDSProp(db.getName()));
//    TISTable t = dbPlugin.loadTableMeta(tab.getName());
//    t.setDbName(db.getName());
//    t.setTableName(tab.getName());
//    t.setTabId(tableId);
//    t.setDbId(db.getId());
//    return t;
//  }

  public WorkflowPojo getWorkflowConfig(Integer workflowId, boolean isMaster) {
    WorkFlow workFlow = this.workflowDAOFacade.getWorkFlowDAO().selectByPrimaryKey(workflowId);
    if (workFlow == null) {
      throw new IllegalStateException("workflow obj is null");
    }
    return GitUtils.$().getWorkflow(workFlow.getName(), GitBranchInfo.$("xxxxxxxxxxxxxx"));
  }

  // public WorkflowPojo getWorkflowConfig(String name, boolean isMaster) {
  // return
  // }
  // public WorkflowPojo getWorkflowConfig(String name, String sha) {
  // return GitUtils.$().getWorkflowSha(GitUtils.WORKFLOW_GIT_PROJECT_ID, sha,
  // name);
  // }
  public void deleteWorkflow(int id, BasicModule action, Context context) {
    WorkFlow workFlow = workflowDAOFacade.getWorkFlowDAO().selectByPrimaryKey(id);
    if (workFlow == null) {
      action.addErrorMessage(context, "数据库没有这条记录");
      return;
    }
    if (workFlow.getInChange().intValue() != 0) {
      action.addErrorMessage(context, "该工作流在变更中，无法删除");
      return;
    }
    ApplicationCriteria applicationCriteria = new ApplicationCriteria();
    applicationCriteria.createCriteria().andWorkFlowIdEqualTo(id);
    List<Application> applications = action.getApplicationDAO().selectByExample(applicationCriteria);
    if (!CollectionUtils.isEmpty(applications)) {
      StringBuilder stringBuilder = new StringBuilder();
      for (Application application : applications) {
        stringBuilder.append(application.getProjectName()).append(", ");
      }
      action.addErrorMessage(context, "请先删除与该工作流相关的索引，相关索引为" + stringBuilder.toString());
      return;
    }
    try {
      GitUser user = GitUser.dft();
      // delete git
      GitUtils.$().deleteWorkflow(workFlow.getName(), user);
      // delete db
      workflowDAOFacade.getWorkFlowDAO().deleteByPrimaryKey(id);
      // TODO 删除线上db的数据，发送一个http请求
      action.addActionMessage(context, "工作流删除成功");
    } catch (Exception e) {
      action.addErrorMessage(context, "工作流删除失败");
      action.addErrorMessage(context, e.getMessage());
    }
  }

  /**
   * 校验是否存在相同的表
   *
   * @param tableLogicName
   * @return
   */
  public boolean checkTableLogicNameRepeat(String tableLogicName, DatasourceDb db) {
//    if (db == null) {
//      throw new IllegalStateException(" database can not be null");
//    }
//    DatasourceTableCriteria criteria = new DatasourceTableCriteria();
//    criteria.createCriteria().andNameEqualTo(tableLogicName).andDbIdEqualTo(db.getId());
//    int tableCount = workflowDAOFacade.getDatasourceTableDAO().countByExample(criteria);
//    return tableCount > 0;
    return false;
  }

  //
  // public void syncDb(int id, String dbName, BasicModule action, Context
  // context) {
  // DatasourceDbCriteria criteria = new DatasourceDbCriteria();
  // criteria.createCriteria().andIdEqualTo(id).andNameEqualTo(dbName);
  // List<DatasourceDb> dbList =
  // this.workflowDAOFacade.getDatasourceDbDAO().selectByExample(criteria);
  //
  // // 1. 检查是否有这个数据库
  // if (CollectionUtils.isEmpty(dbList)) {
  // action.addErrorMessage(context, "找不到该数据库，id为" + id + "，数据库名为" + dbName);
  // return;
  // }
  //
  // // 2. 看看数据库的状态
  // DatasourceDb datasourceDb = dbList.get(0);
  // if (datasourceDb.getSyncOnline().intValue() == 1) {
  // action.addErrorMessage(context, "该数据库已经同步了");
  // return;
  // }
  //
  // // 3. 看看git线上有没有这个文件
  // String gitPah = dbName + "/db_config";
  // boolean onlineGitFileExisted =
  // GitUtils.$().isFileExisted(GitUtils.DATASOURCE_PROJECT_ID_ONLINE,
  // gitPah);
  // if (onlineGitFileExisted) {
  // action.addErrorMessage(context, "线上git已经有该数据库的配置文件了");
  // return;
  // }
  //
  // // 4. git同步配置文件到线上
  //
  // try {
  // String file = GitUtils.$().getFileContent(GitUtils.DATASOURCE_PROJECT_ID,
  // gitPah);
  // GitUtils.$().createDatasourceFileOnline(gitPah, file, "add db " +
  // dbName);
  // } catch (Exception e) {
  // action.addErrorMessage(context, "git同步配置文件到线上失败");
  // return;
  // }
  //
  // // 5. db记录同步到线上
  // // TODO 等线上配好数据库
  // List<HttpUtils.PostParam> params = new LinkedList<>();
  // params.add(new HttpUtils.PostParam("id",
  // datasourceDb.getId().toString()));
  // params.add(new HttpUtils.PostParam("name", datasourceDb.getName()));
  // String url = URL_ONLINE +
  // "&&event_submit_do_sync_db_record=true&resulthandler=advance_query_result";
  // try {
  // HttpUtils.post(new URL(url), params, new PostFormStreamProcess<Boolean>()
  // {
  //
  // public Boolean p(int status, InputStream stream, String md5) {
  // try {
  // return Boolean.parseBoolean(IOUtils.toString(stream, "utf8"));
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }
  // });
  // } catch (MalformedURLException e) {
  // e.printStackTrace();
  // }
  //
  // // 6. 日常db状态更改
  // datasourceDb.setSyncOnline(new Byte("1"));
  // this.workflowDAOFacade.getDatasourceDbDAO().updateByExample(datasourceDb,
  // criteria);
  // }
  //
  // public void syncTable(int id, String tableLogicName, BasicModule action,
  // Context context) {
  // DatasourceTableCriteria criteria = new DatasourceTableCriteria();
  // criteria.createCriteria().andIdEqualTo(id).andTableLogicNameEqualTo(tableLogicName);
  // List<DatasourceTable> tableList =
  // this.workflowDAOFacade.getDatasourceTableDAO().selectByExample(criteria);
  //
  // // 1. 检查是否有这个数据库
  // if (CollectionUtils.isEmpty(tableList)) {
  // action.addErrorMessage(context, "找不到该数据库表，id为" + id + "，数据库逻辑名为" +
  // tableLogicName);
  // return;
  // }
  //
  // // 2. 看看数据库的状态
  // DatasourceTable datasourceTable = tableList.get(0);
  // if (datasourceTable.getSyncOnline().intValue() == 1) {
  // action.addErrorMessage(context, "该数据库表已经同步了");
  // return;
  // }
  // int dbId = datasourceTable.getDbId();
  // DatasourceDb datasourceDb =
  // this.workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(dbId);
  //
  // // 3. 看看git线上有没有这个文件
  // String gitPah = datasourceDb.getName() + "/" + tableLogicName;
  // boolean onlineGitFileExisted =
  // GitUtils.$().isFileExisted(GitUtils.DATASOURCE_PROJECT_ID_ONLINE,
  // gitPah);
  // if (onlineGitFileExisted) {
  // action.addErrorMessage(context, "线上git已经有该数据库表的配置文件了");
  // return;
  // }
  //
  // // 4. git同步配置文件到线上
  // try {
  // String file = GitUtils.$().getFileContent(GitUtils.DATASOURCE_PROJECT_ID,
  // gitPah);
  // GitUtils.$().createDatasourceFileOnline(gitPah, file, "add table " +
  // gitPah);
  // } catch (Exception e) {
  // action.addErrorMessage(context, "git同步配置文件到线上失败");
  // return;
  // }
  //
  // // 5. db记录同步到线上
  // // TODO 等线上配好数据库
  // List<HttpUtils.PostParam> params = new LinkedList<>();
  // params.add(new HttpUtils.PostParam("id",
  // datasourceTable.getId().toString()));
  // params.add(new HttpUtils.PostParam("name", datasourceTable.getName()));
  // params.add(new HttpUtils.PostParam("table_logic_name",
  // datasourceTable.getTableLogicName()));
  // params.add(new HttpUtils.PostParam("db_id",
  // datasourceTable.getDbId().toString()));
  // params.add(new HttpUtils.PostParam("git_tag",
  // datasourceTable.getGitTag()));
  // String url = URL_ONLINE +
  // "&&event_submit_do_sync_table_record=true&resulthandler=advance_query_result";
  // try {
  // HttpUtils.post(new URL(url), params, new PostFormStreamProcess<Boolean>()
  // {
  //
  // public Boolean p(int status, InputStream stream, String md5) {
  // try {
  // return Boolean.parseBoolean(IOUtils.toString(stream, "utf8"));
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }
  // });
  // } catch (MalformedURLException e) {
  // e.printStackTrace();
  // }
  //
  // // 6. 日常db状态更改
  // datasourceTable.setSyncOnline(new Byte("1"));
  // this.workflowDAOFacade.getDatasourceTableDAO().updateByExample(datasourceTable,
  // criteria);
  // }
  public void syncDbRecord(DatasourceDb datasourceDb, BasicModule action, Context context) {
    try {
      this.workflowDAOFacade.getDatasourceDbDAO().insertSelective(datasourceDb);
      action.setBizResult(context, true);
    } catch (Exception e) {
      action.addErrorMessage(context, e.getMessage());
      action.setBizResult(context, false);
    }
  }

//  public void syncTableRecord(DatasourceTable datasourceTable, BasicModule action, Context context) {
//    try {
//      this.workflowDAOFacade.getDatasourceTableDAO().insertSelective(datasourceTable);
//      action.setBizResult(context, true);
//    } catch (Exception e) {
//      action.addErrorMessage(context, e.getMessage());
//      action.setBizResult(context, false);
//    }
//  }

  private static boolean isXmlValid(JoinRule xmlStr) {
    boolean result = true;
    try {
      StringReader sr = new StringReader(xmlStr.getContent());
      InputSource is = new InputSource(sr);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.parse(is);
    } catch (Exception e) {
      result = false;
    }
    return result;
  }

  // /**
  // * description: 使某个table的dump失效 date: 11:20 AM 6/17/2017
  // */
  // public void disableTableDump(int tableId, BasicModule action, Context context) {
  // TableDumpCriteria criteria = new TableDumpCriteria();
  // criteria.createCriteria().andDatasourceTableIdEqualTo(tableId).andStateEqualTo(new Byte("1")).andIsValidEqualTo(new Byte("1"));
  // criteria.setOrderByClause("op_time desc");
  // // 取出最新的一个合法的dump
  // List<TableDump> tableDumps = this.workflowDAOFacade.getTableDumpDAO().selectByExampleWithoutBLOBs(criteria, 1, 1);
  // if (CollectionUtils.isEmpty(tableDumps)) {
  // return;
  // }
  // TableDump tableDump = tableDumps.get(0);
  // tableDump.setIsValid(new Byte("0"));
  // criteria = new TableDumpCriteria();
  // criteria.createCriteria().andIdEqualTo(tableDump.getId());
  // // 更新它
  // this.workflowDAOFacade.getTableDumpDAO().updateByExampleSelective(tableDump, criteria);
  // }
  // /**
  // * description: 使某个db的所有table的dump失效 date: 11:20 AM 6/17/2017
  // */
  // public void disableDbDump(int dbId, BasicModule action, Context context) {
  // DatasourceTableCriteria tableCriteria = new DatasourceTableCriteria();
  // tableCriteria.createCriteria().andDbIdEqualTo(dbId);
  // List<DatasourceTable> datasourceTables = this.workflowDAOFacade.getDatasourceTableDAO().selectByExample(tableCriteria);
  // if (CollectionUtils.isEmpty(datasourceTables)) {
  // return;
  // }
  // // 对所有的table处理
  // for (DatasourceTable datasourceTable : datasourceTables) {
  // this.disableTableDump(datasourceTable.getId(), action, context);
  // }
  // }
  public void deleteDbById(int dbId, DbScope dbModel, BasicModule action, Context context) throws Exception {
    // 1 先检查db是否存在
    DatasourceDb db = this.workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(dbId);
    if (db == null) {
      action.addErrorMessage(context, "找不到该db，db id = " + dbId);
      return;
    }

    // 2 检查还有表没
//    DatasourceTableCriteria tableCriteria = new DatasourceTableCriteria();
//    tableCriteria.createCriteria().andDbIdEqualTo(dbId);
//    List<DatasourceTable> datasourceTables = this.workflowDAOFacade.getDatasourceTableDAO().minSelectByExample(tableCriteria);
//    if (dbModel == DbScope.DETAILED && !CollectionUtils.isEmpty(datasourceTables)) {
//      action.addErrorMessage(context, "该数据库下仍然有数据表，请先删除所有的表");
//      return;
//    }
//    DataSourceFactoryPluginStore dsPluginStore = TIS.getDataBasePluginStore(new PostedDSProp(db.getName()));
//    dsPluginStore.deleteDB();
    TIS.deleteDB(db.getName(), dbModel);
    // GitUser user = GitUser.dft();
    // 3 删除git
    //GitUtils.$().deleteDb(db.getName(), user);
    // 4 删除db
    if (dbModel == DbScope.DETAILED) {
      this.workflowDAOFacade.getDatasourceDbDAO().deleteByPrimaryKey(dbId);
    }
    action.addActionMessage(context, "成功删除'" + db.getName() + "'");
  }

  public void deleteDatasourceTableById(int tableId, BasicModule action, Context context) {
    throw new UnsupportedOperationException();
    // 1 检查表是否存在
//    DatasourceTable datasourceTable = this.workflowDAOFacade.getDatasourceTableDAO().selectByPrimaryKey(tableId);
//    if (datasourceTable == null) {
//      action.addErrorMessage(context, "找不到该表，table id = " + tableId);
//      return;
//    }
//    // 2 检查对应的db是否存在
//    Integer dbId = datasourceTable.getDbId();
//    DatasourceDb datasourceDb = this.workflowDAOFacade.getDatasourceDbDAO().selectByPrimaryKey(dbId);
//    if (datasourceDb == null) {
//      action.addErrorMessage(context, "找不到该表对应的数据库，db id = " + dbId);
//      return;
//    }
//    // 3 检查是否有工作流用到了该table
//    WorkFlowCriteria workFlowCriteria = new WorkFlowCriteria();
//    workFlowCriteria.createCriteria();
//    List<WorkFlow> workFlows = this.workflowDAOFacade.getWorkFlowDAO().selectByExample(workFlowCriteria);
//    for (WorkFlow workFlow : workFlows) {
//      // WorkflowPojo workflowPojo = getWorkflowConfig(workFlow.getName(), true);
//      // if (!CollectionUtils.isEmpty(workflowPojo.getDependTableIds())
//      // && workflowPojo.getDependTableIds().contains(datasourceTable.getId())) {
//      // action.addErrorMessage(context,
//      // "数据库下面的表" + datasourceTable.getTableLogicName() + "仍然被工作流" +
//      // workflowPojo.getName() + "占用");
//      // return;
//      // }
//    }
//    // 4 删除git
//    try {
//      GitUser user = GitUser.dft();
//      RunEnvironment runEnvironment = action.getAppDomain().getRunEnvironment();
//      if (RunEnvironment.DAILY.equals(runEnvironment)) {
//        GitUtils.$().deleteTableDaily(datasourceDb.getName(), datasourceTable.getName(), user);
//      } else if (RunEnvironment.ONLINE.equals(runEnvironment)) {
//        GitUtils.$().deleteTableOnline(datasourceDb.getName(), datasourceTable.getName(), user);
//      } else {
//        action.addErrorMessage(context, "当前运行环境" + runEnvironment + "既不是daily也不是online");
//        return;
//      }
//    } catch (Exception e) {
//      action.addErrorMessage(context, "删除数据库失败");
//      action.addErrorMessage(context, e.getMessage());
//      return;
//    }
//    // 5 删除db
//    this.workflowDAOFacade.getDatasourceTableDAO().deleteByPrimaryKey(tableId);
//    action.addActionMessage(context, "成功删除table");
  }

  public void deleteWorkflowChange(int workflowId, BasicModule action, Context context) {
    WorkFlow workFlow = this.workflowDAOFacade.getWorkFlowDAO().selectByPrimaryKey(workflowId);
    if (workFlow == null) {
      action.addErrorMessage(context, "找不到id为" + workflowId + "的工作流");
      return;
    }
    if (workFlow.getInChange().intValue() == 0) {
      action.addErrorMessage(context, "id为" + workflowId + "的工作流不在变更中");
      return;
    }
    try {
      GitUtils.$().deleteWorkflowBranch(workFlow.getName());
    } catch (Exception e) {
      e.printStackTrace();
      action.addErrorMessage(context, "删除分支" + workFlow.getName() + "失败");
      action.addErrorMessage(context, e.getMessage());
      return;
    }
    int inChange = workFlow.getInChange().intValue();
    if (inChange == 1) {
      // 新建工作流变更中 直接删除db
      this.workflowDAOFacade.getWorkFlowDAO().deleteByPrimaryKey(workflowId);
    } else if (inChange == 2) {
      // 对已有的工作流进行变更
      workFlow.setInChange(new Byte("0"));
      WorkFlowCriteria criteria = new WorkFlowCriteria();
      criteria.createCriteria().andIdEqualTo(workflowId);
      this.workflowDAOFacade.getWorkFlowDAO().updateByExample(workFlow, criteria);
    }
    // 更新变更记录
//    WorkFlowPublishHistoryCriteria criteria1 = new WorkFlowPublishHistoryCriteria();
//    criteria1.createCriteria().andWorkflowIdEqualTo(workflowId).andPublishStateEqualTo(new Byte("3"));
//    List<WorkFlowPublishHistory> workFlowPublishHistories = this.workflowDAOFacade.getWorkFlowPublishHistoryDAO().selectByExampleWithoutBLOBs(criteria1);
//    if (CollectionUtils.isEmpty(workFlowPublishHistories)) {
//      action.addErrorMessage(context, "找不到这条变更记录");
//      return;
//    } else {
//      WorkFlowPublishHistory workFlowPublishHistory = workFlowPublishHistories.get(0);
//      workFlowPublishHistory.setPublishState(new Byte("2"));
//      this.workflowDAOFacade.getWorkFlowPublishHistoryDAO().updateByExampleWithoutBLOBs(workFlowPublishHistory, criteria1);
//    }
    action.addActionMessage(context, "删除变更成功");
  }

  public void confirmWorkflowChange(int workflowId, BasicModule action, Context context) {
    WorkFlow workFlow = this.workflowDAOFacade.getWorkFlowDAO().selectByPrimaryKey(workflowId);
    if (workFlow == null) {
      action.addErrorMessage(context, "找不到id为" + workflowId + "的工作流");
      return;
    }
    int inChange = workFlow.getInChange().intValue();
    if (inChange == 0) {
      action.addErrorMessage(context, "id为" + workflowId + "的工作流不在变更中");
      return;
    }
    // git 合并
    try {
      GitUtils.$().mergeWorkflowChange(workFlow.getName());
    } catch (Exception e) {
      action.addErrorMessage(context, "git分支合并失败");
      action.addErrorMessage(context, "此次变更没有任何配置变动，请撤销变更");
      action.addErrorMessage(context, e.getMessage());
      return;
    }
    // db 变更
    if (inChange == 1) {
      // 新建工作流变更中 同步到线上
      // TODO 发送一个请求给线上的console 要求加入一条db记录
    }
    // 对已有的工作流进行变更
    workFlow.setInChange(new Byte("0"));
    WorkFlowCriteria criteria = new WorkFlowCriteria();
    criteria.createCriteria().andIdEqualTo(workflowId);
    this.workflowDAOFacade.getWorkFlowDAO().updateByExample(workFlow, criteria);
    // 变更历史
//    WorkFlowPublishHistoryCriteria criteria1 = new WorkFlowPublishHistoryCriteria();
//    criteria1.createCriteria().andWorkflowIdEqualTo(workflowId).andPublishStateEqualTo(new Byte("3"));
//    List<WorkFlowPublishHistory> workFlowPublishHistories = this.workflowDAOFacade.getWorkFlowPublishHistoryDAO().selectByExampleWithoutBLOBs(criteria1);
//    if (CollectionUtils.isEmpty(workFlowPublishHistories)) {
//      action.addErrorMessage(context, "找不到这条变更记录");
//      return;
//    } else {
//      // 把之前在使用中的变为未使用
//      WorkFlowPublishHistoryCriteria inUseCriteria = new WorkFlowPublishHistoryCriteria();
//      inUseCriteria.createCriteria().andWorkflowIdEqualTo(workflowId).andInUseEqualTo(true);
//      WorkFlowPublishHistory notInUse = new WorkFlowPublishHistory();
//      notInUse.setInUse(false);
//      this.workflowDAOFacade.getWorkFlowPublishHistoryDAO().updateByExampleSelective(notInUse, inUseCriteria);
//      // 把新建的记录给确定下来
//      WorkFlowPublishHistory workFlowPublishHistory = workFlowPublishHistories.get(0);
//      workFlowPublishHistory.setPublishState(new Byte("1"));
//      workFlowPublishHistory.setGitSha1(GitUtils.$().getLatestSha(GitUtils.WORKFLOW_GIT_PROJECT_ID));
//      workFlowPublishHistory.setInUse(true);
//      this.workflowDAOFacade.getWorkFlowPublishHistoryDAO().updateByExampleSelective(workFlowPublishHistory, criteria1);
//    }
    action.addActionMessage(context, "变更提交成功");
  }

  public static class DBDataXReaderDescName {
    public final Optional<String> readerDescName;
    public final DataSourceFactory.BaseDataSourceFactoryDescriptor dsDescriptor;

    public DBDataXReaderDescName(Optional<String> readerDescName, DataSourceFactory.BaseDataSourceFactoryDescriptor dsDescriptor) {
      this.readerDescName = readerDescName;
      this.dsDescriptor = dsDescriptor;
    }

    public boolean isSupportDataXReader() {
      return readerDescName.isPresent();
    }

    public String getReaderDescName() {
      return this.readerDescName.get();
    }
  }
}
