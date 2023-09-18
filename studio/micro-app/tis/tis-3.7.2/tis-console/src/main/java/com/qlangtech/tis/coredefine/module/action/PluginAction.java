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
package com.qlangtech.tis.coredefine.module.action;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.koubei.web.tag.pager.Pager;
import com.opensymphony.xwork2.ActionContext;
import com.qlangtech.tis.IPluginEnum;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.extension.impl.RootFormProperties;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.extension.util.TextFile;
import com.qlangtech.tis.install.InstallState;
import com.qlangtech.tis.install.InstallUtil;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.offline.module.manager.impl.OfflineManager;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.IPluginTaggable;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.util.*;
import com.qlangtech.tis.workflow.pojo.DatasourceDb;
import com.qlangtech.tis.workflow.pojo.DatasourceDbCriteria;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.Parameter;
import org.apache.struts2.dispatcher.multipart.UploadedFile;
import org.apache.struts2.interceptor.FileUploadInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@InterceptorRefs({@InterceptorRef("tisStack")})
public class PluginAction extends BasicModule {
  private static final Logger logger = LoggerFactory.getLogger(PluginAction.class);
  private OfflineManager offlineManager;

  static {

    PluginItems.addPluginItemsSaveObserver((new PluginItems.PluginItemsSaveObserver() {
      // 通知Assemble节点更新pluginStore的缓存
      @Override
      public void afterSaved(PluginItems.PluginItemsSaveEvent event) {
        final String extendPoint = event.heteroEnum.getExtensionPoint().getName();
        // @see "com.qlangtech.tis.fullbuild.servlet.TaskStatusServlet"
        notifyPluginUpdate2AssembleNode(DescriptorsJSON.KEY_EXTEND_POINT + "=" + extendPoint, "pluginStore");
      }
    }));
  }

  private static void notifyPluginUpdate2AssembleNode(String applyParams, String targetResource) {
    long start = System.currentTimeMillis();
    try {

      URL url = new URL(Config.getAssembleHttpHost() + "/task_status?" + applyParams);
      HttpUtils.get(url, new ConfigFileContext.StreamProcess<Void>() {
        @Override
        public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
          logger.info("has apply clean " + targetResource + " cache by " + applyParams);
          return null;
        }
      });
    } catch (Exception e) {
      logger.warn("apply clean " + targetResource + ",consume:" + (System.currentTimeMillis() - start) + "ms, cache faild " + e.getMessage());
    }
  }

  /**
   * 通过之前缓存在服务端的NotebookEntry实例对象打开notebook
   *
   * @param context
   * @throws Exception
   */
  public void doGetOrCreateNotebook(Context context) throws Exception {
    DataxProcessor dataxProcessor = IAppSource.load(this, this.getAppDomain().getAppName());
    String pluginIdVal = this.getString("pluginIdVal");
    if (StringUtils.isEmpty(pluginIdVal)) {
      throw new IllegalArgumentException("param pluginIdVal can not be null");
    }
    Map<String, INotebookable.NotebookEntry> notebooks = dataxProcessor.scanNotebook();
    INotebookable.NotebookEntry notebookEntry = notebooks.get(pluginIdVal);
    Objects.requireNonNull(notebookEntry, "pluginId:" + pluginIdVal + " relevant notebookEntry can not be null");
    this.setBizResult(context, notebookEntry.createOrGetNotebook());
  }

  /**
   * @param context
   */
  public void doScanNotebooks(Context context) throws Exception {
    String dataxName = this.getAppDomain().getAppName();

    DataxProcessor dataxProcessor = IAppSource.load(this, dataxName);
    Map<String, INotebookable.NotebookEntry> notebooks = dataxProcessor.scanNotebook();
    String pluginIdVal = null;
    DescriptorsJSON descJson = null;
    List<Descriptor> descs = Lists.newArrayList();
    INotebookable.NotebookEntry note = null;
    List<Map<String, Object>> notebookProps = Lists.newArrayList();
    Map<String, Object> props = null;
    for (Map.Entry<String, INotebookable.NotebookEntry> entry : notebooks.entrySet()) {
      pluginIdVal = entry.getKey();
      note = entry.getValue();

      props = new HashMap<>(note.getDescriptor().getExtractProps());
      props.put("pluginId", pluginIdVal);
      props.put("displayName", note.getDescriptor().getDisplayName());
      notebookProps.add(props);
    }
    this.setBizResult(context, notebookProps);
  }


  /**
   * 为表单中提交临时文件
   *
   * @param context
   * @see FileUploadInterceptor
   */
  public void doUploadFile(Context context) {
    final String inputName = "file";
    final String fileNameName = inputName + "FileName";
    ActionContext ac = ActionContext.getContext();
    HttpParameters parameters = ac.getParameters();

    Parameter.File file = (Parameter.File) parameters.get(inputName);
    UploadedFile[] uploades = (UploadedFile[]) file.getObject();
    for (UploadedFile f : uploades) {
      java.io.File tmpFile = new java.io.File(f.getAbsolutePath());
      java.io.File renameTo = new java.io.File(tmpFile.getParentFile(), f.getName() + "_tmp");
      tmpFile.renameTo(renameTo);
      this.setBizResult(context, Collections.singletonMap(inputName, renameTo.getAbsolutePath()));
      return;
    }

    throw new IllegalStateException(" have not receive any upload file,inputName:" + inputName);
  }

  /**
   * @param context
   */
  public void doCreateOrGetNotebook(Context context) {

  }

  /**
   * 刷新多选字段内容
   *
   * @param context
   */
  public void doGetFreshEnumField(Context context) {
    DescriptorField descField = parseDescField();
    List<Descriptor.SelectOption> options = null;
    if (descField.getFieldPropType().typeIdentity() == FormFieldType.SELECTABLE.getIdentity()) {
      options = DescriptorsJSON.getSelectOptions(
        descField.getTargetDesc(), descField.getFieldPropType(), descField.field);
      this.setBizResult(context, options);
    } else if (descField.getFieldPropType().typeIdentity() == FormFieldType.ENUM.getIdentity()) {
      this.setBizResult(context
        , descField.getFieldPropType().getExtraProps().getJSONArray(Descriptor.KEY_ENUM_PROP));
    }
  }

  private static class DescriptorField {
    final String pluginImpl;
    final String field;

    public DescriptorField(String pluginImpl, String field) {
      this.pluginImpl = pluginImpl;
      this.field = field;
    }

    Descriptor getTargetDesc() {
      return TIS.get().getDescriptor(this.pluginImpl);
    }

    PropertyType getFieldPropType() {
      return (PropertyType) getTargetDesc().getPropertyType(this.field);
    }
  }

  private DescriptorField parseDescField() {
    String pluginImpl = this.getString("impl");
    String fieldName = this.getString("field");
    if (StringUtils.isEmpty(pluginImpl)) {
      throw new IllegalArgumentException("param 'impl' can not be null");
    }
    if (StringUtils.isEmpty(fieldName)) {
      throw new IllegalArgumentException("param 'field' can not be null");
    }
    return new DescriptorField(pluginImpl, fieldName);
  }

  /**
   * 取得字段的帮助信息
   *
   * @param context
   */
  public void doGetPluginFieldHelp(Context context) {
    DescriptorField descField = parseDescField();

    String plugin = this.getString("plugin");
    Optional<IPropertyType.SubFormFilter> subFormFilter = Optional.empty();
    if (StringUtils.isNotEmpty(plugin)) {
      UploadPluginMeta pluginMeta
        = UploadPluginMeta.parse(this, plugin, true);
      subFormFilter = pluginMeta.getSubFormFilter();
    }


    PluginFormProperties pluginFormPropertyTypes
      = descField.getTargetDesc().getPluginFormPropertyTypes(subFormFilter);

    PluginExtraProps.Props props = pluginFormPropertyTypes.accept(
      new DescriptorsJSON.SubFormFieldVisitor(subFormFilter) {
        @Override
        public PluginExtraProps.Props visit(BaseSubFormProperties props) {
          PropertyType propertyType = props.getPropertyType(descField.field);
          return propertyType.extraProp;
        }

        @Override
        public PluginExtraProps.Props visit(RootFormProperties props) {
          return descField.getFieldPropType().extraProp;
        }
      });


    if (!props.isAsynHelp()) {
      throw new IllegalStateException("plugin:"
        + descField.pluginImpl + ",field:"
        + descField.field + " is not support async help content fecthing");
    }
    this.setBizResult(context, props.getAsynHelp());
  }

  /**
   * 取得安装进度状态
   *
   * @param context
   */
  public void doGetUpdateCenterStatus(Context context) {
    UpdateCenter updateCenter = TIS.get().getUpdateCenter();
    List<UpdateCenter.UpdateCenterJob> jobs = updateCenter.getJobs();
    Collections.sort(jobs, (a, b) -> {
      // 保证最新的安装job排列在最上面
      return b.id - a.id;
    });
    jobs.forEach((job) -> {
      if (job instanceof UpdateCenter.DownloadJob) {
        ((UpdateCenter.DownloadJob) job).status.setUsed();
      }
    });
    setBizResult(context, jobs);
  }

  /**
   * 取得已经安装的插件
   *
   * @param context
   */
  public void doGetInstalledPlugins(Context context) {

    List<String> extendpoint = getExtendpointParam();
    PluginManager pluginManager = TIS.get().getPluginManager();
    JSONArray response = new JSONArray();
    JSONObject pluginInfo = null;
    UpdateSite.Plugin info = null;
    PluginFilter pluginFilter = new PluginFilter();
    for (PluginWrapper plugin : pluginManager.getPlugins()) {


      pluginInfo = new JSONObject();
      pluginInfo.put("installed", true);
      info = plugin.getInfo();
      if (info != null) {
        // pluginInfo.put("meta", info);
        pluginInfo.put("releaseTimestamp", info.releaseTimestamp);
        pluginInfo.put("excerpt", info.excerpt);
      }

      if (CollectionUtils.isNotEmpty(extendpoint)) {
        if (info == null) {
          continue;
        }
//        if (!CollectionUtils.containsAny(info.extendPoints.keySet(), extendpoint)) {
//          continue;
//        }
        pluginInfo.put("extendPoints", info.extendPoints);
      }

      if (pluginFilter.filter(Optional.of(plugin), info)) {
        continue;
      }

      Optional<PluginClassifier> classifier = plugin.getClassifier();
      if (classifier.isPresent()) {
        pluginInfo.put(PluginManager.PACAKGE_CLASSIFIER, classifier.get().getClassifier());
      }

      pluginInfo.put("name", plugin.getShortName());
      pluginInfo.put("version", plugin.getVersion());
      pluginInfo.put("title", plugin.getDisplayName());
      pluginInfo.put("active", plugin.isActive());
      pluginInfo.put("enabled", plugin.isEnabled());
      // pluginInfo.put("bundled", plugin.isBundled);
      pluginInfo.put("deleted", plugin.isDeleted());
      pluginInfo.put("downgradable", plugin.isDowngradable());
      pluginInfo.put("website", plugin.getUrl());
      List<PluginWrapper.Dependency> dependencies = plugin.getDependencies();
      if (dependencies != null && !dependencies.isEmpty()) {
        Option o = null;
        List<Option> dependencyMap = Lists.newArrayList();
        for (PluginWrapper.Dependency dependency : dependencies) {
          o = new Option(dependency.shortName, dependency.version);
          dependencyMap.add(o);
        }
        pluginInfo.put("dependencies", dependencyMap);
      } else {
        pluginInfo.put("dependencies", Collections.emptyList());
      }
      response.add(pluginInfo);
    }
    this.setBizResult(context, response);
  }

  private class PluginFilter {
    final Set<IPluginTaggable.PluginTag> tags;
    final Predicate<UpdateSite.Plugin> endTypeMatcher;
    final List<String> extendpoint;

    public PluginFilter() {
      this.extendpoint = getExtendpointParam();
      final String[] filterTags = getStringArray("tag");
      if (filterTags != null && filterTags.length > 0) {
        this.tags = Sets.newHashSet();
        for (String tag : filterTags) {
          tags.add(IPluginTaggable.PluginTag.parse(tag));
        }
      } else {
        this.tags = null;
      }
      this.endTypeMatcher = getEndTypeMatcher();
    }

    /**
     * @param plugin
     * @param info
     * @return true 就直接过滤掉了
     */
    private boolean filter(Optional<PluginWrapper> plugin, UpdateSite.Plugin info) {
      if (this.tags != null) {
        //  info.pluginTags
        if (!CollectionUtils.containsAny(info.pluginTags, this.tags)) {
          return true;
        }
      }

      if (CollectionUtils.isNotEmpty(extendpoint)) {
        if (!CollectionUtils.containsAny(info.extendPoints.keySet(), extendpoint)) {
          return true;
        }
      }

      boolean collect = endTypeMatcher.test(info);
      if (!collect) {
        return true;
      }
      return filterPlugin((plugin.isPresent() ? plugin.get().getDisplayName() : info.title)
        , (info != null ? info.excerpt : null));
    }
  }


  private boolean filterPlugin(String title, String excerpt) {

    List<String> queries = getQueryPluginParam();
    if (CollectionUtils.isEmpty(queries)) {
      return false;
    }
    boolean collect = false;
    for (String searchQ : queries) {
      if (StringUtils.indexOfIgnoreCase(title, searchQ) > -1
        || (StringUtils.isNotBlank(excerpt) && StringUtils.indexOfIgnoreCase(excerpt, searchQ) > -1)
      ) {
        collect = true;
        break;
      }
    }
    // 收集
    return !collect;
  }

  private List<String> getQueryPluginParam() {
    String[] queries = StringUtils.split(this.getString("query"), " ");
    if (queries == null) {
      return Collections.emptyList();
    }
    return Lists.newArrayList(queries);
  }

  /**
   * 安装插件
   *
   * @param context
   */
  public void doInstallPlugins(Context context) {
    JSONArray pluginsInstall = this.parseJsonArrayPost();
    if (pluginsInstall.size() < 1) {
      this.addErrorMessage(context, "请选择需要安装的插件");
      return;
    }
    long start = System.currentTimeMillis();
    boolean dynamicLoad = true;
    UUID correlationId = UUID.randomUUID();
    UpdateCenter updateCenter = TIS.get().getUpdateCenter();
    List<Future<UpdateCenter.UpdateCenterJob>> installJobs = new ArrayList<>();
    JSONObject willInstall = null;
    String pluginName = null;
    UpdateSite.Plugin plugin = null;
    List<Pair<UpdateSite.Plugin, Optional<PluginClassifier>>> coords = Lists.newArrayList();
    Optional<PluginClassifier> classifier = null;
    String c = null;
    List<PluginWrapper> batch = new ArrayList<>();
    for (int i = 0; i < pluginsInstall.size(); i++) {
      willInstall = pluginsInstall.getJSONObject(i);
      pluginName = willInstall.getString("name");
      classifier = Optional.empty();
      if (StringUtils.isNotEmpty(c = willInstall.getString("selectedClassifier"))) {
        classifier = Optional.of(PluginClassifier.create(c));
      }
      if (willInstall.getBooleanValue("multiClassifier") && !classifier.isPresent()) {
        this.addFieldError(context, pluginName, "请选择安装的版本");
        continue;
      }

      if (StringUtils.isEmpty(pluginName)) {
        throw new IllegalStateException("plugin name can not empty");
      }
      plugin = updateCenter.getPlugin(pluginName);

      coords.add(Pair.of(plugin, classifier));
    }

    if (this.hasErrors(context)) {
      return;
    }

    for (Pair<UpdateSite.Plugin, Optional<PluginClassifier>> coord : coords) {
      Future<UpdateCenter.UpdateCenterJob> installJob = coord.getLeft().deploy(dynamicLoad, correlationId, coord.getRight(), batch);
      installJobs.add(installJob);
    }

    if (dynamicLoad) {
      installJobs.add(updateCenter.addJob(updateCenter.new CompleteBatchJob(batch, start, correlationId)));
    }

    final TIS tis = TIS.get();

    //TODO: 每个安装流程都要进来
    if (true || !tis.getInstallState().isSetupComplete()) {
      tis.setInstallState(InstallState.INITIAL_PLUGINS_INSTALLING);
      updateCenter.persistInstallStatus();
      new Thread() {
        @Override
        public void run() {
          boolean failures = false;
          INSTALLING:
          while (true) {
            try {
              updateCenter.persistInstallStatus();
              Thread.sleep(500);
              failures = false;
              for (Future<UpdateCenter.UpdateCenterJob> jobFuture : installJobs) {
                if (!jobFuture.isDone() && !jobFuture.isCancelled()) {
                  continue INSTALLING;
                }
                UpdateCenter.UpdateCenterJob job = jobFuture.get();
                if (job instanceof UpdateCenter.InstallationJob && ((UpdateCenter.InstallationJob) job).status instanceof UpdateCenter.DownloadJob.Failure) {
                  failures = true;
                }
              }
            } catch (Exception e) {
              logger.warn("Unexpected error while waiting for initial plugin set to install.", e);
            }
            break;
          }
          updateCenter.persistInstallStatus();
          if (!failures) {

            InstallUtil.proceedToNextStateFrom(InstallState.INITIAL_PLUGINS_INSTALLING);
            // 为了让Assemble等节点的uberClassLoader重新加载一次，需要主动向Assemble等节点发送一个指令
            notifyPluginUpdate2AssembleNode(TIS.KEY_ACTION_CLEAN_TIS + "=true", "TIS");
          }
        }
      }.start();
    }
  }


  /**
   * 重新加载updateSite元数据信息
   *
   * @throws Exception
   */
  public void doReloadUpdateSiteMeta(Context context) throws Exception {
    UpdateCenter center = TIS.get().getUpdateCenter();
    for (UpdateSite usite : center.getSiteList()) {
      TextFile textFile = usite.getDataLoadFaildFile();
      if (textFile.exists()) {
        usite.updateDirectly().get();
      }
    }
    this.doGetAvailablePlugins(context);
  }

  /**
   * 取得当前可以被安装的插件
   *
   * @param context
   */
  public void doGetAvailablePlugins(Context context) throws Exception {


    List<String> extendpoint = getExtendpointParam();
    Pager pager = this.createPager();
    pager.setTotalCount(Integer.MAX_VALUE);
    UpdateCenter center = TIS.get().getUpdateCenter();
    List<UpdateSite.Plugin> availables = center.getAvailables();
    if (CollectionUtils.isEmpty(availables)) {
      for (UpdateSite usite : center.getSiteList()) {
        TextFile textFile = usite.getDataLoadFaildFile();
        if (textFile.exists()) {

          Map<String, Object> err = Maps.newHashMap();
          err.put("updateSiteLoadErr", true);
          err.put(IMessageHandler.ACTION_ERROR_MSG, textFile.read());
          this.setBizResult(context, new PaginationResult(pager, availables, err));
          return;
        }
      }
    }

    PluginFilter filter = new PluginFilter();
    availables = availables.stream().filter((plugin) -> {
      return !(filter.filter(Optional.empty(), plugin));
    }).collect(Collectors.toList());


//    if (CollectionUtils.isNotEmpty(extendpoint)) {
//
//      Predicate<UpdateSite.Plugin> endTypeMatcher = getEndTypeMatcher();
//
//      availables = availables.stream().filter((plugin) -> {
//        return CollectionUtils.containsAny(plugin.extendPoints.keySet(), extendpoint);
//      }).filter(endTypeMatcher).collect(Collectors.toList());
//    }
//
//    if (CollectionUtils.isNotEmpty(this.getQueryPluginParam())) {
//      availables = availables.stream().filter((plugin) -> {
//        return !filterPlugin(plugin.title, plugin.excerpt);
//      }).collect(Collectors.toList());
//    }

    this.setBizResult(context, new PaginationResult(pager, availables));
  }

  public Predicate<UpdateSite.Plugin> getEndTypeMatcher() {
    final String endType = this.getString(IEndTypeGetter.KEY_END_TYPE);
    return (plugin) -> {
      if (StringUtils.isEmpty(endType)) {
        // 需要，将会收集
        return true;
      } else {
        IEndTypeGetter.EndType targetEndType = IEndTypeGetter.EndType.parse(endType);
        return targetEndType.containIn(plugin.endTypes);
      }
    };
  }

  private List<String> getExtendpointParam() {
    return Arrays.asList(this.getStringArray("extendpoint"));
  }

  /**
   * @param context
   * @throws Exception
   */
  public void doSwitchExtensionPointShow(Context context) throws Exception {
    boolean open = this.getBoolean("switch");
    TIS tis = TIS.get();
    tis.saveComponent(tis.loadGlobalComponent().setShowExtensionDetail(open));
  }

  public void doGetExtensionPointShow(Context context) throws Exception {
    TIS tis = TIS.get();
    this.setBizResult(context, tis.loadGlobalComponent().isShowExtensionDetail());
  }

  /**
   * @param context
   */
  public void doGetDescriptor(Context context) {
    this.errorsPageShow(context);
    final String displayName = this.getString("name");
    if (StringUtils.isEmpty(displayName)) {
      throw new IllegalArgumentException("request param 'impl' can not be null");
    }
    IPluginEnum hetero = HeteroEnum.of(this.getString("hetero"));
    List<Descriptor<Describable>> descriptors = hetero.descriptors();
    for (Descriptor desc : descriptors) {
      if (StringUtils.equals(desc.getDisplayName(), displayName)) {
        this.setBizResult(context, new DescriptorsJSON(desc).getDescriptorsJSON());
        return;
      }
    }
    this.setBizResult(context, Collections.singletonMap("notFoundExtension", hetero.getExtensionPoint().getName()));
    this.addErrorMessage(context, "displayName:" + displayName + " relevant Descriptor can not be null");

  }

  /**
   * @param context
   */
  public void doGetDescsByExtendpoint(Context context) throws Exception {
    List<String> extendpoints = this.getExtendpointParam();
    if (CollectionUtils.isEmpty(extendpoints)) {
      throw new IllegalArgumentException("extendpoints can not be null");
    }

    for (String extend : extendpoints) {
      this.setBizResult(context
        , new DescriptorsJSON(TIS.get().getDescriptorList((Class<Describable>) Class.forName(extend))).getDescriptorsJSON());
      return;
    }

    throw new IllegalArgumentException("extendpoints can not be null");
  }

  /**
   * plugin form 的子表单的某条详细记录被点击
   *
   * @param context
   * @throws Exception
   */
  public void doSubformDetailedClick(Context context) throws Exception {

    List<UploadPluginMeta> pluginsMeta = getPluginMeta();
    List<Describable> plugins = null;

    IPluginEnum heteroEnum = null;
    HeteroList<?> hList = null;

    for (UploadPluginMeta meta : pluginsMeta) {

      heteroEnum = meta.getHeteroEnum();
      plugins = heteroEnum.getPlugins(this, meta);
      for (Describable plugin : plugins) {

        SuFormProperties.setSuFormGetterContext(plugin, meta, this.getString(SuFormProperties.SuFormGetterContext.FIELD_SUBFORM_ID));

        hList = meta.getHeteroList(this);

        this.setBizResult(context, hList.toJSON());
        return;
      }
    }
    throw new IllegalStateException("have not set plugin meta");
  }

  /**
   * @param context
   * @throws Exception
   */
  public void doGetPluginConfigInfo(Context context) throws Exception {

    HeteroList<?> hetero = null;


    List<UploadPluginMeta> plugins = getPluginMeta();

    if (plugins == null || plugins.size() < 1) {
      throw new IllegalArgumentException("param plugin is not illegal");
    }
    com.alibaba.fastjson.JSONObject pluginDetail = new com.alibaba.fastjson.JSONObject();
    com.alibaba.fastjson.JSONArray hlist = new com.alibaba.fastjson.JSONArray();
    pluginDetail.put("showExtensionPoint", TIS.get().loadGlobalComponent().isShowExtensionDetail());
    for (UploadPluginMeta pmeta : plugins) {

      hetero = this.createHeteroList(pmeta);
      if (!pmeta.isUseCache()) {
        hetero.getItems().forEach((p) -> {
          if (p instanceof Describable.IRefreshable) {
            ((Describable.IRefreshable) p).refresh();
          }
        });
      }
      hlist.add(hetero.toJSON());
    }
    pluginDetail.put("plugins", hlist);
    this.setBizResult(context, pluginDetail);
  }

  private HeteroList<?> createHeteroList(UploadPluginMeta pmeta) {
    return pmeta.getHeteroList(this);
  }

  /**
   * 保存plugin配置
   *
   * @param context
   */
  public void doSavePluginConfig(Context context) throws Exception {
    if (this.getBoolean("errors_page_show")) {
      this.errorsPageShow(context);
    }
    List<UploadPluginMeta> plugins = getPluginMeta();
    JSONObject postData = this.parseJsonPost();
    String[] forwardParams = getActionForwardParam(postData);

    JSONArray pluginArray =
      Objects.requireNonNull(postData.getJSONArray("items"), "json prop items can not be null");
    UploadPluginMeta pluginMeta = null;

    boolean faild = false;
    List<PluginItems> categoryPlugins = Lists.newArrayList();
    final boolean processNotebook = this.getBoolean("getNotebook");
    // 是否进行业务逻辑校验？当正式提交表单时候不进行业务逻辑校验，用户可能先添加一个不存在的数据库配置
    final boolean verify = processNotebook || this.getBoolean("verify");
    PluginItemsParser pluginItemsParser = null;
    for (int pluginIndex = 0; pluginIndex < plugins.size(); pluginIndex++) {

      pluginMeta = plugins.get(pluginIndex);
      JSONArray itemsArray = pluginArray.getJSONArray(pluginIndex);
      pluginItemsParser = parsePluginItems(this, pluginMeta, context, pluginIndex, itemsArray, verify);
      if (pluginItemsParser.faild) {
        faild = true;
      }
      categoryPlugins.add(pluginItemsParser.pluginItems);
    }
    if (this.hasErrors(context) || (verify && !processNotebook)) {
      return;
    }
    if (faild) {
      // 判断提交的plugin表单是否有错误？错误则退出
      this.addErrorMessage(context, "提交表单内容有错误");
      return;
    }

    if (processNotebook) {
      for (PluginItems pi : categoryPlugins) {
        this.setBizResult(context, pi.cerateOrGetNotebook(this, context));
        return;
      }
    }

    List<ItemsSaveResult> describables = Lists.newArrayList();

    for (PluginItems pi : categoryPlugins) {
      describables.add(pi.save(context));
    }

    if (forwardParams != null) {
      this.getRequest().setAttribute(ItemsSaveResult.KEY_ITEMS_SAVE_RESULT, describables);
      getRundata().forwardTo(forwardParams[0], forwardParams[1], forwardParams[2]);
      return;
    }

    addActionMessage(context, "配置保存成功");
    // 成功保存的主键信息返回给客户端
    if (context.get(IMessageHandler.ACTION_BIZ_RESULT) == null) {
      this.setBizResult(context, describables.stream().flatMap((itemSaveResult) -> itemSaveResult.describableList.stream())
        .filter((d) -> d instanceof IdentityName)
        .map((d) -> ((IdentityName) d).identityValue()).collect(Collectors.toList()));
    }
  }

  public static List<ItemsSaveResult> getItemsSaveResultInRequest(HttpServletRequest request) {
    return (List<ItemsSaveResult>) request.getAttribute(ItemsSaveResult.KEY_ITEMS_SAVE_RESULT);
  }

  private String[] getActionForwardParam(JSONObject postData) {
    String serverForward = postData.getString("serverForward");
    String[] forwardParams = null;
    if (StringUtils.isNotEmpty(serverForward)) {
      forwardParams = StringUtils.split(serverForward, ":");
      if (forwardParams.length != 3) {
        throw new IllegalArgumentException("illegal forward param:" + serverForward);
      }
    }
    return forwardParams;
  }


  public static PluginItemsParser parsePluginItems(BasicModule module, UploadPluginMeta pluginMeta
    , Context context, int pluginIndex, JSONArray itemsArray, boolean verify) {
    context.put(UploadPluginMeta.KEY_PLUGIN_META, pluginMeta);
    PluginItemsParser parseResult = new PluginItemsParser();
    List<Descriptor.PluginValidateResult> items = Lists.newArrayList();
    Optional<IPropertyType.SubFormFilter> subFormFilter = pluginMeta.getSubFormFilter();
    Descriptor.PluginValidateResult validateResult = null;
    IPluginEnum hEnum = pluginMeta.getHeteroEnum();
    //context.put(KEY_VALIDATE_PLUGIN_INDEX, new Integer(pluginIndex));
    PluginItems pluginItems = new PluginItems(module, pluginMeta);
    List<AttrValMap> describableAttrValMapList = AttrValMap.describableAttrValMapList(itemsArray, subFormFilter);
    if (pluginMeta.isRequired() && describableAttrValMapList.size() < 1) {
      module.addErrorMessage(context, "请设置'" + hEnum.getCaption() + "'表单内容");
    }


    pluginItems.items = describableAttrValMapList;
    parseResult.pluginItems = pluginItems;
    AttrValMap attrValMap = null;


    for (int itemIndex = 0; itemIndex < describableAttrValMapList.size(); itemIndex++) {
      attrValMap = describableAttrValMapList.get(itemIndex);
      Descriptor.PluginValidateResult.setValidateItemPos(context, pluginIndex, itemIndex);
      if (!(validateResult = attrValMap.validate(module, context, verify)).isValid()) {
        parseResult.faild = true;
      } else {
        validateResult.setDescriptor(attrValMap.descriptor);
        items.add(validateResult);
      }
    }


    /**===============================================
     * 校验Item字段的identity字段不能重复，不然就报错
     ===============================================*/
    Map<String, Descriptor.PluginValidateResult> identityUniqueMap = Maps.newHashMap();

    Descriptor.PluginValidateResult previous = null;
    if (!parseResult.faild && hEnum.isIdentityUnique()
      && hEnum.getSelectable() == Selectable.Multi
      && (items.size() > 1 || pluginMeta.isAppend())) {

      if (pluginMeta.isAppend()) {
        List<IdentityName> plugins = hEnum.getPlugins(module, pluginMeta);
        for (IdentityName p : plugins) {
          Descriptor.PluginValidateResult r = new Descriptor.PluginValidateResult(
            new Descriptor.PostFormVals(AttrValMap.IAttrVals.rootForm(Collections.emptyMap())), 0, 0);
          r.setDescriptor(((Describable) p).getDescriptor());
          identityUniqueMap.put(p.identityValue(), r);
        }
      }

      for (Descriptor.PluginValidateResult i : items) {
        if ((previous = identityUniqueMap.put(i.getIdentityFieldValue(), i)) != null) {
          previous.addIdentityFieldValueDuplicateError(module, context);
          i.addIdentityFieldValueDuplicateError(module, context);
          return parseResult;
        }
      }
    }
    return parseResult;
  }

  public static class PluginItemsParser {
    public boolean faild = false;
    public PluginItems pluginItems;
  }

  private List<UploadPluginMeta> getPluginMeta() {
    final boolean useCache = Boolean.parseBoolean(this.getString("use_cache", "true"));
    return UploadPluginMeta.parse(this, this.getStringArray("plugin"), useCache);
  }

  /**
   * 是否是和数据源相关的流程处理
   *
   * @return
   */
  @Override
  public boolean isDataSourceAware() {
    //return super.isDataSourceAware();
    List<UploadPluginMeta> pluginMeta = getPluginMeta();
    return pluginMeta.size() == 1 && pluginMeta.stream().findFirst().get().getHeteroEnum() == HeteroEnum.DATASOURCE;
  }

  /**
   * description: 添加一个 数据源库 date: 2:30 PM 4/28/2017
   */
  @Override
  public final void addDb(Descriptor.ParseDescribable<DataSourceFactory> dbDesc, String dbName, Context context, boolean shallUpdateDB) {
    createDatabase(this, dbDesc, dbName, context, shallUpdateDB, this.offlineManager);
  }

  public static DatasourceDb createDatabase(BasicModule module, Descriptor.ParseDescribable<DataSourceFactory> dbDesc, String dbName, Context context
    , boolean shallUpdateDB, OfflineManager offlineManager) {
    DatasourceDb datasourceDb = null;
    if (shallUpdateDB) {
      datasourceDb = new DatasourceDb();
      datasourceDb.setName(dbName);
      datasourceDb.setSyncOnline(new Byte("0"));
      datasourceDb.setCreateTime(new Date());
      datasourceDb.setOpTime(new Date());
      Describable plugin = dbDesc.getInstance();
      datasourceDb.setExtendClass(StringUtils.lowerCase(plugin.getDescriptor().getDisplayName()));

      DatasourceDbCriteria criteria = new DatasourceDbCriteria();
      criteria.createCriteria().andNameEqualTo(dbName);
      int exist = module.getWorkflowDAOFacade().getDatasourceDbDAO().countByExample(criteria);
      if (exist > 0) {
        module.addErrorMessage(context, "已经有了同名(" + dbName + ")的数据库");
        return null;
      }
      /**
       * 校验数据库连接是否正常
       */
      int dbId = module.getWorkflowDAOFacade().getDatasourceDbDAO().insertSelective(datasourceDb);
      datasourceDb.setId(dbId);
      //module.setBizResult(context, datasourceDb);
    } else {
      // 更新状态
      DatasourceDbCriteria dbCriteria = new DatasourceDbCriteria();
      dbCriteria.createCriteria().andNameEqualTo(dbName);
      for (DatasourceDb db : module.getWorkflowDAOFacade().getDatasourceDbDAO().selectByExample(dbCriteria)) {
        datasourceDb = db;
        break;
      }
      Objects.requireNonNull(datasourceDb, "dbName:" + dbName + " relevant datasourceDb can not be null");
    }

    module.setBizResult(context, offlineManager.getDbConfig(module, datasourceDb));
    return datasourceDb;
  }


  @Autowired
  public void setOfflineManager(OfflineManager offlineManager) {
    this.offlineManager = offlineManager;
  }

}
