///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.runtime.pojo;
//
//import com.alibaba.citrus.turbine.Context;
//import com.alibaba.fastjson.annotation.JSONField;
//import com.qlangtech.tis.manage.biz.dal.pojo.Application;
//import com.qlangtech.tis.manage.biz.dal.pojo.Department;
//import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
//import com.qlangtech.tis.manage.common.*;
//import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
//
//import com.qlangtech.tis.openapi.impl.AppKey;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import com.qlangtech.tis.runtime.module.action.BasicModule;
//import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
//import com.qlangtech.tis.solrdao.ISchemaPluginContext;
//import junit.framework.Assert;
//import name.fraser.neil.plaintext.diff_match_patch;
//import name.fraser.neil.plaintext.diff_match_patch.Diff;
//import name.fraser.neil.plaintext.diff_match_patch.Operation;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.StringEscapeUtils;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.json.JSONTokener;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.*;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2013-1-16
// */
//public final class ResSynManager {
//
//  public static final String ERROR_MSG_SCHEMA_TITLE = "更新流程中用DTD来校验XML的合法性，请先在文档头部添加<br/>“&lt;!DOCTYPE schema SYSTEM   &quot;solrres://tisrepository/dtd/solrschema.dtd&quot;&gt;”<br/>";
//
//  private final SnapshotDomain dailyRes;
//
//  private final SnapshotDomain onlineResource;
//
//  private final List<ResSyn> compareResults;
//
//  private final String collectionName;
//
//  private final RunContext runContext;
//
//  private static final Logger logger = LoggerFactory.getLogger(ResSynManager.class);
//
//  /**
//   * 创建从日常更新配置同步到线上管理器(在日常执行)
//   *
//   * @param appName
//   * @param runContext
//   * @return
//   * @throws Exception
//   */
//  public static ResSynManager createSynManagerOnlineFromDaily(String appName, RunContext runContext) throws Exception {
//    final AppKey appKey = new AppKey(appName, (short) 0, RunEnvironment.DAILY, true);
//    // 取得内容从数据库中拿
//    appKey.setFromCache(false);
//    // 日常向线上推送的文件
//    SnapshotDomain dailyResDomain = LoadSolrCoreConfigByAppNameServlet.getSnapshotDomain(ConfigFileReader.getConfigList(), appKey, runContext);
//    if (dailyResDomain == null) {
//      throw new IllegalStateException("appName:" + appName + " is not exist");
//    }
//    // 线上的配置
//    SnapshotDomain onlineRes = ResSynManager.getOnlineResourceConfig(appName);
//    return create(appName, dailyResDomain, onlineRes, runContext);
//  }
//
//  public static ResSynManager create(String collectionName, SnapshotDomain dailyRes, SnapshotDomain onlineRes, RunContext runContext) throws RepositoryException {
//    return new ResSynManager(collectionName, dailyRes, onlineRes, runContext);
//  }
//
//  private ResSynManager(String collectionName, SnapshotDomain dailyResource, SnapshotDomain onlineResource, RunContext runContext) throws RepositoryException {
//    this.runContext = runContext;
//    // 取得daily环境中的配置文件信息
//    Assert.assertNotNull(dailyResource);
//    this.dailyRes = dailyResource;
//    this.onlineResource = onlineResource;
//    this.compareResults = new ArrayList<ResSyn>();
//    this.collectionName = collectionName;
//    for (PropteryGetter getter : ConfigFileReader.getAry) {
//      // daily中没有资源的话则退出
//      if (getter.getUploadResource(dailyResource) == null) {
//        continue;
//      }
//      this.compareResults.add(new ResSyn(getter.getFileName(), getter.getUploadResource(dailyResource), (onlineResource == null) ? null : getter.getUploadResource(onlineResource), getter));
//    }
//    dmp = new diff_match_patch();
//  }
//
//  /**
//   * 取得daily日常环境下的配置文件(不需要合并全局参数)
//   *
//   * @param appName
//   * @return
//   * @throws RepositoryException
//   */
//  public static SnapshotDomain getOnlineResourceConfig(String appName) throws RepositoryException {
//    throw new UnsupportedOperationException();
//    // return HttpConfigFileReader.getResource(Config.getTerminatorRepositoryOnline(), appName, 0, /* groupIndex */
//    // RunEnvironment.ONLINE, true, /* unmergeglobalparams */
//    // false, /* reThrowNewException */
//    // ConfigFileReader.getAry);
//  }
//
//  /**
//   * 取得daily环境中应用名称的suggest
//   *
//   * @param appNamePrefix
//   * @return
//   */
//  public static List<Application> appSuggest(String appNamePrefix) {
//    final StringBuffer urlbuffer = new StringBuffer(Config.getTerminatorRepositoryOnline());
//    // StringBuffer urlbuffer = new StringBuffer("http://localhost");
//    urlbuffer.append("/config/changedomain.action?action=app_relevant_action&event_submit_do_app_name_suggest=y&query=");
//    urlbuffer.append(appNamePrefix);
//    URL requestUrl;
//    try {
//      requestUrl = new URL(urlbuffer.toString());
//    } catch (MalformedURLException e) {
//      throw new RuntimeException(e);
//    }
//    return ConfigFileContext.processContent(requestUrl, new StreamProcess<List<Application>>() {
//
//      @Override
//      public List<Application> p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//        List<Application> suggest = new ArrayList<Application>();
//        try {
//          JSONTokener tokener = new JSONTokener(IOUtils.toString(stream));
//          JSONObject json = new JSONObject(tokener);
//          JSONArray dataAry = (JSONArray) json.get("data");
//          JSONArray suggestAry = (JSONArray) json.get("suggestions");
//          for (int i = 0; i < dataAry.length(); i++) {
//            Application app = new Application();
//            app.setAppId(dataAry.getInt(i));
//            app.setProjectName(suggestAry.getString(i));
//            suggest.add(app);
//          }
//        } catch (Exception e) {
//          throw new RuntimeException(e);
//        }
//        return suggest;
//      }
//    });
//  }
//
//  @JSONField(serialize = false)
//  public SnapshotDomain getDailyRes() {
//    return dailyRes;
//  }
//
//  @JSONField(serialize = false)
//  public SnapshotDomain getOnlineResDomain() {
//    return onlineResource;
//  }
//
//  /**
//   * 执行将日常配置推送到线上的操作
//   *
//   * @param context
//   * @param module
//   * @return
//   * @throws UnsupportedEncodingException
//   * @throws SchemaFileInvalidException
//   */
//  public Boolean getSynchronizedOnlineSnapshot(final Context context, final BasicModule module) throws Exception {
//    final SnapshotDomain onlineResDomain = this.getOnlineResDomain();
//    List<ResSyn> reslist = this.getCompareResult();
//    List<ResSyn> pushResource = new ArrayList<ResSyn>();
//    for (ResSyn res : reslist) {
//      if (!res.isSame()) {
//        // 若日常和线上相等，则该资源就不推送了
//        pushResource.add(res);
//        continue;
//      }
//      // pushResource.add(res);
//    }
//    if (pushResource.size() < 1) {
//      module.addErrorMessage(context, "all config resource has been updated already");
//      return false;
//    }
//    // return snapshot;
//    UploadResource push = null;
//    List<UploadResource> uploadResources = new ArrayList<UploadResource>();
//    for (ResSyn res : pushResource) {
//      if (res.getDaily() == null) {
//        continue;
//      }
//      push = new UploadResource();
//      push.setResourceType(res.getGetter().getFileName());
//      push.setContent(res.getDaily().getContent());
//      push.setMd5Code(res.getDaily().getMd5Code());
//      uploadResources.add(push);
//    }
//    ConfigPush configPush = new ConfigPush();
//    configPush.setCollection(this.collectionName);
//    configPush.setSnapshotId(this.getDailyRes().getSnapshot().getSnId());
//    if (this.getOnlineResDomain() != null) {
//      configPush.setRemoteSnapshotId(this.getOnlineResDomain().getSnapshot().getSnId());
//    }
//    configPush.setUploadResources(uploadResources);
//    if (onlineResDomain == null) {
//      // 说明线上还没有第一次发布
//      Department dpt = new Department();
//      // dpt.setDptId();
//      // dpt.setFullName();
//      Application app = this.runContext.getApplicationDAO().loadFromWriteDB(this.getDailyRes().getAppId());
//      dpt.setFullName(app.getDptName());
//      configPush.setDepartment(dpt);
//      configPush.setReception(app.getRecept());
//    } else {
//      configPush.setReception(this.getDailyRes().getSnapshot().getCreateUserName());
//    }
//    Writer writer = null;
//    try {
//      ByteArrayOutputStream out = new ByteArrayOutputStream();
//      writer = new OutputStreamWriter(out, BasicModule.getEncode());
//      HttpConfigFileReader.xstream.toXML(configPush, writer);
//      writer.flush();
//      // System.out.println(new String(out.toByteArray(),
//      // BasicModule.getEncode()));
//      final URL url = new URL(Config.getTerminatorRepositoryOnline() + "/config/config.ajax?action=app_syn_action&event_submit_do_init_app_from_daily=y");
//      return HttpUtils.post(url, out.toByteArray(), new PostFormStreamProcess<Boolean>() {
//
//        @Override
//        public ContentType getContentType() {
//          return ContentType.Multipart_byteranges;
//        }
//
//        @Override
//        public Boolean p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//          try {
//            String result = IOUtils.toString(stream, BasicModule.getEncode());
//            JSONTokener tokener = new JSONTokener(result);
//            JSONObject json = new JSONObject(tokener);
//            JSONArray errs = json.getJSONArray("errormsg");
//            if (errs.length() > 0) {
//              for (int i = 0; i < errs.length(); i++) {
//                module.addErrorMessage(context, errs.getString(i));
//              }
//              return false;
//            }
//            JSONArray msg = json.getJSONArray("msg");
//            return true;
//          } catch (Exception e) {
//            throw new RuntimeException(e);
//          }
//        }
//      });
//    } finally {
//      IOUtils.closeQuietly(writer);
//    }
//  }
//
//  /**
//   * @param context
//   * @param uploadContent
//   * @param md5
//   * @param fileGetter
//   * @param messageHandler
//   * @param runContext
//   * @return 新创建的主键
//   * @throws UnsupportedEncodingException
//   * @throws SchemaFileInvalidException
//   */
//  public static Integer createNewResource(Context context, ISchemaPluginContext schemaPluginContext, final byte[] uploadContent, final String md5
//    , PropteryGetter fileGetter, IMessageHandler messageHandler, RunContext runContext) throws SchemaFileInvalidException {
//    UploadResource resource = new UploadResource();
//    resource.setContent(uploadContent);
//    resource.setCreateTime(new Date());
//    resource.setResourceType(fileGetter.getFileName());
//    resource.setMd5Code(md5);
//    ConfigFileValidateResult validateResult = fileGetter.validate(schemaPluginContext, resource);
//    // 校验文件格式是否正确，通用用DTD来校验
//    if (!validateResult.isValid()) {
//      messageHandler.addErrorMessage(context, ERROR_MSG_SCHEMA_TITLE);
//      messageHandler.addErrorMessage(context, validateResult.getValidateResult());
//      throw new SchemaFileInvalidException(validateResult.getValidateResult());
//    }
//    return runContext.getUploadResourceDAO().insertSelective(resource);
//  }
//
//  public List<ResSyn> getCompareResult() {
//    return this.compareResults;
//  }
//
//  /**
//   * 需要同步吗？
//   *
//   * @return
//   */
//  public boolean shallSynchronize() {
//    if (this.dailyRes != null && this.onlineResource == null) {
//      return true;
//    }
//    for (ResSyn res : getCompareResult()) {
//      if (!res.isSame()) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  public static class CompareResult {
//
//    private final PropteryGetter getter;
//
//    private final StringBuffer result = new StringBuffer();
//
//    public CompareResult(PropteryGetter getter) {
//      super();
//      this.getter = getter;
//    }
//
//    public String getFileName() {
//      return getter.getFileName();
//    }
//
//    public String getHtmlDiffer() {
//      return result.toString();
//    }
//  }
//
//  private final diff_match_patch dmp;
//
//  private boolean hasDifferentRes = false;
//
//  // 是否有不一样的资源
//  public boolean isHasDifferentRes() {
//    return this.hasDifferentRes && this.compareResults.size() > 0;
//  }
//
//  /**
//   * 比较两组配置的内容的区别
//   *
//   * @return
//   */
//  public List<CompareResult> diff() {
//    final List<CompareResult> result = new ArrayList<CompareResult>();
//    try {
//      for (ResSyn res : getCompareResult()) {
//        if (res.isSame()) {
//          continue;
//        }
//        this.hasDifferentRes = true;
//        CompareResult compare = new CompareResult(res.getGetter());
//        // if (ConfigFileReader.FILE_JAR.getFileName().equals(res.getGetter().getFileName())) {
//        // compare.result.append("内容不同");
//        // result.add(compare);
//        // continue;
//        // }
//        LinkedList<Diff> differ = dmp.diff_main(new String(res.getDaily().getContent(), BasicModule.getEncode()), new String(res.getOnline().getContent(), BasicModule.getEncode()), true);
//        for (Diff d : differ) {
//          if (d.operation == Operation.EQUAL) {
//            compare.result.append(StringEscapeUtils.escapeXml(d.text));
//          } else if (d.operation == Operation.DELETE) {
//            compare.result.append("<span style='text-decoration:line-through;background-color:pink;'>").append(StringEscapeUtils.escapeXml(d.text)).append("</span>");
//          } else if (d.operation == Operation.INSERT) {
//            compare.result.append("<span style=\"background-color:#00FF00;\">").append(StringEscapeUtils.escapeXml(d.text)).append("</span>");
//          }
//        }
//        result.add(compare);
//      }
//    } catch (UnsupportedEncodingException e) {
//      throw new RuntimeException(e);
//    }
//    return result;
//  }
//
//  // private static ServerGroup getServerGroup0(Integer appId,
//  // RunEnvironment environment, RunContext context) {
//  // ServerGroupCriteria query = new ServerGroupCriteria();
//  // query.createCriteria().andAppIdEqualTo(appId)
//  // .andRuntEnvironmentEqualTo(environment.getId())
//  // .andGroupIndexEqualTo((short) 0);
//  //
//  // List<ServerGroup> groupList = context.getServerGroupDAO()
//  // .selectByExample(query);
//  // for (ServerGroup group : groupList) {
//  // return group;
//  // }
//  //
//  // return null;
//  // }
//  public static void main(String[] ar) throws Exception {
//    List<Application> applist = ResSynManager.appSuggest("search4sucai");
//    for (Application app : applist) {
//      System.out.println(app.getProjectName());
//    }
//  }
//}
