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
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-30
 */
public class HttpConfigFileReader extends ConfigFileReader {

    private static final Logger logger = LoggerFactory.getLogger(HttpConfigFileReader.class);

    private final File localRepos;

    public static final String unmergeglobalparams = "unmergeglobalparams";

    public static final XStream xstream = new XStream();

    static {
        xstream.alias("sdomain", SnapshotDomain.class);
    }

    private final Application application;

    /**
     * @param snapshot
     * @param appDomainDir
     * @param localRepos   本地临时文件夹
     */
    public HttpConfigFileReader(SnapshotDomain snapshot, URL appDomainDir, File localRepos, Application application) {
        super(snapshot, appDomainDir);
        this.localRepos = localRepos;
        this.application = application;
    }

    /**
     * 取得本地存放索引的文件夹
     *
     * @return
     */
    public File getLocalSolrHome() {
        return ConfigFileReader.getAppDomainDir(this.localRepos, this.application.getDptId(), this.application.getAppId());
    }

    @Override
    public String getPath(PropteryGetter pGetter) {
        return getSpecificUrl(pGetter).toString();
    }

    @Override
    public byte[] getContent(PropteryGetter getter) {
        URL apply = getSpecificUrl(getter);
        return ConfigFileContext.processContent(apply, new StreamProcess<byte[]>() {

            @Override
            public byte[] p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                try {
                    return IOUtils.toByteArray(stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    // return super.getContent(getter);
    }

    private URL getSpecificUrl(PropteryGetter getter) {
        try {
            URL apply = new URL(this.getAppDomainDir(), "/download/bypid/" + this.getSnapshot().getSnapshot().getSnId() + "/snid/" + this.getSnapshot().getSnapshot().getSnId() + "/" + getter.getFileName());
            return apply;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public File getNewFile(PropteryGetter pGetter, Long fileSufix) {
        if (fileSufix == null) {
            throw new IllegalArgumentException("fileSufix can not be null");
        }
        File localFile = getNewFile(ConfigFileReader.getAppDomainDir(this.localRepos, application.getDptId(), application.getAppId()).toURI(), pGetter, fileSufix);
        // 文件已经存在
        if (localFile.exists()) {
            return localFile;
        }
        // 将文件从远端服务器上下载下来
        byte[] content = getContent(pGetter);
        try {
            FileUtils.writeByteArrayToFile(localFile, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return localFile;
    }

    // // 百岁添加 20121200113 从后台repository中取得 配置为呢间信息，以及jar包
    // public PublishInfo getPublishInfo(String ipaddress,
    // RunEnvironment environment) throws MalformedURLException {
    //
    // // if (coreName == null) {
    // // throw new IllegalArgumentException("coreCofig can not be null");
    // // }
    // if (ipaddress == null) {
    // throw new IllegalArgumentException("ipaddress can not be null");
    // }
    //
    // final URL applyUrl = new URL(this.getAppDomainDir()
    // + "/download/getconfigbycorename/" + environment.getKeyName()
    // + "/" + ipaddress + "/" + this.application.getProjectName());
    //
    // return ConfigFileContext.processContent(applyUrl,
    // new StreamProcess<PublishInfo>() {
    // @Override
    // public PublishInfo p(int status, InputStream stream,
    // String md5) {
    // return checkFileHasNotDistort((PublishInfo) xstream
    // .fromXML(stream));
    // }
    // });
    //
    // }
    public static SnapshotDomain getResource(String appName, RunEnvironment runtime, PropteryGetter... fileGetter) throws RepositoryException {
        return getResource(appName, -1, /* targetSnapshotid */
        runtime, false, fileGetter);
    }

    /**
     * @param
     * @param appName
     * @param targetSnapshotid 需要下载的snapshot版本号
     * @param runtime
     * @param fileGetter
     * @return
     * @throws RepositoryException
     */
    public static SnapshotDomain getResource(String appName, final long targetSnapshotid, RunEnvironment runtime, PropteryGetter... fileGetter) throws RepositoryException {
        return getResource(appName, targetSnapshotid, runtime, false, fileGetter);
    }

    public static SnapshotDomain getResource(String appName, final long targetSnapshotid, RunEnvironment runtime, boolean unmergeglobalparams, PropteryGetter... fileGetter) throws RepositoryException {
        return getResource(appName, targetSnapshotid, runtime, unmergeglobalparams, true, /* reThrowNewException */
        fileGetter);
    }

    /**
     * 从终搜仓库中取得资源
     *
     * @param appName
     * @param
     * @param runtime
     * @param fileGetter
     * @return
     */
    public static SnapshotDomain getResource(String appName, final long targetSnapshotid, RunEnvironment runtime
            , boolean unmergeglobalparams, boolean reThrowNewException, PropteryGetter... fileGetter) throws RepositoryException {
        String tisUrl = Config.getConfigRepositoryHost();
        if (StringUtils.isEmpty(tisUrl)) {
            throw new IllegalArgumentException("parameter terminatorUrl can not be null");
        }
        if (StringUtils.isEmpty(appName)) {
            throw new IllegalArgumentException("parameter appName can not be null");
        }
        if (fileGetter == null || fileGetter.length < 1) {
            throw new IllegalArgumentException("parameter fileGetter can not be null or length < 1");
        }
        if (runtime == null) {
            throw new IllegalArgumentException("parameter runtime can not be null or length < 1");
        }
        try {
            StringBuffer url = new StringBuffer(tisUrl + "/download/appconfig/" + appName);
            url.append("/").append(0);
            url.append("/").append(runtime.getKeyName());
            for (int i = 0; i < fileGetter.length; i++) {
                url.append("/").append(fileGetter[i].getFileName());
            }
            url.append("?snapshotid=").append(targetSnapshotid);
            // 不需要合并全局参数
            if (unmergeglobalparams) {
                url.append("&").append(HttpConfigFileReader.unmergeglobalparams).append("=true");
            }
            URL requestUrl = new URL(url.toString());
            return ConfigFileContext.processContent(requestUrl, new StreamProcess<SnapshotDomain>() {

                @Override
                public SnapshotDomain p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                    return (SnapshotDomain) xstream.fromXML(stream);
                }
            });
        } catch (Throwable e) {
            if (reThrowNewException) {
                throw new RepositoryException("config resource is not exist,appname:" + appName + " groupIndex:" + 0 + " runtime:" + runtime, e);
            } else {
                logger.warn("can not find resource:" + ExceptionUtils.getMessage(e));
            }
        }
        return null;
    }

    public static SnapshotDomain getResource(String terminatorUrl, Integer snid) throws RepositoryException {
        try {
            StringBuffer url = new StringBuffer(terminatorUrl + "/download/appconfigbysnid/" + snid);
            URL requestUrl = new URL(url.toString());
            return ConfigFileContext.processContent(requestUrl, new StreamProcess<SnapshotDomain>() {

                @Override
                public SnapshotDomain p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                    return (SnapshotDomain) xstream.fromXML(stream);
                }
            });
        } catch (Throwable e) {
            throw new RepositoryException("config resource is not exist,snid:" + snid, e);
        }
    }

    /**
     * 通过packageid 找到对应实体信息
     *
     * @param
     * @return
     * @throws MalformedURLException
     */
    // public static TestDoamin getTestDoaminFromRemote(Integer packid,
    // String terminatorRepository) throws MalformedURLException {
    //
    // return ConfigFileContext.processContent(new URL(terminatorRepository
    // + "/download/testdomain?pid=" + packid),
    // new StreamProcess<TestDoamin>() {
    // @Override
    // public TestDoamin p(int status, InputStream stream,
    // String md5) {
    // return (TestDoamin) xstream.fromXML(stream);
    // }
    // });
    //
    // }
    // /**
    // * 检查远端传过来的文件没有被篡改过
    // *
    // * @param pub
    // */
    // private static PublishInfo checkFileHasNotDistort(final PublishInfo pub)
    // {
    //
    // List<PropteryGetter> propContent = ConfigFileReader.getConfigList();
    //
    // for (PropteryGetter getter : propContent) {
    // if (!StringUtils.equals(getter.getMd5CodeValue(pub.getSnapshot()),
    // ConfigFileReader.md5file(getter.getContent(pub
    // .getSnapshot())))) {
    // throw new IllegalStateException(getter.getFileName()
    // + " file have been distort");
    // }
    // }
    //
    // // final Map<PropteryGetter, byte[]> propContent = new
    // // HashMap<PropteryGetter, byte[]>();
    // // // 将文件写到本地目录中
    // // // getStore2File(pub);
    // //
    // // propContent.put(ConfigFileReader.FILE_APPLICATION, pub
    // // .getSnapshot().getApplication().getContent());
    // //
    // // propContent.put(ConfigFileReader.FILE_DATA_SOURCE, pub
    // // .getDatasourceContent());
    // //
    // // propContent.put(ConfigFileReader.FILE_SCHEMA,
    // // pub.getSchemaContent());
    // //
    // // propContent
    // // .put(ConfigFileReader.FILE_SOLOR, pub.getSolrConfigContent());
    //
    // return pub;
    // }
    public static void main(String[] arg) throws Exception {
    // Application app = new Application();
    // app.setBizId(1);
    // app.setAppId(1);
    //
    // Snapshot snapshot = new Snapshot();
    // // snapshot.setApplicationStamp(1325231504997L);
    // snapshot.setPid(56);
    // snapshot.setSnId(198);
    // snapshot.setApplicationCode("e1baf47fd9869f50dbfb3fbfba1a5a35");
    // HttpConfigFileReader reader = new HttpConfigFileReader(snapshot,
    // new URL("http://localhost"), new File("/tmpp"), app);
    // InputStream input = reader.read(ConfigFileReader.FILE_APPLICATION);
    //
    // System.out.println(IOUtils.toString(input));
    //
    // input.close();
    // System.out
    // .println("======================================================");
    // System.out.println(FileUtils.readFileToString(reader
    // .getFile(ConfigFileReader.FILE_APPLICATION)));
    // SnapshotDomain domain = HttpConfigFileReader.getResource(
    // "http://terminator.admin.taobao.org:9999",
    // "search4wantumobile", 0, RunEnvironment.READY,
    // // ConfigFileReader.FILE_SOLOR);
    // for (int i = 0; i < 1000; i++) {
    // System.out.println(i);
    //
    // fetchResource(RunEnvironment.DAILY);
    // // fetchResource(RunEnvironment.READY);
    // // fetchResource(RunEnvironment.ONLINE);
    // }
    // System.out.println("end");
    }
    // private static void fetchResource(RunEnvironment runtime)
    // throws TerminatorRepositoryException {
    //
    // // SnapshotDomain domain = HttpConfigFileReader.getResource(
    // // "http://l.admin.taobao.org", "search4bbs", 0, runtime,
    // // ConfigFileReader.FILE_SOLOR);
    //
    // SnapshotDomain domain = HttpConfigFileReader.getResource(
    // "http://daily.terminator.admin.taobao.org", "search4bbs", 0,
    // runtime, ConfigFileReader.FILE_SOLOR);
    //
    // // 取得schema 配置文件
    // UploadResource resource = domain.getSolrConfig();
    //
    // Assert.assertNotNull(resource);
    //
    // // content是byte数组类型，编码使用utf8编码
    // Assert.assertNotNull(resource.getContent());
    //
    // System.out.println(new String(resource.getContent()));
    // }
}
