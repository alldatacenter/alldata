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

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opensymphony.xwork2.ModelDriven;
import com.qlangtech.tis.coredefine.biz.FCoreRequest;
import com.qlangtech.tis.coredefine.module.action.CoreAction;
import com.qlangtech.tis.coredefine.module.control.SelectableServer;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.fullbuild.indexbuild.LuceneVersion;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.ISolrAppSource;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.pojo.*;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria.Criteria;
import com.qlangtech.tis.manage.common.BasicDAO;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.apps.AppsFetcher.CriteriaSetter;
import com.qlangtech.tis.manage.common.apps.IAppsFetcher;
import com.qlangtech.tis.manage.common.ibatis.BooleanYorNConvertCallback;
import com.qlangtech.tis.manage.impl.DataFlowAppSource;
import com.qlangtech.tis.manage.servlet.DownloadServlet;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.offline.module.manager.impl.OfflineManager;
import com.qlangtech.tis.openapi.impl.AppKey;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.solrdao.ISchemaPluginContext;
import com.qlangtech.tis.solrdao.SchemaResult;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.qlangtech.tis.manage.servlet.LoadSolrCoreConfigByAppNameServlet;

/**
 * 添加应用
 * order2
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-4-1
 */
public class AddAppAction extends SchemaAction implements ModelDriven<Application> {
  // tab or df
  public static String SOURCE_TYPE_SINGLE_TABLE = "tab";
  public static String SOURCE_TYPE_DF = "df";
  // 单元测试用，生产环境中不需要使用
  public static AppKey.IAppKeyProcess appKeyProcess = (key) -> {
  };

  private static final long serialVersionUID = 1L;

  // private ITerminatorTriggerBizDalDAOFacade triggerContext;
  private OfflineManager offlineManager;

  public static final int FIRST_GROUP_INDEX = 0;

  private final Application app = new Application();

  @Override
  public Application getModel() {
    return app;
  }

  /**
   * 创建业务流程中所需要的系统表信息，在這個方法中全部取得
   *
   * @param context
   * @throws Exception
   */
  @Func(PermissionConstant.APP_ADD)
  public void doGetCreateAppMasterData(Context context) throws Exception {
    Map<String, Object> masterData = Maps.newHashMap();
    boolean justbizLine = this.getBoolean("justbizLine");

    masterData.put("bizlinelist", this.getBizLineList());

    if (!justbizLine) {
      final List<Option> verList = new ArrayList<>();
      for (LuceneVersion v : LuceneVersion.values()) {
        verList.add(new Option(v.getKey(), v.getKey()));
      }
      masterData.put("tplenum", verList);
      masterData.put("usableWorkflow", this.offlineManager.getUsableWorkflow());
    }
    this.setBizResult(context, masterData);
  }

  @Func(value = PermissionConstant.APP_ADD, sideEffect = false)
  public void doGetSelectableNodeList(Context context) throws Exception {
    SelectableServer.CoreNode[] nodes = SelectableServer.getCoreNodeInfo(this.getRequest(), this, false, false);
    this.setBizResult(context, nodes);
  }

  /**
   * 创建索引，不在数据库中添加记录<br>
   * 当一个索引创建之后又被删除了，又需要重新创建就需要需执行该流程了
   *
   * @param context
   * @throws Exception
   */
  @Func(PermissionConstant.APP_ADD)
  public void doCreateCollection(Context context) throws Exception {
    CreateIndexConfirmModel confiemModel = parseJsonPost(CreateIndexConfirmModel.class);
    confiemModel.setTplAppId(getTemplateApp(this).getAppId());
    SchemaResult schemaResult = this.parseSchema(context, ISchemaPluginContext.NULL, confiemModel);

    this.createCollection(context, confiemModel, schemaResult, (ctx, app, publishSnapshotId, schemaContent) -> {
      CreateSnapshotResult result = new CreateSnapshotResult();
      result.setSuccess(true);
      Application a = getApplicationDAO().selectByName(app.getProjectName());
      if (a == null) {
        throw new IllegalStateException("appname:" + app.getProjectName() + " relevant app can not be find in DB");
      }
      result.setNewSnapshotId(getPublishSnapshotId(this.getServerGroupDAO(), a));
      return result;
    });
  }

  /**
   * 高级添加,会在数据库中添加对象记录
   *
   * @param context
   */
  @Func(PermissionConstant.APP_ADD)
  public void doAdvanceAddApp(Context context) throws Exception {
    CreateIndexConfirmModel confiemModel = parseJsonPost(CreateIndexConfirmModel.class);
    confiemModel.setTplAppId(getTemplateApp(this).getAppId());
    SchemaResult schemaResult = this.parseSchema(context, ISchemaPluginContext.NULL, confiemModel);
    if (!createNewApp(context, confiemModel.getAppform(), -1, /**
       * publishSnapshotId
       */
      null, /* schemaContent */
      true).isSuccess()) {
      // 只作表单校验 表单校验不通过
      return;
    }
    this.createCollection(context, confiemModel, schemaResult, (ctx, app, publishSnapshotId, schemaContent) -> {
      return this.createNewApp(ctx, app, publishSnapshotId, schemaContent);
    });
  }

  protected Optional<Application> createCollection(Context context, CreateIndexConfirmModel confiemModel
    , SchemaResult schemaResult, ICreateNewApp appCreator) throws Exception {
    ExtendApp extApp = confiemModel.getAppform();
    appendPrefix(extApp);
    String workflow = confiemModel.getAppform().getWorkflow();
    if (StringUtils.isBlank(workflow)) {
      this.addErrorMessage(context, "缺少全量数据流信息");
      return Optional.empty();
    }
    final String[] candidateNodeIps = confiemModel.getCoreNodeCandidate();
    if (candidateNodeIps == null || candidateNodeIps.length < 1) {
      this.addErrorMessage(context, "请选择引擎节点");
      return Optional.empty();
    }
    if (!schemaResult.isSuccess()) {
      return Optional.empty();
    }
    Application app = new Application();
    app.setAppId(confiemModel.getTplAppId());
    Integer publishSnapshotId = getPublishSnapshotId(this.getServerGroupDAO(), app);
    byte[] content = schemaResult.content;
    SelectableServer.ServerNodeTopology coreNode = confiemModel.getCoreNode();
    final int gourpCount = coreNode.getShardCount();
    int repliation = coreNode.getReplicaCount();
    // 由于是在日常环境中，默认就是设置为 1*1
    FCoreRequest request = new FCoreRequest(CoreAction.createIps(context, extApp.getProjectName(), candidateNodeIps), gourpCount);
    for (String ip : candidateNodeIps) {
      request.addNodeIps(gourpCount - 1, ip);
    }
    request.setValid(true);
    CreateAppResult createResult = appCreator.createNewApp(context, extApp, publishSnapshotId, content);
    if (!createResult.isSuccess()) {
      return Optional.of(app);
    }

    IAppSource.save(this, extApp.getProjectName(), extApp.createAppSource(this));
    /**
     * *************************************************************************************
     * 因为这里数据库的事务还没有提交，需要先将schema配置信息保存到缓存中去以便solrcore节点能获取到
     * 设置缓存
     * **************************************************************************************
     */
    final AppKey appKey = AppKey.create(extApp.getProjectName());
//      new AppKey(extApp.getProjectName(), /* appName ========== */
//      (short) 0, /* groupIndex */
//      RunEnvironment.getSysRuntime(), false);

    if (!(createResult instanceof CreateSnapshotResult)) {
      throw new IllegalStateException("instance of createResult must be CreateSnapshotResult");
    }
    CreateSnapshotResult snapshotResult = (CreateSnapshotResult) createResult;
    appKey.setTargetSnapshotId((long) snapshotResult.getNewId());
    appKey.setFromCache(false);
    appKeyProcess.process(appKey);
    // LoadSolrCoreConfigByAppNameServlet.getSnapshotDomain(ConfigFileReader.getConfigList(), appKey, this);
    CoreAction.createCollection(this, context, gourpCount, repliation, request, snapshotResult.getNewId());
    return Optional.of(app);
  }

  public interface ICreateNewApp {

    public CreateAppResult createNewApp(Context context, ExtendApp app, int publishSnapshotId, byte[] schemaContent) throws Exception;
  }

  /**
   * 校验添加新索引第一步提交的form表单
   *
   * @param context
   */
  @Func(value = PermissionConstant.APP_ADD, sideEffect = false)
  public void doValidateAppForm(Context context) throws Exception {
    this.errorsPageShow(context);
    ExtendApp app = this.parseJsonPost(ExtendApp.class);
    this.appendPrefix(app);
    this.createNewApp(context, app, -1, /* publishSnapshotId */
      null, /* schemaContent */
      true);
  }

  private void appendPrefix(ExtendApp app) {
    if (StringUtils.isNotBlank(app.getProjectName())) {
      app.setProjectName("search4" + app.getProjectName());
    }
  }

//  public static CreateSnapshotResult createNewSnapshot(Context context, final SnapshotDomain domain, PropteryGetter fileGetter
//    , ISchemaPluginContext schemaPlugin, byte[] uploadContent, BasicModule module, String memo, Long userId, String userName) {
//    CreateSnapshotResult createResult = new CreateSnapshotResult();
//    final String md5 = MD5Utils.md5file(uploadContent);
//    // 创建一条资源记录
//    try {
//
//      Integer newResId = ResSynManager.createNewResource(context, schemaPlugin, uploadContent, md5, fileGetter, module, module);
//      //  Integer newResId = createNewResource(context, schemaPluginContext, uploadContent, md5, fileGetter, module);
//      final Snapshot snapshot = fileGetter.createNewSnapshot(newResId, domain.getSnapshot());
//      snapshot.setMemo(memo);
//      createResult.setNewSnapshotId(createNewSnapshot(snapshot, memo, module, userId, userName));
//      snapshot.setSnId(createResult.getNewId());
//      context.put("snapshot", snapshot);
//    } catch (SchemaFileInvalidException e) {
//      return createResult;
//    }
//    createResult.setSuccess(true);
//    return createResult;
//  }

  /**
   * 仅仅创建 Application相关的數據表
   *
   * @param context
   * @param app
   */
  protected CreateAppResult createNewApp(Context context, ExtendApp app, int publishSnapshotId, byte[] schemaContent) throws Exception {
    return this.createNewApp(context, app, publishSnapshotId, schemaContent, false);
  }


  private static // BasicModule module
  Integer createNewSnapshot(// BasicModule module
                            final Snapshot snapshot, // BasicModule module
                            final String memo, // BasicModule module
                            RunContext runContext, // BasicModule module
                            Long userid, String userName) {
    Integer newId;
    snapshot.setSnId(null);
    snapshot.setUpdateTime(new Date());
    snapshot.setCreateTime(new Date());
    // snapshot.setCreateUserName();
    try {
      snapshot.setCreateUserId(userid);
    } catch (Throwable e) {
      snapshot.setCreateUserId(0l);
    }
    snapshot.setCreateUserName(userName);
    if (StringUtils.isNotEmpty(memo)) {
      snapshot.setMemo(memo);
    }
    // 插入一条新纪录
    newId = runContext.getSnapshotDAO().insertSelective(snapshot);
    if (newId == null) {
      throw new IllegalArgumentException(" have not create a new snapshot id");
    }
    return newId;
  }

  /**
   * 删除一条记录
   *
   * @param context
   */
  @Func(PermissionConstant.APP_DELETE)
  public void doDelete(Context context) {
    Integer appid = this.getInt("appid");
    Assert.assertNotNull("appid can not be null", appid);
    // 需要判断在solr cluster中是否存在
    // DocCollection collection = this.getZkStateReader().getClusterState().getCollectionOrNull(this.getAppDomain().getAppName());
//    if (collection != null) {
//      this.addErrorMessage(context, "集群中存在索引实例“" + collection.getName() + "”，请先联系管理员将该实例删除");
//      return;
//    }
    rescycleAppDB(appid);
    this.addActionMessage(context, "索引实例“" + this.getAppDomain().getAppName() + "”该应用被成功删除");
  }

  protected void rescycleAppDB(Integer appid) {
    if (appid < 1) {
      throw new IllegalArgumentException("appid can not empty");
    }
    ServerGroupCriteria criteria = new ServerGroupCriteria();
    criteria.createCriteria().andAppIdEqualTo(appid);
    // group 表删除
    for (ServerGroup sg : this.getServerGroupDAO().selectByExample(criteria)) {
      this.getServerGroupDAO().deleteByPrimaryKey(sg.getGid());
    }

    SnapshotCriteria snCriteria = new SnapshotCriteria();
    snCriteria.createCriteria().andAppidEqualTo(appid);
    Set<Long> resIds = Sets.newHashSet();
    for (Snapshot snapshot : this.getSnapshotDAO().selectByExample(snCriteria)) {
      resIds.add(snapshot.getResSchemaId());
      // 先不删除 solrConfig的配置，很有可能把 模版配置里面的solrconfig也删除掉的
      //  resIds.add(snapshot.getResSolrId());
      this.getSnapshotDAO().deleteByPrimaryKey(snapshot.getSnId());
    }
    UploadResourceCriteria urCriteria = new UploadResourceCriteria();
    urCriteria.createCriteria().andUrIdIn(Lists.newArrayList(resIds));
    this.getUploadResourceDAO().deleteByExample(urCriteria);

    AppTriggerJobRelationCriteria acriteria = new AppTriggerJobRelationCriteria();
    acriteria.createCriteria().andAppIdEqualTo(appid);
    // 触发表刪除
    this.getAppTriggerJobRelationDAO().deleteByExample(acriteria);
    this.getApplicationDAO().deleteByPrimaryKey(appid);
  }

  /**
   * @param app
   * @param context
   * @param module
   * @param
   * @return
   * @throws Exception
   */
  public static CreateAppResult createApplication(Application app //, Integer tplPublishSnapshotId, byte[] schemaContent
    , Context context, BasicModule module, IAfterApplicationCreate afterAppCreate) throws Exception {
    final Integer newAppid = module.getApplicationDAO().insertSelective(app);
    // IUser loginUser = module.getUser();
    CreateAppResult snapshotResult = new CreateSnapshotResult();
    snapshotResult.setNewAppId(newAppid);
    snapshotResult.setSuccess(true);

    SchemaAction.CreateAppResult createAppResult = null;
    if (!(createAppResult = afterAppCreate.process(newAppid)).isSuccess()) {
      return createAppResult;
    }

//    if (schemaContent != null) {
//      SnapshotDomain domain = module.getSnapshotViewDAO().getView(tplPublishSnapshotId);
//      domain.getSnapshot().setAppId(newAppid);
//      snapshotResult = createNewSnapshot(// Long.parseLong(loginUser.getId())
//        context, // Long.parseLong(loginUser.getId())
//        domain, // Long.parseLong(loginUser.getId())
//        ConfigFileReader.FILE_SCHEMA, // Long.parseLong(loginUser.getId())
//        ISchemaPluginContext.NULL,
//        schemaContent, // Long.parseLong(loginUser.getId())
//        module, // Long.parseLong(loginUser.getId())
//        StringUtils.EMPTY, -1l, loginUser.getName());
//      snapshotResult.setNewAppId(newAppid);
//      if (!snapshotResult.isSuccess()) {
//        return snapshotResult;
//      }
//    }
    int offset = (int) (Math.random() * 10);
    // TriggerAction.createJob(newAppid, context, "0 0 " + offset + " * * ?", JobConstant.JOB_TYPE_FULL_DUMP, module, triggerContext);
    // TriggerAction.createJob(newAppid, context, "0 0/10 * * * ?", JobConstant.JOB_INCREASE_DUMP, module, triggerContext);
    // 创建默认组和服务器
//    GroupAction.createGroup(RunEnvironment.DAILY, FIRST_GROUP_INDEX, newAppid, snapshotResult.getNewId(), module.getServerGroupDAO());
//    GroupAction.createGroup(RunEnvironment.ONLINE, FIRST_GROUP_INDEX, newAppid, snapshotResult.getNewId(), module.getServerGroupDAO());
    return snapshotResult;
  }

  public static class IpsValidateResult {

    private final Map<String, String> ipsValidateResult = new HashMap<String, String>();

    public String put(String key, String value) {
      return ipsValidateResult.put(key, value);
    }

    private boolean success = true;

    public boolean isSuccess() {
      return success;
    }

    public boolean isValidate(String ip) {
      Matcher matcher = pattern.matcher(StringUtils.trimToEmpty(ipsValidateResult.get(ip)));
      return matcher.matches();
      // return StringUtils.isNumeric(StringUtils.substringAfter(
      // , "version:"));
    }

    public String getMsg(String ip) {
      return ipsValidateResult.get(ip);
    }
  }

  private static final Pattern pattern = Pattern.compile("version:\\d+");

  public static class AddReplic {

    private String appname;

    private int groupCount;

    private Integer replica;

    private RunEnvironment runtime;

    private List<String> servers;

    public AddReplic(String appname, int groupCount, Integer replica, RunEnvironment runtime, List<String> servers) {
      super();
      this.appname = appname;
      this.groupCount = groupCount;
      this.replica = replica;
      this.runtime = runtime;
      this.servers = servers;
    }

    // public void setClientProtocol(CoreManagerClient manageClient) {
    //
    // }
    public void publishNewCore() {
    }

    public Map<String, Set<String>> createPublishNewCoreParam() {
      return Collections.emptyMap();
    }
  }

  public static Department getDepartment(RunContext runcontext, Integer dptId) {
    // Department department = null;
    DepartmentCriteria criteria = new DepartmentCriteria();
    criteria.createCriteria().andDptIdEqualTo(dptId);
    for (Department dpt : runcontext.getDepartmentDAO().selectByExample(criteria)) {
      return dpt;
    }
    throw new IllegalArgumentException("dptId:" + dptId + " can not find any department obj");
  }

  /**
   * 从其他应用拷贝配置文件
   *
   * @param context
   */
  @Func(PermissionConstant.CONFIG_UPLOAD)
  public void doCopyConfigFromOtherApp(Context context) {
    // 拷贝源
    Integer fromAppId = this.getInt("hiddenAppnamesuggest");
    if (fromAppId == null) {
      fromAppId = this.getInt("combAppid");
    }
    Assert.assertNotNull("fromAppId can not be null", fromAppId);
    Application fromApp = this.getApplicationDAO().loadFromWriteDB(fromAppId);
    if (fromApp == null) {
      this.addErrorMessage(context, "拷贝源应用已经删除，请重新选择");
      return;
    }
    // 拷贝目的地
    Integer toAppId = this.getInt("toAppId");
    Assert.assertNotNull("toAppId can not be null", toAppId);
    Application destinationApp = this.getApplicationDAO().loadFromWriteDB(toAppId);
    if (destinationApp == null) {
      this.addErrorMessage(context, "拷贝目标应用已经删除");
      return;
    }
    final ServerGroup group = DownloadServlet.getServerGroup(fromAppId
      , (new Integer(FIRST_GROUP_INDEX)).shortValue(), this.getAppDomain().getRunEnvironment().getId(), getServerGroupDAO());
    if (group == null) {
      this.addErrorMessage(context, "拷贝目标应用还没有定义组");
      return;
    }
    if (group.getPublishSnapshotId() == null) {
      this.addErrorMessage(context, "拷贝目标应用还没有设定配置文件版本");
      return;
    }
    // 开始插入配置文件
    // 先插入snapshot
    Snapshot newSnapshto = this.getSnapshotDAO().loadFromWriteDB(group.getPublishSnapshotId());
    newSnapshto.setCreateTime(new Date());
    newSnapshto.setUpdateTime(new Date());
    newSnapshto.setPreSnId(newSnapshto.getSnId());
    newSnapshto.setSnId(null);
    newSnapshto.setAppId(toAppId);
    newSnapshto.setMemo("从应用“" + fromApp.getProjectName() + "” 拷贝而来");
    try {
      newSnapshto.setCreateUserId(Long.parseLong(this.getUserId()));
    } catch (Throwable e) {
    }
    newSnapshto.setCreateUserName(this.getLoginUserName());
    // final Integer newsnapshotId =
    this.getSnapshotDAO().insertSelective(newSnapshto);

    this.addActionMessage(context, "拷贝源应用“" + fromApp.getProjectName() + "”已经成功复制到目标应用“" + destinationApp.getProjectName() + "”");
  }


  /**
   * 更新应用
   */
  @Func(PermissionConstant.APP_UPDATE)
  public // Navigator nav,
  void doUpdate(Context context) {
    Application form = new Application();
    context.put("app", form);
    Integer bizid = this.getInt("bizid");
    final Integer appid = this.getInt("appid");
    Assert.assertNotNull(bizid);
    Assert.assertNotNull(appid);
    form.setAppId(appid);
    DepartmentCriteria criteria = new DepartmentCriteria();
    criteria.createCriteria().andDptIdEqualTo(bizid).andLeafEqualTo(BooleanYorNConvertCallback.YES);
    List<Department> depatment = this.getDepartmentDAO().selectByExample(criteria);
    Assert.assertTrue("dptid:" + bizid + " depatment can not be null ", depatment.size() == 1);
    for (Department d : depatment) {
      form.setDptId(d.getDptId());
      form.setDptName(d.getFullName());
    }
    form.setProjectName(this.getString(SchemaAction.FIELD_PROJECT_NAME));
    form.setRecept(this.getString("recept"));
    if (!isAppNameValid(this, context, SchemaAction.FIELD_PROJECT_NAME, form)) {
      return;
    }
    // 是否使用自动部署新方案
    form.setIsAutoDeploy("true".equalsIgnoreCase(this.getString("isautodeploy")));
    if (!validateAppForm(context, form)) {
      return;
    }
    IAppsFetcher fetcher = getAppsFetcher();
    fetcher.update(form, new CriteriaSetter() {

      @Override
      public void set(Criteria criteria) {
        criteria.andAppIdEqualTo(appid);
      }
    });
    this.addActionMessage(context, "已经成功更新应用[" + form.getProjectName() + "]");
  }

  // private boolean setYuntiPath(IYuntiPath yuntipath, Context context) {
  // if ("true".equals(this.getString("yunti"))) {
  // String yuntiPath = this.getString("yuntiPath");
  // if (StringUtils.isEmpty(yuntiPath)) {
  // this.addErrorMessage(context, "请填写云梯路径");
  // return false;
  // }
  //
  // String yuntiToken = this.getString("yuntiToken");
  // yuntipath.setYuntiPath(
  // YuntiPathInfo.createYuntiPathInfo(yuntiPath, yuntiToken));
  // } else {
  // // 设置为空字符串
  // yuntipath.setYuntiPath(StringUtils.EMPTY);
  // }
  //
  // return true;
  // }
  // @Autowired
  // public void setTerminatorTriggerBizDalDaoFacade(ITerminatorTriggerBizDalDAOFacade triggerDaoContext) {
  // this.triggerContext = triggerDaoContext;
  // }
  // private static final Pattern APPNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  public static void main(String[] arg) throws Exception {
//    System.out.println("search4realjhsItemtest");
//    Matcher m = APPNAME_PATTERN.matcher("search4realj_hsItemtest");
//    System.out.println(m.matches());
  }

  public static boolean isAppNameValid(IFieldErrorHandler msgHandler, Context context, String fieldKey, Application form) {
//    Matcher m = APPNAME_PATTERN.matcher(form.getProjectName());
//    if (!m.matches()) {
//      msgHandler.addFieldError(context, fieldKey, "必须用小写字母或大写字母数字组成");
//      return false;
//    }
//    return true;
    boolean valid = Validator.identity.validate(msgHandler, context, fieldKey, form.getProjectName());
    if (!valid) {
      return false;
    }
    if (StringUtils.indexOf(form.getProjectName(), "-") > -1) {
      msgHandler.addFieldError(context, fieldKey, "名称中不能有'-'字符");
      return false;
    }

    return true;
  }

  private boolean validateAppForm(Context context, Application app) {
    if (StringUtils.isBlank(app.getProjectName())) {
      this.addErrorMessage(context, "索引名称不能为空");
      return false;
    }
    if (StringUtils.isBlank(app.getRecept())) {
      this.addErrorMessage(context, "接口人不能为空");
      return false;
    }
    return true;
  }


  public static class ExtendApp extends Application {

    private static final long serialVersionUID = 1L;

    private List<Option> selectableDepartment;
    // format:  [ "196", "169%employees" ]
    private String[] tabCascadervalues;

    private String dsType;
    private String workflow;

    @JSONField(serialize = false)
    @Override
    public Integer getWorkFlowId() {
      return Integer.parseInt(StringUtils.substringBefore(this.workflow, ":"));
    }

    public void setName(String name) {
      this.setProjectName(name);
    }

    public void setWorkflow(String val) {
      this.workflow = val;
    }

    public ISolrAppSource createAppSource(BasicModule module) {
      if (AddAppAction.SOURCE_TYPE_SINGLE_TABLE.equals(this.getDsType())) {
//        String[] tabCascadervalues = this.getTabCascadervalues();
//        if (tabCascadervalues == null) {
//          throw new IllegalStateException("tabCascadervalues can not be null");
//        }
//        //[ "196", "169%employees"];
//        Integer dbId = Integer.parseInt(tabCascadervalues[0]);
//        String[] pair = StringUtils.split(tabCascadervalues[1], "%");
//        Integer tabId = Integer.parseInt(pair[0]);
//        String tabName = StringUtils.trimToEmpty(pair[1]);
//        DatasourceTable table = null;// module.wfDAOFacade.getDatasourceTableDAO().loadFromWriteDB(tabId);
//        DatasourceDb db = module.wfDAOFacade.getDatasourceDbDAO().loadFromWriteDB(dbId);
//        return new SingleTableAppSource(db, table);
        throw new UnsupportedOperationException();
      } else if (AddAppAction.SOURCE_TYPE_DF.equals(this.getDsType())) {
        String workflowName = this.getWorkflow();
        if (StringUtils.isEmpty(workflowName)) {
          throw new IllegalStateException("workflowName can not be null");
        }
        String wfName = StringUtils.split(workflowName, ":")[1];
        Integer wfId = Integer.parseInt(StringUtils.split(workflowName, ":")[0]);
        IDataxProcessor process = DataxProcessor.load(module, StoreResourceType.DataFlow, wfName);
        return new DataFlowAppSource(module.loadDF(wfId), process.getWriter(module, true));
      }
      throw new IllegalStateException("dsType:" + this.getDsType() + " is not illegal");
    }

    public String[] getTabCascadervalues() {
      return tabCascadervalues;
    }

    public void setTabCascadervalues(String[] tabCascadervalues) {
      this.tabCascadervalues = tabCascadervalues;
    }

    public String getDsType() {
      return dsType;
    }

    public void setDsType(String dsType) {
      this.dsType = dsType;
    }

    public List<Option> getSelectableDepartment() {
      return selectableDepartment;
    }

    public void setSelectableDepartment(List<Option> selectableDepartment) {
      this.selectableDepartment = selectableDepartment;
    }

    public String getWorkflow() {
      return workflow;
    }
  }

  @Autowired
  public void setOfflineManager(OfflineManager offlineManager) {
    this.offlineManager = offlineManager;
  }

  @SuppressWarnings("all")
  public void doCopyFromOtherIndex(Context context) throws Exception {
    final String colonFrom = this.getString("appname");
    if (StringUtils.startsWith(colonFrom, "search4")) {
      throw new IllegalArgumentException("colonFrom:" + colonFrom + " is not start with 'search4'");
    }
    BasicDAO<Application, ApplicationCriteria> basicDao = (BasicDAO<Application, ApplicationCriteria>) this.getApplicationDAO();
    DataSource dataSource = (DataSource) basicDao.getDataSource();
    Connection conn = dataSource.getConnection();
    PreparedStatement statement = conn.prepareStatement("select app_id from application where project_name = ?");
    statement.setString(1, colonFrom);
    Integer oldAppId = null;
    ResultSet result = statement.executeQuery();
    if (result.next()) {
      oldAppId = result.getInt(1);
    }
    result.close();
    statement.close();
    Assert.assertNotNull(oldAppId);

    String insertSql = "insert into application(project_name,recept,manager,create_time,update_time" + ", is_auto_deploy, dpt_id,  dpt_name ) "
      + "select  concat('search4', 'N', SUBSTRING(project_name,8)) as project_name "
      + ",recept,manager,create_time,update_time "
      + ",'Y' as is_auto_deploy, 8 as dpt_id, '淘宝网-产品技术部-综合业务平台-互动业务平台-终搜' as dpt_name "
      + "from application where app_id = ?";

    statement = conn.prepareStatement(insertSql);
    statement.setInt(1, oldAppId);
    Assert.assertTrue(statement.execute());
    statement.close();
    statement = conn.prepareStatement(" SELECT LAST_INSERT_ID()");
    result = statement.executeQuery();
    Integer newAppId = null;
    if (result.next()) {
      newAppId = result.getInt(1);
    }
    Assert.assertNotNull(newAppId);
    result.close();
    statement.close();
    statement = conn.prepareStatement("insert into"
      + "snapshot(create_time,update_time,app_id,res_schema_id ,res_solr_id ,res_jar_id ,res_core_prop_id ,res_ds_id ,res_application_id ,pre_sn_id)"
      + " select create_time,update_time,? as app_id,res_schema_id ,res_solr_id ,res_jar_id ,res_core_prop_id ,res_ds_id ,res_application_id ,pre_sn_id"
      + "   from snapshot "
      + " where sn_id in (select publish_snapshot_id from server_group where publish_snapshot_id is not null and app_id = ?)");
    statement.setInt(1, newAppId);
    statement.setInt(2, oldAppId);
    statement.execute();
    statement.close();
    conn.close();
  }
}
