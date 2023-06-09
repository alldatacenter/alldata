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
package com.qlangtech.tis.config.module.action;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.*;
import com.qlangtech.tis.coredefine.module.control.SelectableServer;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.biz.dal.dao.impl.SnapshotViewImplDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.TISCollectionUtils;
import com.qlangtech.tis.offline.module.manager.impl.OfflineManager;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.incr.IncrStreamFactory;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState;
import com.qlangtech.tis.runtime.module.action.CreateIndexConfirmModel;
import com.qlangtech.tis.runtime.module.action.SchemaAction;
import com.qlangtech.tis.runtime.module.action.SysInitializeAction;
import com.qlangtech.tis.solrdao.ISchemaField;
import com.qlangtech.tis.solrdao.ISchemaPluginContext;
import com.qlangtech.tis.solrdao.SchemaResult;
import com.qlangtech.tis.solrdao.pojo.PSchemaField;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.meta.Position;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.util.*;
import com.qlangtech.tis.workflow.pojo.DatasourceDb;
import com.qlangtech.tis.workflow.pojo.DatasourceDbCriteria;
import com.qlangtech.tis.workflow.pojo.WorkFlow;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistoryCriteria;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020-12-13 16:10
 */
public class CollectionAction extends com.qlangtech.tis.runtime.module.action.AddAppAction {
  private static final String QUERY_PARSIING_DEF_TYPE = "defType";
  private static final Position DEFAULT_SINGLE_TABLE_POSITION;
  private static final Position DEFAULT_SINGLE_JOINER_POSITION;
  private static final Logger logger = LoggerFactory.getLogger(CollectionAction.class);
  private static final int SHARED_COUNT = 1;
  public static final String KEY_SHOW_LOG = "log";
  public static final String KEY_INDEX_NAME = "indexName";
  public static final String KEY_QUERY_SEARCH_FIELDS = "search_fields";
  public static final String KEY_QUERY_FIELDS = "fields";
  //public static final String KEY_QUERY_QUERY_FIELDS = "queryFields";
  public static final String KEY_QUERY_LIMIT = "limit";
  public static final String KEY_QUERY_ORDER_BY = "orderBy";
  public static final String KEY_QUERY_ROWS_OFFSET = "rowsOffset";

  public static final String RESULT_KEY_ROWS_COUNT = "rowsCount";
  public static final String RESULT_KEY_ROWS = "rows";

  public static final String KEY_PK = "pk";
  public static final String KEY_SHARD_NAME = "name";
  public static final String KEY_CORE_URL = "coreUrl";
  public static final String KEY_IS_ACTIVE = "active";
  public static final String KEY_REPLICS = "replics";
  public static final String KEY_COLS_META = "colMetas";


//  private

  static {
    DEFAULT_SINGLE_TABLE_POSITION = new Position();
    DEFAULT_SINGLE_TABLE_POSITION.setX(141);
    DEFAULT_SINGLE_TABLE_POSITION.setY(121);

    DEFAULT_SINGLE_JOINER_POSITION = new Position();
    DEFAULT_SINGLE_JOINER_POSITION.setX(237);
    DEFAULT_SINGLE_JOINER_POSITION.setY(296);
  }

  private IndexName indexName = null;

  private PlatformTransactionManager transactionManager;
  private OfflineManager offlineManager;

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

//  /**
//   * @param context
//   * @throws Exception
//   */
//  public void doGetIndexTopology(Context context) throws Exception {
//    this.getIndexWithPost();
//    JSONObject biz = new JSONObject();
//
//    CollectionTopology topology = CoreAction.getCollectionTopology(this);
//
//    JSONArray shards = new JSONArray();
//    JSONObject shard = null;
//    JSONArray replics = null;
//    JSONObject replic = null;
//    for (CollectionTopology.Shared s : topology.getShareds()) {
//      shard = new JSONObject();
//      replics = new JSONArray();
//      shard.put(KEY_SHARD_NAME, s.getName());
//      for (Replica r : s.getReplics()) {
//        replic = new JSONObject();
//        replic.put(KEY_CORE_URL, r.getCoreUrl());
//        replic.put(KEY_IS_ACTIVE, r.getState() == Replica.State.ACTIVE);
//        replics.add(replic);
//      }
//      shard.put(KEY_REPLICS, replics);
//      shards.add(shard);
//    }
//
//
//    biz.put(SqlTaskNodeMeta.KEY_PROFILE_TOPOLOGY, shards);
//    SnapshotDomain snapshot = ViewPojo.getSnapshotDoamin(this, this.getAppDomain());
//    ParseResult parseResult = SolrFieldsParser.parse(() -> {
//      return snapshot.getSolrSchema().getContent();
//    }).getSchemaParseResult();
//
//    biz.put(KEY_PK, parseResult.getUniqueKey());
//
//
//    QueryResutStrategy queryStrategy = QueryIndexServlet.createQueryResutStrategy(
//      this.getAppDomain(), new IndexQuery.QueryRequestWrapper(getRequest(), context), getResponse(), getDaoContext());
//    List<ServerJoinGroup> nodes = queryStrategy.queryProcess();
//    List<PSchemaField> sfields = IndexQuery.getSfields(this.getRequest(), queryStrategy, nodes);
//    JSONArray colsMeta = new JSONArray();
//    JSONObject colmeta = null;
//    for (PSchemaField field : sfields) {
//      colmeta = new JSONObject();
//      colmeta.put("name", field.getName());
//      colmeta.put("typeName", field.getType().getJavaType().getSimpleName());
//      colmeta.put("typeCode", field.getType().getJavaType().getTypeCode());
//      colmeta.put("docval", field.isDocValue());
//      colmeta.put("indexd", field.isIndexed());
//      colmeta.put("stored", field.isStored());
//      colmeta.put("dynamic", field.isDynamic());
//      colsMeta.add(colmeta);
//    }
//    biz.put(KEY_COLS_META, colsMeta);
//
//    this.setBizResult(context, biz);
//  }

//  /**
//   * 回调获取索引当前状态
//   *
//   * @param context
//   * @throws Exception
//   */
//  public void doGetIndexStatus(Context context) throws Exception {
//    this.getIndexWithPost();
//    this.setBizResult(context, CoreAction.getCollectionStatus(this));
//  }

  /**
   * 取得增量执行状态
   *
   * @param context
   */
  public void doGetIncrStatus(Context context) throws Exception {
    this.getIndexWithPost();
    this.setBizResult(context, CoreAction.getIndexIncrStatus(this, true));
  }

//  /**
//   * 回调获取索引创建的状态
//   *
//   * @param context
//   * @throws Exception
//   */
//  public void doGetTaskStatus(Context context) throws Exception {
//    JSONObject post = this.parseJsonPost();
//    Integer taskId = post.getInteger(IParamContext.KEY_TASK_ID);
//    boolean showLog = post.getBooleanValue(KEY_SHOW_LOG);
//
//    WorkFlowBuildHistory buildHistory = this.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByPrimaryKey(taskId);
//    if (buildHistory == null) {
//      throw new IllegalStateException("taskid:" + taskId + "relevant buildHistory can not be null");
//    }
//    if (StringUtils.isEmpty(buildHistory.getAppName())) {
//      throw new IllegalStateException("the prop appname of buildHistory can not be empty");
//    }
//    LogReader logReader = new LogReader();
//    if (showLog) {
//      RpcServiceReference service = StatusRpcClient.getService(getSolrZkClient());
//      PMonotorTarget.Builder t = PMonotorTarget.newBuilder();
//      t.setLogtype(LogCollectorClient.convert(LogType.FULL.typeKind));
//      t.setCollection(buildHistory.getAppName());
//      if (taskId > 0) {
//        t.setTaskid(taskId);
//      }
//      StatusRpcClient.AssembleSvcCompsite feedbackRpc = service.get();
//      StreamObserver<PMonotorTarget> observer = feedbackRpc.registerMonitorEvent(logReader);
//      observer.onNext(t.build());
//      Thread.sleep(3000);
//      observer.onCompleted();
//    }
//    Map<String, Object> bizResult = Maps.newHashMap();
//    bizResult.put("status", new ExtendWorkFlowBuildHistory(buildHistory));
//    if (showLog) {
//      bizResult.put("log", logReader.logContent.toString());
//    }
//    this.setBizResult(context, bizResult);
//
//  }

  /**
   * 创建索实例
   *
   * @param context
   * @throws Exception
   */
  public void doCreate(Context context) throws Exception {
    throw new UnsupportedOperationException();
//    JSONObject post = getIndexWithPost();
//    Objects.requireNonNull(this.indexName, "indexName can not be null");
//
//    JSONObject datasource = post.getJSONObject("datasource");
//    JSONObject incrCfg = post.getJSONObject("incr");
//    if (datasource == null) {
//      throw new IllegalStateException("prop 'datasource' can not be null");
//    }
//    final String targetTable = post.getString("table");
//    if (StringUtils.isEmpty(targetTable)) {
//      throw new IllegalStateException("param 'table' can not be null");
//    }
//    // this.indexName = StringUtils.defaultIfEmpty(post.getString(KEY_INDEX_NAME), targetTable);
//    List<String> existCollection = CoreAction.listCollection(this, context);
//    if (existCollection.contains(this.getCollectionName())) {
//      //throw new IllegalStateException();
//      this.addErrorMessage(context, "index:" + this.getCollectionName() + " already exist in cloud");
//      return;
//    }
//    PluginItems dataSourceItems = getDataSourceItems(datasource);
//    if (dataSourceItems.items.size() < 1) {
//      throw new IllegalStateException("datasource item can not small than 1,now:" + dataSourceItems.items.size());
//    }
//
//    TargetColumnMeta targetColMetas = getTargetColumnMeta(this, context, post, targetTable, dataSourceItems);
//    if (!targetColMetas.valid) {
//      return;
//    }
//    dataSourceItems.save(context);
//    if (context.hasErrors()) {
//      return;
//    }
//    DBConfigSuit dsDb = (DBConfigSuit) context.get(IMessageHandler.ACTION_BIZ_RESULT);
//    Objects.requireNonNull(dsDb, "can not find dsDb which has insert into DB just now");
//
//    TISTable table = new TISTable();
//    table.setTableName(targetTable);
//    table.setDbId(dsDb.getDbId());
//
////    OfflineManager.ProcessedTable dsTable = offlineManager.addDatasourceTable(table, this
////      , this, context, false, true);
//    if (context.hasErrors()) {
//      return;
//    }
//    this.setBizResult(context, new Object());
//    Objects.requireNonNull(dsTable, "dsTable can not be null");
//
//    // 开始创建DF
//    final String topologyName = indexName.param;
//    File parent = new File(SqlTaskNode.parent, topologyName);
//    FileUtils.forceMkdir(parent);
//    final SqlTaskNodeMeta.SqlDataFlowTopology topology = this.createTopology(topologyName, dsTable, targetColMetas);
//
//    OfflineDatasourceAction.CreateTopologyUpdateCallback dbSaver
//      = new OfflineDatasourceAction.CreateTopologyUpdateCallback(this.getUser(), this.getWorkflowDAOFacade(), true);
//    WorkFlow df = dbSaver.execute(topologyName, topology);
//    // 保存一个时间戳
//    SqlTaskNodeMeta.persistence(topology, parent);
//    boolean hasCreateCollection = false;
//    Optional<Application> createdApp = Optional.empty();
//    try {
//      // 在在引擎节点上创建实例节点
//      createdApp = this.createCollection(context, df, indexName.param, targetColMetas);
//      hasCreateCollection = true;
//      if (incrCfg != null) {
//        logger.info("start incr channel create");
//        if (!createIncrSyncChannel(context, incrCfg)) {
//          return;
//        }
//      }
//    } catch (Throwable e) {
//      if (hasCreateCollection) {
//        // 需要将已经 创建的索引删除
//        this.deleteCollectionInCloud(context, indexName.getCollectionName());
//      }
//      throw e;
//    }
//    // 需要提交一下事务
//    TransactionStatus tranStatus
//      = (TransactionStatus) ActionContext.getContext().get(TransactionStatus.class.getSimpleName());
//    Objects.requireNonNull(tranStatus, "transtatus can not be null");
//    transactionManager.commit(tranStatus);
//
//    if (!createdApp.isPresent()) {
//      throw new IllegalStateException("createdApp can not be null");
//    }
//
//    // 现在需要开始触发全量索引了
//    CoreAction.TriggerBuildResult triggerBuildResult
//      = CoreAction.triggerFullIndexSwape(this, context, createdApp.get(), SHARED_COUNT);
//    this.setBizResult(context, triggerBuildResult);
  }

  @Override
  public String getCollectionName() {
    if ((this.indexName) == null) {
      throw new IllegalStateException("indexName can not be null");
    }
    return this.indexName.getCollectionName();
  }


  /**
   * 触发全量构建
   *
   * @param context
   * @throws Exception
   */
  public void doFullbuild(Context context) throws Exception {
    this.getIndexWithPost();

    Application app = this.getApplicationDAO().selectByName(this.indexName.getCollectionName());

    WorkFlow wf = this.loadDF(app.getWorkFlowId());

    this.setBizResult(context
      , CoreAction.triggerFullIndexSwape(this, context, app, 1));
  }

  private JSONObject getIndexWithPost() {
    JSONObject post = this.parseJsonPost();
    if (StringUtils.isEmpty(post.getString(KEY_INDEX_NAME))) {
      throw new IllegalArgumentException("indexName can not be null");
    }
    this.indexName = new IndexName(post.getString(KEY_INDEX_NAME)); // TISCollectionUtils.NAME_PREFIX + ;
    return post;
  }

  /**
   * 删除索引实例
   *
   * @param context
   * @throws Exception
   */
  public void doDeleteIndex(Context context) throws Exception {
    getIndexWithPost();
    // 删除
    Application app = this.getApplicationDAO().selectByName(this.indexName.getCollectionName());
    if (app == null) {
      throw new IllegalStateException("indexName:" + this.indexName.getCollectionName() + " relevant instance in db can not be empty");
    }
    // final WorkFlow workFlow = this.loadDF(app.getWorkFlowId());
    this.rescycleAppDB(app.getAppId());
    //this.getWorkflowDAOFacade().getWorkFlowDAO().deleteByPrimaryKey(workFlow.getId());
    WorkFlowBuildHistoryCriteria wfHistoryCriteria = new WorkFlowBuildHistoryCriteria();
    //wfHistoryCriteria.createCriteria().andWorkFlowIdEqualTo(workFlow.getId());
    this.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().deleteByExample(wfHistoryCriteria);

    this.deleteCollectionInCloud(context, app.getProjectName());

    // 删除workflow数据库及本地存储文件
//    TopologyDir topologyDir = SqlTaskNodeMeta.getTopologyDir(workFlow.getName());
//    if (topologyDir.synchronizeSubRemoteRes().size() > 0) {
//      IndexStreamCodeGenerator indexStreamCodeGenerator
//        = CoreAction.getIndexStreamCodeGenerator(this, workFlow, false);
//      indexStreamCodeGenerator.deleteScript();
//    }
//    topologyDir.delete();

    IPluginStore<IncrStreamFactory> store = CoreAction.getIncrStreamFactoryStore(this);
    try {
      if (store.getPlugin() != null) {
        // 删除增量实例
        TISK8sDelegate k8sDelegate = TISK8sDelegate.getK8SDelegate(this.getCollectionName());
        k8sDelegate.removeIncrProcess();
      }
    } catch (Throwable e) {
      // 可能创建过程增量没有正常，不能导致删除失败
      logger.warn("k8sDelegate illegal", e);
    }

  }

  private void deleteCollectionInCloud(Context context, String collectionName) {
    // 删除索引实例
//    try {
//      URL url = new URL("http://" + CoreAction.getCloudOverseerNode(this.getSolrZkClient())
//        + CoreAction.ADMIN_COLLECTION_PATH + "?action=DELETE&name=" + collectionName);
//      HttpUtils.processContent(url, new ConfigFileContext.StreamProcess<Object>() {
//        @Override
//        public Object p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//          ProcessResponse result = null;
//          if ((result = ProcessResponse.processResponse(stream, (err) -> addErrorMessage(context, err))).success) {
//            addActionMessage(context, "成功删除了索引实例'" + collectionName + "'");
//          }
//          return null;
//        }
//      });
//    } catch (Throwable e) {
//      logger.warn(e.getMessage(), e);
//    }
  }

//  /**
//   * 利用solr的disMax QP进行查询
//   *
//   * @param context
//   * @throws Exception
//   */
//  public void doQuery(Context context) throws Exception {
//    JSONObject post = getIndexWithPost();//this.parseJsonPost();
//
////    if (StringUtils.isEmpty(post.getString(KEY_INDEX_NAME))) {
////      throw new IllegalArgumentException("indexName can not be null");
////    }
//    // this.indexName = TISCollectionUtils.NAME_PREFIX + post.getString(KEY_INDEX_NAME);
//
//    JSONArray searchFields = post.getJSONArray(KEY_QUERY_SEARCH_FIELDS);
//    Objects.requireNonNull(searchFields, "param " + KEY_QUERY_SEARCH_FIELDS + " can not be null ");
//    if (searchFields.size() < 1) {
//      throw new IllegalArgumentException(KEY_QUERY_SEARCH_FIELDS + " relevant field can not be empty");
//    }
//    final List<SubCriteria> andQueryCriteria = Lists.newArrayList();
//    searchFields.forEach((f) -> {
//      SubCriteria subCriteria = new SubCriteria();
//      JSONObject o = (JSONObject) f;
//      Option opt = null;
//      String word = null;
//      for (String key : o.keySet()) {
//        word = o.getString(key);
//        if (StringUtils.isEmpty(word)) {
//          throw new IllegalArgumentException("query field:" + key + ",relevant:" + word + ",val can not be null");
//        }
//        opt = new Option(key, word);
//        subCriteria.addOr(opt);
//      }
//      andQueryCriteria.add(subCriteria);
//    });
//
//    JSONArray storedFields = post.getJSONArray(KEY_QUERY_FIELDS);
//    if (storedFields == null) {
//      throw new IllegalArgumentException("param 'fields' can not be null");
//    }
//    storedFields.stream().map((r) -> (String) r).collect(Collectors.toList());
//
////    final String fields = post.getString(KEY_QUERY_FIELDS);
////    if (StringUtils.isEmpty(fields)) {
////      throw new IllegalArgumentException("param 'fields' can not be null");
////    }
//    final Integer limit = post.getInteger(KEY_QUERY_LIMIT);
//    if (limit == null) {
//      throw new IllegalArgumentException("param limit can not be null");
//    }
////    final String queryFields = post.getString(KEY_QUERY_QUERY_FIELDS);
////    if (StringUtils.isEmpty(queryFields)) {
////      throw new IllegalArgumentException("'queryFields' can not be null");
////    }
//    final String orderBy = post.getString(KEY_QUERY_ORDER_BY);
//    Integer rowsOffset = post.getInteger(KEY_QUERY_ROWS_OFFSET);
//
//    AppDomainInfo app = getAppDomain();
//    final QueryResutStrategy queryResutStrategy = QueryIndexServlet.createQueryResutStrategy(app, this.getRequest(), getResponse(), getDaoContext());
//
//    final List<ServerJoinGroup> serverlist = queryResutStrategy.queryProcess();
//    for (ServerJoinGroup server : serverlist) {
//
//      // 组装url
//      final String url = server.getIpAddress();
//
//      QueryCloudSolrClient solrClient = new QueryCloudSolrClient(url);
//      SolrQuery query = new SolrQuery();
//      query.set(CommonParams.FL, storedFields.stream().map((r) -> (String) r).collect(Collectors.joining(",")));
//      // query.setParam(QUERY_PARSIING_DEF_TYPE, "dismax");
//      // query.setParam(DisMaxParams.QF, queryFields);
//      query.setQuery(this.createQuery(andQueryCriteria));
//      query.setRows(limit);
//      if (rowsOffset != null) {
//        query.setStart(rowsOffset);
//      }
//      if (StringUtils.isNotEmpty(orderBy)) {
//        query.add(CommonParams.SORT, orderBy);
//      }
//      QueryResponse result = solrClient.query(indexName.getCollectionName(), query, SolrRequest.METHOD.POST);
//      solrClient.close();
//      Map<String, Object> biz = Maps.newHashMap();
//      long c = result.getResults().getNumFound();
//      biz.put(RESULT_KEY_ROWS_COUNT, c);
//
//      List<Map<String, Object>> resultList = Lists.newArrayList();
//      Map<String, Object> row = null;
//      for (SolrDocument doc : result.getResults()) {
//        row = Maps.newHashMap();
//        for (Map.Entry<String, Object> f : doc.entrySet()) {
//          row.put(f.getKey(), f.getValue());
//        }
//        resultList.add(row);
//      }
//      biz.put(RESULT_KEY_ROWS, resultList);
//      this.setBizResult(context, biz);
//      return;
//    }
//  }

  private String createQuery(List<SubCriteria> andQueryCriteria) {
    return andQueryCriteria.stream().map(sc -> {
      return "(" + sc.ors.stream().map((or) -> or.getName() + ":" + or.getValue()).collect(Collectors.joining(" OR ")) + ")";
    }).collect(Collectors.joining(" AND "));
  }

  @Override
  public AppDomainInfo getAppDomain() {
    Application application = this.getApplicationDAO().selectByName(this.getCollectionName());
    if (application == null) {
      throw new IllegalStateException("indexName:" + indexName + " relevant app can not be null");
    }
    return new AppDomainInfo(0, application.getAppId(), RunEnvironment.getSysRuntime(), application);
  }


  private TargetColumnMeta getTargetColumnMeta(
    IPluginContext pluginContext, Context context, JSONObject post, String targetTable, PluginItems dataSourceItems)
    throws TableNotFoundException {
    TargetColumnMeta columnMeta = new TargetColumnMeta(targetTable);
    Map<String, ColumnMetaData> colMetas = null;
    for (AttrValMap vals : dataSourceItems.items) {
      if (!vals.validate(this, context, false).isValid()) {
        return columnMeta.invalid();
      }
      DataSourceFactory dsFactory = (DataSourceFactory) vals.createDescribable(pluginContext).getInstance();
      List<ColumnMetaData> tableMetadata = null;
      tableMetadata = dsFactory.getTableMetadata(false, EntityName.parse(targetTable));

      colMetas = tableMetadata.stream().collect(Collectors.toMap((m) -> m.getKey(), (m) -> m));
      break;
    }
    Objects.requireNonNull(colMetas, "colMetas can not null");

    Map<String, TargetCol> targetColMap = getTargetCols(post);
    columnMeta.targetColMap = targetColMap;
    ColumnMetaData colMeta = null;

    for (Map.Entry<String, TargetCol> tc : targetColMap.entrySet()) {
      colMeta = colMetas.get(tc.getKey());
      if (colMeta == null) {
        throw new IllegalStateException("target col:" + tc.getKey() + " is not exist in table:" + targetTable + " meta cols"
          + colMetas.values().stream().map((c) -> c.getKey()).collect(Collectors.joining(",")));
      }
      columnMeta.targetColMetas.add(colMeta);
    }
    return columnMeta;
  }

  public static class TargetColumnMeta {
    private final String tableName;

    public TargetColumnMeta(String tableName) {
      this.tableName = tableName;
    }

    boolean valid = true;
    private Map<String, TargetCol> targetColMap = null;
    final List<ColumnMetaData> targetColMetas = Lists.newArrayList();
    //  ref: com.pingcap.tikv.types.MySQLType
//    static final int TypeTimestamp = 7;
//    static final int TypeDatetime = 12;
//    static final int TypeDate = 10;

    /**
     * 目前只取得一个
     *
     * @return
     */
    public ColumnMetaData getPKMeta() {
      List<ColumnMetaData> pks = targetColMetas.stream().filter((c) -> c.isPk()).collect(Collectors.toList());
      if (pks.size() > 1) {
        throw new IllegalStateException("table:" + tableName + "'s pk col can not much than 1,now is:"
          + pks.stream().map((r) -> r.getKey()).collect(Collectors.joining(",")));
      }
      if (pks.size() < 1) {
        throw new IllegalStateException("table:" + tableName + " can not find pk");
      }
      for (ColumnMetaData pk : pks) {
        TargetCol targetCol = targetColMap.get(pk.getKey());
        Objects.requireNonNull(targetCol, "pk:" + pk.getKey() + " is not in target cols:"
          + targetColMap.values().stream().map((r) -> r.getName()).collect(Collectors.joining(",")));
        return pk;
      }
      throw new IllegalStateException("can not find primary key");
    }

    public Map<String, ColMetaTuple> getTargetCols() {
      Objects.requireNonNull(targetColMap, "targetColMap can not be bull");
      return this.targetColMetas.stream().collect(
        Collectors.toMap((c) -> c.getKey(), (c) -> new ColMetaTuple(targetColMap.get(c.getKey()), c)));
      //   return this.targetColMap;
    }


    private TargetColumnMeta invalid() {
      this.valid = false;
      return this;
    }


  }


  /**
   * 创建索引实例
   *
   * @param context
   * @param df
   * @param indexName
   * @param targetColMetas
   * @throws Exception
   */
  private Optional<Application> createCollection(Context context, WorkFlow df, String indexName, TargetColumnMeta targetColMetas) throws Exception {
    Objects.requireNonNull(df, "param df can not be null");
    CreateIndexConfirmModel confirmModel = new CreateIndexConfirmModel();
    SelectableServer.ServerNodeTopology coreNode = new SelectableServer.ServerNodeTopology();

    SelectableServer.CoreNode[] coreNodeInfo
      = SelectableServer.getCoreNodeInfo(this.getRequest(), this, false, true);

    //FIXME 这一步应该是去掉的最终提交的host内容应该是一个ip格式的，应该是取getNodeName的内容，UI中的内容应该要改一下
    for (SelectableServer.CoreNode n : coreNodeInfo) {
      n.setHostName(n.getNodeName());
    }
    coreNode.setReplicaCount(1);
    coreNode.setShardCount(SHARED_COUNT);
    coreNode.setHosts(coreNodeInfo);

    confirmModel.setCoreNode(coreNode);
    confirmModel.setTplAppId(getTemplateApp(this).getAppId());
    ExtendApp extendApp = new ExtendApp();
    extendApp.setDptId(SysInitializeAction.DEPARTMENT_DEFAULT_ID);
    extendApp.setName(indexName);
    extendApp.setRecept(this.getUser().getName());
    Objects.requireNonNull(df.getId(), "id of dataflow can not be null");
    extendApp.setWorkflow(df.getId() + ":" + df.getName());

    confirmModel.setAppform(extendApp);

    SchemaResult schemaResult = SchemaAction.mergeWfColsWithTplCollection(this
      , context, null, ISchemaPluginContext.NULL, (cols, schemaParseResult) -> {
        ColumnMetaData pkMeta = targetColMetas.getPKMeta();
        PSchemaField field = null;
        ColMetaTuple rft = null;
        TargetCol tcol = null;
        final Map<String, ColMetaTuple> targetCols = targetColMetas.getTargetCols();
        for (ISchemaField f : schemaParseResult.getSchemaFields()) {
          field = (PSchemaField) f;

          rft = targetCols.get(f.getName());
          if (rft == null) {
            throw new IllegalStateException("field:" + f.getName() + " relevant reflect 'SchemaFieldType' can not be null");
          }

          boolean isPk = false;
          if (StringUtils.equals(pkMeta.getKey(), field.getName())) {
            // 设置主键
            isPk = true;
            field.setIndexed(true);
            field.setType(schemaParseResult.getTisType(ReflectSchemaFieldType.STRING.literia));
          } else {
            field.setType(schemaParseResult.getTisType(rft.getSchemaFieldType()));
          }
          tcol = targetColMetas.targetColMap.get(field.getName());
          if (tcol != null) {
            if (tcol.isIndexable()) {
              field.setIndexed(true);
            }

            if (rft.colMeta.getSchemaFieldType().tokenizer) {
              if (StringUtils.isNotEmpty(tcol.getToken())) {
                field.setTokenizerType(tcol.getToken());
              } else {
                // 主键不需要分词
                if (!isPk && rft.isTypeOf(ReflectSchemaFieldType.STRING)) {
                  // String类型默认使用like分词
                  field.setTokenizerType(ReflectSchemaFieldType.LIKE.literia);
                }
              }
            }
          }
        }

        schemaParseResult.setUniqueKey(pkMeta.getKey());
        schemaParseResult.setSharedKey(pkMeta.getKey());
      });

    // 创建索引实例
    return this.createCollection(context, confirmModel, schemaResult
      , (ctx, app, publishSnapshotId, schemaContent) -> {
        return this.createNewApp(ctx, app, publishSnapshotId, schemaContent);
      });
  }

  private SqlTaskNodeMeta.SqlDataFlowTopology createTopology(
    String topologyName, OfflineManager.ProcessedTable dsTable, TargetColumnMeta targetColMetas) throws Exception {
    SqlTaskNodeMeta.SqlDataFlowTopology topology = new SqlTaskNodeMeta.SqlDataFlowTopology();
    SqlTaskNodeMeta.TopologyProfile profile = new SqlTaskNodeMeta.TopologyProfile();
    profile.setName(topologyName);
    profile.setTimestamp(System.currentTimeMillis());
    topology.setProfile(profile);

    DependencyNode dNode = createDumpNode(dsTable);
    topology.addDumpTab(dNode);

    SqlTaskNodeMeta joinNodeMeta = new SqlTaskNodeMeta();
    joinNodeMeta.setId(String.valueOf(UUID.randomUUID()));
    joinNodeMeta.addDependency(dNode);
    joinNodeMeta.setExportName(topologyName);
    joinNodeMeta.setType(NodeType.JOINER_SQL.getType());
    joinNodeMeta.setPosition(DEFAULT_SINGLE_JOINER_POSITION);

    joinNodeMeta.setSql(ColumnMetaData.buildExtractSQL(
      dsTable.getName(), true, targetColMetas.targetColMetas).toString());

    topology.addNodeMeta(joinNodeMeta);
    ERRules.createErRule(topologyName, createDumpNode(dsTable), targetColMetas.getPKMeta());

    // topology

    return topology;
  }

  private DependencyNode createDumpNode(OfflineManager.ProcessedTable dsTable) {
    DependencyNode dNode = new DependencyNode();
    dNode.setId(String.valueOf(UUID.randomUUID()));
    dNode.setDbName(dsTable.getDBName());
    dNode.setName(dsTable.getName());
    dNode.setDbid(String.valueOf(dsTable.getDbId()));
    dNode.setTabid(String.valueOf(dsTable.getId()));
    dNode.setExtraSql(dsTable.getExtraSql());
    dNode.setPosition(DEFAULT_SINGLE_TABLE_POSITION);
    dNode.setType(NodeType.DUMP.getType());
    return dNode;
  }

  private Map<String, TargetCol> getTargetCols(JSONObject post) {
    ThreadLocal<SnapshotViewImplDAO.MergeData> mergeDataContext = SnapshotViewImplDAO.mergeDataContext;
    mergeDataContext.remove();
    SnapshotViewImplDAO.MergeData mergeData = mergeDataContext.get();
    JSONObject colMeta = post.getJSONObject("colMeta");
    JSONObject options = colMeta.getJSONObject("options");
    if (options != null) {
      List<String> acceptKeys = this.getSnapshotViewDAO().getOptionParamKeys();
      options.forEach((key, val) -> {
        if (!acceptKeys.contains(key)) {
          throw new IllegalArgumentException("key:" + key + " is not acceptable,params:"
            + acceptKeys.stream().collect(Collectors.joining(",")));
        }
        mergeData.put(key, val);
      });
    }
    JSONArray targetCols = colMeta.getJSONArray("columns");
    Map<String, TargetCol> targetColMap = targetCols.stream().map((c) -> {
      JSONObject o = (JSONObject) c;
      TargetCol targetCol = new TargetCol(o.getString("name"));
      Boolean indexable = o.getBoolean("search");
      targetCol.setIndexable(indexable == null ? true : indexable);
      targetCol.setToken(o.getString("parser"));
      return targetCol;
    }).collect(Collectors.toMap((c) -> c.getName(), (c) -> c));
    return targetColMap;
  }

  /**
   * 创建增量同步通道
   *
   * @param incrCfg
   */
  private boolean createIncrSyncChannel(Context context //, WorkFlow df
    , JSONObject incrCfg) throws Exception {

    // 生成DAO脚本
    HeteroEnum pluginType = HeteroEnum.MQ;
    UploadPluginMeta pluginMeta = UploadPluginMeta.parse(pluginType.identity + ":" + UploadPluginMeta.KEY_REQUIRE);
    PluginItems incrPluginItems = getPluginItems(incrCfg, pluginType, pluginMeta);
    if (incrPluginItems.items.size() < 1) {
      throw new IllegalStateException("incr plugin item size can not small than 1");
    }

    for (AttrValMap vals : incrPluginItems.items) {
      if (!vals.validate(this, context, false).isValid()) {
        // return columnMeta.invalid();
        return false;
      }
      // MQListenerFactory mqListenerFactory = (MQListenerFactory) vals.createDescribable().instance;
      break;
    }
    incrPluginItems.save(context);

    /**=======================================
     *开始生成脚本并且编译打包
     *=======================================*/
    // SqlTaskNodeMeta.SqlDataFlowTopology wfTopology = SqlTaskNodeMeta.getSqlDataFlowTopology(df.getName());

    IndexIncrStatus incrStatus = CoreAction.generateDAOAndIncrScript(
      this, context, true, true, false);

    if (context.hasErrors()) {
      return false;
    }

    ReplicasSpec incrPodSpec = new ReplicasSpec();
    //FIXME 目前先写死
    incrPodSpec.setReplicaCount(1);
    incrPodSpec.setMemoryRequest(Specification.parse("1G"));
    incrPodSpec.setMemoryLimit(Specification.parse("2G"));
    incrPodSpec.setCpuRequest(Specification.parse("500m"));
    incrPodSpec.setCpuLimit(Specification.parse("1"));

//    IncrUtils.IncrSpecResult applySpec = IncrUtils.parseIncrSpec(context, this.parseJsonPost(), this);
//    if (!applySpec.isSuccess()) {
//      return;
//    }
    // 将打包好的构建，发布到k8s集群中去
    // https://github.com/kubernetes-client/java
    TISK8sDelegate k8sClient = TISK8sDelegate.getK8SDelegate(this.getCollectionName());
    // 通过k8s发布
    k8sClient.deploy(incrPodSpec, incrStatus.getIncrScriptTimestamp());
    return true;
  }

  private PluginItems getDataSourceItems(JSONObject datasource) {
    HeteroEnum pluginType = HeteroEnum.DATASOURCE;
    UploadPluginMeta pluginMeta = UploadPluginMeta.parse(pluginType.identity
      + ":" + UploadPluginMeta.KEY_REQUIRE + "," + PostedDSProp.KEY_TYPE + "_detailed,update_false");
    return getPluginItems(datasource, pluginType, pluginMeta);
  }


  private PluginItems getPluginItems(JSONObject pluginCfg, HeteroEnum pluginType, UploadPluginMeta pluginMeta) {
    Map<String, String> dsParams = Maps.newHashMap();
    for (String dsKey : pluginCfg.keySet()) {
      dsParams.put(dsKey, pluginCfg.getString(dsKey));
    }
    List<Descriptor<?>> descriptorList = TIS.get().getDescriptorList((Class) pluginType.extensionPoint);
    final String plugin = dsParams.remove("plugin");
    if (StringUtils.isEmpty(plugin)) {
      throw new IllegalStateException("pluginCfg/plugin can not be null");
    }
    Optional<Descriptor<?>> pluginDesc
      = descriptorList.stream().filter((des) -> plugin.equals(des.getDisplayName())).findFirst();
    Descriptor<?> dsDescriptpr = null;
    if (!pluginDesc.isPresent()) {
      throw new IllegalStateException("plugin:'" + plugin + "' relevant plugin descriper can not be null");
    }
    dsDescriptpr = pluginDesc.get();


    PluginItems items = new PluginItems(new DftPluginContext(pluginType), pluginMeta);
    JSONArray itemsArray = new JSONArray();
    JSONObject item = new JSONObject();
    JSONObject vals = new JSONObject();
    JSONObject val = null;
    for (Map.Entry<String, String> p : dsParams.entrySet()) {
      val = new JSONObject();
      val.put(Descriptor.KEY_primaryVal, p.getValue());
      vals.put(p.getKey(), val);
    }
    item.put(AttrValMap.PLUGIN_EXTENSION_IMPL, dsDescriptpr.getId());
    item.put(AttrValMap.PLUGIN_EXTENSION_VALS, vals);
    itemsArray.add(item);
    items.items = AttrValMap.describableAttrValMapList(itemsArray, pluginMeta.getSubFormFilter());
    return items;
  }


  private static class TargetCol {
    private final String name;
    private String token;
    private boolean indexable;

    public TargetCol(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public boolean isIndexable() {
      return indexable;
    }

    public void setIndexable(boolean indexable) {
      this.indexable = indexable;
    }
  }

  private static class ColMetaTuple {
    public final TargetCol targetCol;
    public final ColumnMetaData colMeta;

    public ColMetaTuple(TargetCol targetCol, ColumnMetaData colMeta) {
      if (targetCol == null) {
        throw new IllegalArgumentException("targetCol can not be null");
      }
      if (colMeta == null) {
        throw new IllegalArgumentException("colMeta can not be null");
      }
      this.targetCol = targetCol;
      this.colMeta = colMeta;
    }

    public String getSchemaFieldType() {
      return colMeta.getSchemaFieldType().type.literia;
    }

    public boolean isTypeOf(ReflectSchemaFieldType type) {
      return colMeta.getSchemaFieldType().type == type;
    }

  }

  @Autowired
  public void setOfflineManager(OfflineManager offlineManager) {
    this.offlineManager = offlineManager;
  }

  private class DftPluginContext implements IPluginContext {

    private final HeteroEnum pluginType;

    public DftPluginContext(HeteroEnum pluginType) {
      this.pluginType = pluginType;
    }

    @Override
    public boolean isCollectionAware() {
      return this.pluginType == HeteroEnum.MQ;
    }

    @Override
    public String getCollectionName() {
      return CollectionAction.this.getCollectionName();
    }

    @Override
    public boolean isDataSourceAware() {
      return pluginType == HeteroEnum.DATASOURCE;
    }

    @Override
    public void errorsPageShow(Context context) {

    }

    @Override
    public String getExecId() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addActionMessage(Context context, String msg) {

    }

    @Override
    public void setBizResult(Context context, Object result, boolean overwriteable) {
      CollectionAction.this.setBizResult(context, result, overwriteable);
    }

    @Override
    public void addErrorMessage(Context context, String msg) {

    }

    @Override
    public void addDb(Descriptor.ParseDescribable<DataSourceFactory> dbDesc, String dbName, Context context, boolean shallUpdateDB) {
      // CollectionAction.this.
      DatasourceDbCriteria criteria = new DatasourceDbCriteria();
      criteria.createCriteria().andNameEqualTo(dbName);
      int exist = CollectionAction.this.getWorkflowDAOFacade().getDatasourceDbDAO().countByExample(criteria);
      // 如果数据库已经存在则直接跳过
      if (exist > 0) {
        for (DatasourceDb db : CollectionAction.this.getWorkflowDAOFacade()
          .getDatasourceDbDAO().selectByExample(criteria)) {
          CollectionAction.this.setBizResult(context, offlineManager.getDbConfig(CollectionAction.this, db));
          return;
        }
      }
      if (shallUpdateDB) {
        PluginAction.createDatabase(CollectionAction.this, dbDesc, dbName, context, true, offlineManager);
      }

    }
  }

  private static class LogReader implements ILogListener {
    private final StringBuffer logContent = new StringBuffer();

    @Override
    public void sendMsg2Client(Object biz) throws IOException {

    }

    @Override
    public void read(Object event) {
      final PExecuteState state = (PExecuteState) event;
      //System.out.println(state.getMsg());
      logContent.append(state.getMsg()).append("\n");
    }

    @Override
    public boolean isClosed() {
      return false;
    }
  }

  public static class IndexName {
    private final String param;
    private final String collectionName;

    public IndexName(String param) {
      this.param = param;
      this.collectionName = TISCollectionUtils.NAME_PREFIX + param;
    }

    public String getCollectionName() {
      return this.collectionName;
    }
  }

}
