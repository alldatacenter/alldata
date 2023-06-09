/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.runtime.module.action.jarcontent;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.Savefilecontent;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.pubhook.common.ConfigConstant;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.solrdao.ISchemaPluginContext;
import com.qlangtech.tis.utils.MD5Utils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * 用户修改文件，并且保存文件 保存文件内容
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年4月20日
 */
public class SaveFileContentAction extends BasicModule {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(SaveFileContentAction.class);

  /**
   * Schema编辑页面通過ajax方式取得文本信息
   *
   * @param context
   * @throws Exception
   */
  public void doGetConfig(Context context) throws Exception {
    byte[] resContent = getResContent(this, context);
    setConfigFileContent(this, context, new String(resContent, TisUTF8.get()), true);
  }

  public static byte[] getResContent(BasicModule module, Context context) {
    SnapshotDomain snapshot = getSnapshot(module, context);
    PropteryGetter cgetter = ConfigFileReader.createPropertyGetter(module.getString("restype"));
    return cgetter.getContent(snapshot);
  }

  private static final SnapshotDomain getSnapshot(BasicModule module, Context context) {
    Snapshot sn = (Snapshot) context.get("snapshot");
    if (sn != null) {
      return module.getSnapshotViewDAO().getView(sn.getSnId(), false);
    }
    // isEditModel() ? this.getInt("snapshotid") :
    Integer snapshotId = module.getInt("snapshot");
    SnapshotDomain snapshot = module.getSnapshotViewDAO().getView(snapshotId, false);
    if (snapshot == null) {
      throw new IllegalArgumentException("snapshotId:" + snapshotId + " can not find pojo in db");
    }
    context.put("snap", snapshot.getSnapshot());
    return snapshot;
  }

  public static void setConfigFileContent(BasicModule basicModule, Context context, String content, boolean editModel) {
    // context.put(KEY_FILE_CONTENT, );
    basicModule.setBizResult(context, editModel ? content : StringEscapeUtils.escapeHtml(content));
  }

//  /**
//   * 将日常的文件同步到线上<br>
//   * 这段逻辑原先应该是在线上执行的，现在要放在日常上执行，日常连线上肯定是可以执行的
//   *
//   * @param context
//   */
//  @Func(PermissionConstant.CONFIG_SYNCHRONIZE_FROM_DAILY)
//  public void doSyncDailyConfig(Context context) throws Exception {
//    AppDomainInfo app = this.getAppDomain();
//    Assert.assertEquals(this.getInt("appid"), app.getAppid());
//    // 配置文件同步控制器
//    final ResSynManager synManager = ResSynManager.createSynManagerOnlineFromDaily(app.getAppName(), this);
//    if (!synManager.shallSynchronize()) {
//      this.addErrorMessage(context, "DAILY环境配置文件已经同步到线上，不需要再同步了");
//      return;
//    }
//    // final Snapshot snapshot =
//    if (synManager.getSynchronizedOnlineSnapshot(context, this)) {
//      this.addActionMessage(context, "已经将日常的配置成功发布到线上生产环境");
//    }
//    // 创建新SNAPSHOT
//    // this.addActionMessage(
//    // context,
//    // "同步文件成功,最新snapshot:"
//    // + createNewSnapshot(snapshot, "synchronize from daily",
//    // this, new Long(this.getUserId()),
//    // this.getLoginUserName()));
//  }

//  /**
//   * 保存文本内容
//   */
//  @Func(PermissionConstant.CONFIG_EDIT)
//  public void doSaveContent(Context context) throws Exception {
//    if (!RunEnvironment.isDevelopMode()) {
//      this.addErrorMessage(context, "请先更新日常环境中的配置文件，然后同步到线上环境!");
//      return;
//    }
//    Savefilecontent xmlContent = this.parseJsonPost(Savefilecontent.class);
//    Integer snapshotid = xmlContent.getSnapshotid();
//    String fileName = xmlContent.getFilename();
//    // if (isEditSchemaApply(context, fileName)) return;
//    PropteryGetter propertyGetter = createConfigFileGetter(fileName);
//    Long userid = 999l;
//    try {
//      userid = new Long(this.getUserId());
//    } catch (Throwable e) {
//    }
//    ISchemaPluginContext schemaPlugin = SchemaAction.createSchemaPlugin(this.getCollectionName());
//    CreateSnapshotResult createResult = createNewSnapshot(context
//      , this.getSnapshotViewDAO().getView(snapshotid, false), propertyGetter, schemaPlugin, xmlContent.getContentBytes()
//      , this, this, xmlContent.getMemo(), userid, this.getLoginUserName());
//
//    if (!createResult.isSuccess()) {
//      // forward("edit_" + BasicContentScreen.getResourceName(propertyGetter));
//      return;
//    }
//    this.setBizResult(context, createResult);
//    this.addActionMessage(context, "保存文件成功,最新snapshot:" + createResult.getNewId());
//  }

//  /**
//   * @param context
//   * @param domain         原有对象
//   * @param fileGetter
//   * @param uploadContent
//   * @param runContext
//   * @param messageHandler
//   * @param memo
//   * @param userId
//   * @param userName
//   * @return
//   * @throws UnsupportedEncodingException
//   */
//  public static CreateSnapshotResult createNewSnapshot(Context context, final SnapshotDomain domain
//    , PropteryGetter fileGetter, ISchemaPluginContext schemaPlugin
//    , byte[] uploadContent, RunContext runContext, IMessageHandler messageHandler
//    , String memo, Long userId, String userName) throws UnsupportedEncodingException {
//    return createNewSnapshot(context, domain, fileGetter, schemaPlugin, uploadContent, runContext
//      , messageHandler, memo, userId, userName, true);
//  }

//  public static CreateSnapshotResult createNewSnapshot(Context context, final SnapshotDomain domain, PropteryGetter fileGetter
//    , ISchemaPluginContext schemaPlugin, byte[] uploadContent, RunContext runContext, IMessageHandler messageHandler, String memo, Long userId, String userName, boolean createNewSnapshot) throws UnsupportedEncodingException {
//    CreateSnapshotResult createResult = new CreateSnapshotResult();
//    try {
//      final String md5 = MD5Utils.md5file(uploadContent);
//      if (StringUtils.equals(md5, fileGetter.getMd5CodeValue(domain))) {
//        saveHasNotModifyMessage(context, messageHandler, domain.getSnapshot().getSnId());
//        return createResult;
//      }
//      // 创建一条资源记录
//      try {
//        Integer newResId = ResSynManager.createNewResource(context, schemaPlugin, uploadContent, md5, fileGetter, messageHandler, runContext);
//        final Snapshot snapshot = fileGetter.createNewSnapshot(newResId, domain.getSnapshot());
//        if (createNewSnapshot) {
//          snapshot.setMemo(memo);
//          createResult.setNewSnapshotId(createNewSnapshot(snapshot, memo, runContext, userId, userName));
//          snapshot.setSnId(createResult.getNewId());
//        }
//        createResult.setSnapshot(snapshot);
//        context.put("snapshot", snapshot);
//      } catch (SchemaFileInvalidException e) {
//        logger.error(e.getMessage(), e);
//        return createResult;
//      }
//    } finally {
//
//    }
//    createResult.setSuccess(true);
//    return createResult;
//  }

  public static class CreateSnapshotResult {

    private Integer newSnapshotId;

    private boolean success = false;

    private Snapshot snapshot;

    public Snapshot getSnapshot() {
      return snapshot;
    }

    public void setSnapshot(Snapshot snapshot) {
      this.snapshot = snapshot;
    }

    public Integer getNewId() {
      return newSnapshotId;
    }

    public void setNewSnapshotId(Integer newId) {
      this.newSnapshotId = newId;
    }

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }
  }

  public static // BasicModule
  Integer createNewSnapshot(// BasicModule
                            final Snapshot snapshot, // BasicModule
                            final String memo, // BasicModule
                            RunContext runContext, // BasicModule
                            Long userid, // module
                            String userName) {
    Integer newId;
    snapshot.setSnId(null);
    snapshot.setUpdateTime(new Date());
    snapshot.setCreateTime(new Date());
    try {
      snapshot.setCreateUserId(userid);
    } catch (Throwable e) {
      snapshot.setCreateUserId(0l);
    }
    snapshot.setCreateUserName(userName);
    // final String memo = this.getString("memo");
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

  private PropteryGetter createConfigFileGetter(String fileName) {
    if (ConfigConstant.FILE_SCHEMA.equals(fileName)) {
      return ConfigFileReader.FILE_SCHEMA;
    } else if (ConfigConstant.FILE_SOLR.equals(fileName)) {
      return ConfigFileReader.FILE_SOLR;
    } else {
      throw new IllegalStateException("fileName:" + fileName + " can not match any process");
    }
  }

  private static void saveHasNotModifyMessage(Context context, IMessageHandler messageHandler, Integer snapshotid) {
    messageHandler.addErrorMessage(context, "文件没有变更，保持当前snapshot:" + snapshotid);
  }
}
