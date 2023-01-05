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
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.opensymphony.xwork2.ModelDriven;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.UploadJarForm;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.manage.common.ConfigFileReader;
import com.qlangtech.tis.manage.common.ConfigFileValidateResult;
import com.qlangtech.tis.manage.common.PropteryGetter;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.servlet.DownloadResource;
import com.qlangtech.tis.manage.servlet.DownloadServlet;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.solrdao.ISchemaPluginContext;
import com.qlangtech.tis.utils.MD5Utils;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UploadJarAction extends BasicModule implements ModelDriven<UploadJarForm> {

  private static final long serialVersionUID = 1L;

  private static final String GROUP_NAME = "uploadjar";

  public UploadJarAction() {
    super(GROUP_NAME);
  }

  // private static final ServletFileUpload servletFileUpload;
  // static {
  //
  // DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
  // diskFileItemFactory.setKeepFormFieldInMemory(true);
  //
  // servletFileUpload = new ServletFileUpload(diskFileItemFactory);
  // }
  private UploadJarForm form = new UploadJarForm();

  @Override
  public UploadJarForm getModel() {
    return form;
  }

  /**
   * 处理上传文件的请求
   */
  @Func(PermissionConstant.CONFIG_UPLOAD)
  public // @FormGroup(GROUP_NAME) UploadJarForm form,
  void doUploadJar(Context context) throws Exception {
    if (form.getSchema() == null || form.getConfig() == null) {
      this.addErrorMessage(context, "您需要上传schema和solrconfig文件");
      return;
    }
    Snapshot snapshot = parseSnapshot(context, this.form);
    // 最后 创建snapshot对象
    this.addActionMessage(context, "成功添加snapshotID:"
      + this.getSnapshotDAO().insertSelective(snapshot) + "的快照记录");
  }

  /**
   * 创建snapshot
   *
   * @param context
   * @param formm
   * @return
   * @throws IOException
   */
  protected Snapshot parseSnapshot(Context context, UploadJarForm formm, SnapshotCreater snapshotCreater) throws IOException {
    ConfigContentGetter[] getter = getContentGetter(formm);
    ConfigFileValidateResult validateValidateResult = null;
    boolean fileValid = true;
    // 校验文件格式是否正确
    ISchemaPluginContext schemaPlugin = SchemaAction.createSchemaPlugin(this.getCollectionName());
    for (ConfigContentGetter get : getter) {
      validateValidateResult = get.getterStrategy.validate(schemaPlugin, get.content);
      if (!validateValidateResult.isValid()) {
        this.addErrorMessage(context, validateValidateResult.getValidateResult());
        fileValid = false;
      }
    }
    if (!fileValid) {
      return null;
    }
    // createSnapshot(getter);
    Snapshot snapshot = snapshotCreater.create(getter);
    snapshot.setPreSnId(-1);
    return snapshot;
  }

  protected Snapshot parseSnapshot(Context context, UploadJarForm formm) throws IOException {
    return parseSnapshot(context, formm, new SnapshotCreater() {

      @Override
      public Snapshot create(ConfigContentGetter[] getter) {
        try {
          return createSnapshot(getter);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public interface SnapshotCreater {

    public Snapshot create(ConfigContentGetter[] getter);
  }

  /**
   * 上传全局资源
   *
   * @param form
   * @param nav
   * @param context
   * @throws Exception
   */
  // @Func(PermissionConstant.GLOBAL_DEPENDENCY_UPLOAD)
  // public void doUploadGlobalResource(
  // @FormGroup("globalres") GlobalResource form, Navigator nav,
  // Context context) throws Exception {
  //
  // Assert.assertNotNull(form);
  // Assert.assertNotNull(form.getMemo());
  // Assert.assertNotNull(form.getResource());
  // if (StringUtils.endsWith(form.getResource().getFieldName(), ".jar")) {
  // this.addErrorMessage(context, "文件属性应该为.jar");
  // return;
  // }
  //
  // InputStream reader = null;
  // try {
  //
  // reader = form.getResource().getInputStream();
  //
  // UploadResource solrCoreDependedResource = new UploadResource();
  // solrCoreDependedResource.setCreateTime(new Date());
  // solrCoreDependedResource.setContent(IOUtils.toByteArray(reader));
  // solrCoreDependedResource.setMd5Code(ConfigFileReader
  // .md5file(solrCoreDependedResource.getContent()));
  // solrCoreDependedResource
  // .setResourceType(com.taobao.terminator.runtime.module.screen.GlobalResource.UPLOAD_RESOURCE_TYPE_GLOBAL);
  // solrCoreDependedResource.setMemo(form.getMemo());
  // Integer newid = this.getUploadResourceDAO().insert(
  // solrCoreDependedResource);
  //
  // this.addActionMessage(context, "成功上传一条全局依赖资源 ID:" + newid);
  //
  // } finally {
  // IOUtils.closeQuietly(reader);
  // }
  //
  // }

//  /**
//   * 绑定全局资源
//   *
//   * @param context
//   * @throws Exception
//   */
//  @Func(PermissionConstant.CONFIG_EDIT)
//  public void doBindGlobalResource(Context context) throws Exception {
//    GlobalAppResource globalAppres = null;
//    final String[] uridAry = this.getRequest().getParameterValues("urId");
//    StringBuffer idstr = new StringBuffer();
//    for (String rid : uridAry) {
//      globalAppres = new GlobalAppResource();
//      globalAppres.setAppId(this.getAppDomain().getAppid());
//      globalAppres.setUrId(Long.parseLong(rid));
//      globalAppres.setGmtCreate(new Date());
//      globalAppres.setGmtModified(new Date());
//      this.getGlobalAppResourceDAO().insertSelective(globalAppres);
//      idstr.append("[").append(rid).append("]");
//    }
//    this.addActionMessage(context, "绑定资源" + idstr + "成功");
//  }

//  /**
//   * 解除全局资源绑定
//   *
//   * @param context
//   * @throws Exception
//   */
//  @Func(PermissionConstant.CONFIG_EDIT)
//  public void doUnbindGlobalResource(Context context) throws Exception {
//    final String[] uridAry = this.getRequest().getParameterValues("urId");
//    GlobalAppResourceCriteria criteria = null;
//    StringBuffer idstr = new StringBuffer();
//    for (String rid : uridAry) {
//      criteria = new GlobalAppResourceCriteria();
//      criteria.createCriteria().andUrIdEqualTo(Long.parseLong(rid)).andAppIdEqualTo(this.getAppDomain().getAppid());
//      this.getGlobalAppResourceDAO().deleteByExample(criteria);
//      idstr.append("[").append(rid).append("]");
//    }
//    this.addActionMessage(context, "解除绑定" + idstr + "成功！！！");
//  }

  /**
   * 下载全局依赖资源
   *
   * @param context
   * @throws Exception
   */
  public void doDownloadGlobalResource(Context context) throws Exception {
    Integer rid = this.getInt("rid");
    UploadResource resource = this.getUploadResourceDAO().loadFromWriteDB(new Long(rid));
    Assert.assertNotNull("resource can not be null", resource);
    Assert.assertNotNull("resource.getContent() can not null", resource.getContent());
    Assert.assertEquals("resource type:" + resource.getResourceType() + " rid:" + rid + " is not illegal", com.qlangtech.tis.runtime.module.screen.GlobalResource.UPLOAD_RESOURCE_TYPE_GLOBAL, resource.getResourceType());
    Assert.assertEquals(resource.getMd5Code(), MD5Utils.md5file(resource.getContent()));
    getResponse().setContentType(DownloadResource.JAR_CONTENT_TYPE);
    DownloadServlet.setDownloadName(getResponse(), rid + "_" + (System.currentTimeMillis() / 1000) + ".jar");
    IOUtils.write(resource.getContent(), getResponse().getOutputStream());
  }

  private // UploadJarForm form,
  Snapshot createSnapshot(final ConfigContentGetter[] getters) throws IOException {
    Snapshot snapshot = createSnapshot();
    for (ConfigContentGetter get : getters) {
      snapshot = processConfigFile(get, snapshot);
    }
    return snapshot;
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getSchema()
    // .getInputStream()), ConfigFileReader.FILE_SCHEMA, snapshot);
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getConfig()
    // .getInputStream()), ConfigFileReader.FILE_SOLOR, snapshot);
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getCoreprop()
    // .getInputStream()), ConfigFileReader.FILE_CORE_PROPERTIES,
    // snapshot);
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getDatasource()
    // .getInputStream()), ConfigFileReader.FILE_DATA_SOURCE, snapshot);
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getSpring()
    // .getInputStream()), ConfigFileReader.FILE_APPLICATION, snapshot);
    // snapshot = processConfigFile(IOUtils.toByteArray(form.getUploadfile()
    // .getInputStream()), ConfigFileReader.FILE_JAR, snapshot);
  }

  protected ConfigContentGetter[] getContentGetter(UploadJarForm form) throws IOException {
    ConfigContentGetter[] getters = new ConfigContentGetter[2];
    getters[0] = new ConfigContentGetter(ConfigFileReader.FILE_SCHEMA, form.getSchema());
    getters[1] = new ConfigContentGetter(ConfigFileReader.FILE_SOLR, form.getConfig());
    return getters;
  }

  public static class ConfigContentGetter {

    private final PropteryGetter getterStrategy;

    // private final FileItem formItem;
    private byte[] content = null;

    protected ConfigContentGetter(PropteryGetter getterStrategy, File formItem) throws IOException {
      super();
      this.getterStrategy = getterStrategy;
      // this.formItem = formItem;
      try {
        if (formItem != null) {
          // InputStream reader = null;
          // try {
          // reader = ;
          // IOUtils.toByteArray(reader);
          this.content = FileUtils.readFileToByteArray(formItem);
          // } finally {
          // IOUtils.closeQuietly(reader);
          // }
        }
      } catch (Exception e) {
        throw new RuntimeException("file name:" + getterStrategy.getFileName() + " path:" + formItem.getAbsolutePath() + " is faild");
      } finally {
        FileUtils.deleteQuietly(formItem);
      }
    }

    public ConfigContentGetter(PropteryGetter getterStrategy, String content) {
      try {
        Assert.assertNotNull("content can not be null", content);
        this.getterStrategy = getterStrategy;
        this.content = content.getBytes(BasicModule.getEncode());
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    public byte[] getContent() throws IOException {
      return this.content;
    }
  }

  //
  // // 用户上传的文件
  // final FileItem upload = form.getUploadfile();
  // final String memo = form.getMemo();
  //
  // if (!StringUtils.endsWith(upload.getName(), ".jar")) {
  // addActionMessage(context, "文件扩展名必须为“.jar”");
  // return;
  // }
  //
  // AppDomainInfo appDomain = this.getAppDomain();
  // // final long currentTimestamp = System.currentTimeMillis();
  //
  // // 将文件保存到本地目录中
  // // final File saveDir = getAppDomainDir();
  // // OutputStream writer = null;
  // // final File saveFile = new File(getAppDomainDir(), String
  // // .valueOf(currentTimestamp)
  // // + ".jar");
  // //
  // // if (!saveFile.exists() && !saveFile.createNewFile()) {
  // // throw new IllegalStateException("file:"
  // // + saveFile.getAbsolutePath() + " can not be null");
  // // }
  //
  // // JarFileManager jarFileManager = new JarFileManager(this
  // // .getApplicationDAO().selectByPrimaryKey(appDomain.getAppid()),
  // // currentTimestamp);
  //
  // // 将流保存到预定目录
  // AppPackage pack = new AppPackage();
  // // pack.setValidateCode(jarFileManager.save(upload.getInputStream()));
  // pack.setCreateTime(new Date());
  // pack.setAppId(appDomain.getAppid());
  // // TODO 先将这个值除以1000
  // // pack.setUploadTimestamp(currentTimestamp);
  // pack.setTestStatus((short) 0);
  // pack.setUploadUser(this.getLoginUserName());
  //
  // // InputStream savedStream = null;
  // // try {
  // // savedStream = new FileInputStream(saveFile);
  // // // 保存的文件的签名
  // // pack.setValidateCode(DigestUtils.md5Hex(savedStream));
  // // } finally {
  // // try {
  // // savedStream.close();
  // // } catch (Throwable e) {
  // // }
  // // }
  //
  // // TODO 需要添加设置 memo的属性
  // // pack.set
  // pack.setPid(this.getAppPackageDAO().insert(pack));
  //
  // JarInputStream jarStream = null;
  // try {
  // jarStream = new JarInputStream(upload.getInputStream());
  //
  // // 从上传的jar包中 取出内容
  // extraConfigFromJarFile(context, jarFileManager.getSaveFile(),
  // getAppDomainDir(), pack, appDomain);
  //
  // nav.redirectTo("manageModule").withTarget("jarlist");
  // } finally {
  // IOUtils.closeQuietly(jarStream);
  // }
  // }
  /**
   * 保存文件顺便取得 签名，保证文件不会被篡改
   *
   * @param reader
   * @param saveFile
   * @return 当前文件签名
   * @throws FileNotFoundException
   * @throws IOException
   */
  /**
   * @param context
   * @param savedJarFile
   *            jar包的保存路径
   * @param saveDir
   *            当前应用的保存文件夹
   * @param pack
   *            新建的package信息
   * @throws Exception
   */
  /**
   * @param context
   * @param savedJarFile
   * @param saveDir
   * @param pack
   * @throws Exception
   */
  // private void extraConfigFromJarFile(Context context,
  // JarInputStream jarStream, // File saveDir,
  // AppPackage pack, AppDomainInfo appdomain) throws Exception {
  //
  // if (jarStream == null) {
  // throw new IllegalArgumentException("jarStream can not be null");
  // }
  //
  // final Snapshot snapshot = new Snapshot();
  // // final long currentTimestamp = System.currentTimeMillis();
  //
  // // JarEntry entry = null;
  // // while ((entry = jarStream.getNextJarEntry()) != null) {
  // //
  // // }
  //
  // // for (PropteryGetter getter : ConfigFileReader.getConfigList()) {
  // // processConfigFile(jarFile, context, getter, snapshot);
  // // }
  //
  // snapshot.setCreateTime(new Date());
  // // snapshot.setPid(pack.getPid());
  // snapshot.setPreSnId(-1);
  // snapshot.setSnId(null);
  //
  // // 最后 创建snapshot对象
  // this.getSnapshotDAO().insert(snapshot);
  //
  // // // 处理applicationcontext.xml 配置文件
  // // processConfigFile(jarFile, context, // saveDir,
  // // // new SetSolrCoreDependency() {
  // // //
  // // // @Override
  // // // public void set(UploadResource res) {
  // // // res.setResourceType(ConfigFileReader.FILE_APPLICATION
  // // // .getFileName());
  // // // }
  // // // // public void set(long currentTimestamp, String md5) {
  // // // // snapshot.setApplicationStamp(currentTimestamp);
  // // // // snapshot.setApplicationCode(md5);
  // // // // }
  // // //
  // // // },
  // // ConfigFileReader.FILE_APPLICATION, snapshot
  // // // ,currentTimestamp
  // // );
  //
  // // // 处理datasource 配置文件
  // // processConfigFile(jarFile, context, saveDir,
  // // new SetSolrCoreDependency() {
  // // public void set(long currentTimestamp, final String md5) {
  // // snapshot.setDsTimeStamp(currentTimestamp);
  // // snapshot.setDsCode(md5);
  // // }
  // // }, FILE_DATA_SOURCE, currentTimestamp);
  // //
  // // // 处理schema.xml 配置文件
  // // processConfigFile(jarFile, context, saveDir,
  // // new SetSolrCoreDependency() {
  // // public void set(long currentTimestamp, final String md5) {
  // // snapshot.setSchemaTimeStamp(currentTimestamp);
  // // snapshot.setSchemaCode(md5);
  // // }
  // // }, FILE_SCHEMA, currentTimestamp);
  // //
  // // // 处理schema.xml 配置文件
  // // processConfigFile(jarFile, context, saveDir,
  // // new SetSolrCoreDependency() {
  // // public void set(long currentTimestamp, final String md5) {
  // // snapshot.setSolrStamp(currentTimestamp);
  // // snapshot.setSolrCode(md5);
  // // }
  // // }, FILE_SOLOR, currentTimestamp);
  //
  // // 找到日常环境的group
  //
  // }

  /**
   * @param snapshot
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   */
  private // , FileNotFoundException
  Snapshot processConfigFile(// , FileNotFoundException
                             ConfigContentGetter getter, // , FileNotFoundException
                             Snapshot snapshot) throws IOException {
    if (getter.getContent() == null) {
      return snapshot;
    }
    return processFormItem(getter, snapshot);
    // final String md5 = JarFileManager.saveFile(reader, new File(saveDir,
    // fileName + currentTimestamp));
    // set.set(currentTimestamp, md5);
    // this.getUploadResourceDAO().insert(solrCoreDependedResource);
  }

  public static Snapshot processFormItem(RunContext runContext, ConfigContentGetter getter, Snapshot snapshot) throws IOException {
    // InputStream reader = null;
    // try {
    // reader = getter.formItem.getInputStream();
    UploadResource solrCoreDependedResource = new UploadResource();
    solrCoreDependedResource.setCreateTime(new Date());
    solrCoreDependedResource.setContent(getter.getContent());
    solrCoreDependedResource.setMd5Code(MD5Utils.md5file(solrCoreDependedResource.getContent()));
    solrCoreDependedResource.setResourceType(getter.getterStrategy.getFileName());
    Integer newid = runContext.getUploadResourceDAO().insertSelective(solrCoreDependedResource);
    return getter.getterStrategy.createNewSnapshot(newid, snapshot);
  }

  private Snapshot processFormItem(ConfigContentGetter getter, Snapshot snapshot) throws IOException {
    return processFormItem(this, getter, snapshot);
    // UploadResource solrCoreDependedResource = new UploadResource();
    // solrCoreDependedResource.setCreateTime(new Date());
    // solrCoreDependedResource.setContent(getter.getContent());
    // solrCoreDependedResource.setMd5Code(ConfigFileReader
    // .md5file(solrCoreDependedResource.getContent()));
    // solrCoreDependedResource.setResourceType(getter.getterStrategy
    // .getFileName());
    //
    // Integer newid = this.getUploadResourceDAO().insert(
    // solrCoreDependedResource);
    //
    // return getter.getterStrategy.createNewSnapshot(newid, snapshot);
    // } finally {
    // IOUtils.closeQuietly(reader);
    // }
    // InputStream reader = null;
    // try {
    // reader = getter.formItem.getInputStream();
    //
    // UploadResource solrCoreDependedResource = new UploadResource();
    // solrCoreDependedResource.setCreateTime(new Date());
    // solrCoreDependedResource.setContent(IOUtils.toByteArray(reader));
    // solrCoreDependedResource.setMd5Code(ConfigFileReader
    // .md5file(solrCoreDependedResource.getContent()));
    // solrCoreDependedResource.setResourceType(getter.getterStrategy
    // .getFileName());
    //
    // Integer newid = this.getUploadResourceDAO().insert(
    // solrCoreDependedResource);
    //
    // return getter.getterStrategy.createNewSnapshot(newid, snapshot);
    // } finally {
    // IOUtils.closeQuietly(reader);
    // }
  }

  // private interface SetSnapshot {
  // public void set(long currentTimestamp, String md5);
  // }
  // private interface SetSolrCoreDependency {
  // public void set(UploadResource solrCoreDependedResource);
  // }
  // /**
  // * @param saveDir
  // * @return
  // */
  // private File createDir(File tempDir) {
  //
  // if (tempDir == null) {
  // throw new IllegalArgumentException("tempdir can not be null");
  // }
  //
  // if (!tempDir.exists() && !tempDir.mkdirs()) {
  // throw new IllegalStateException("tempdir:"
  // + tempDir.getAbsolutePath() + " can not be create");
  // }
  // return tempDir;
  // }
  public static void main(String[] arg) throws Exception {
    // JarFile jarFile = new JarFile(
    // "D:\\文档\\终搜\\后台开发\\terminator_据划算_代码包\\terminator-check\\snsjuterminator-1.0.jar");
    // ZipEntry query = new
    // ZipEntry("checkitem/daily/applicationContext.xml");
    // InputStream reader = jarFile.getInputStream(query);
    //
    // LineIterator it = IOUtils.lineIterator(reader, "utf8");
    // while (it.hasNext()) {
    // System.out.println(it.nextLine());
    // }
    // InputStream reader = null;
    // reader = findEntry(jarFile, "applicationContext.xml");
    //
    // LineIterator it = IOUtils.lineIterator(reader, "utf8");
    // while (it.hasNext()) {
    // System.out.println(it.nextLine());
    // }
  }
  // /**
  // * 提取内容
  // *
  // * @param file
  // * @param name
  // * @return
  // * @throws IOException
  // */
  // private InputStream findEntry(JarInputStream jarinputStream, String name)
  // throws IOException {
  // // Enumeration<JarEntry> entryList = jarinputStream.
  // // JarEntry entry = null;
  // // while (entryList.hasMoreElements()) {
  // // entry = entryList.nextElement();
  // //
  // // System.out.println(entry.getName());
  // // if (StringUtils.indexOf(entry.getName(), name) > -1) {
  // // return file.getInputStream(entry);
  // // }
  // // }
  //
  // JarEntry entry = null;
  // while ((entry = jarinputStream.getNextJarEntry()) != null) {
  // // entry = entryList.nextElement();
  // //
  // // entry.get
  // //
  // // System.out.println(entry.getName());
  // // if (StringUtils.indexOf(entry.getName(), name) > -1) {
  // // return entry.
  // // }
  // }
  //
  // return null;
  //
  // }
  // @Autowired
  // public void setParameterParser(ParameterParser parameterParser) {
  // this.parameterParser = parameterParser;
  // }
}
