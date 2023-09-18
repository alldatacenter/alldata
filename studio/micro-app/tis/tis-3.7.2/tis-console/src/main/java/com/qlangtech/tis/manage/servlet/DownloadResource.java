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
package com.qlangtech.tis.manage.servlet;

import static com.qlangtech.tis.manage.common.ConfigFileReader.FILE_SCHEMA;
import static com.qlangtech.tis.manage.common.ConfigFileReader.FILE_SOLR;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.PropteryGetter;
import com.qlangtech.tis.manage.common.SnapshotDomain;

/**
 * 在服务器端仓库中代理客户端向仓库请求
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-30
 */
public class DownloadResource {

    // private final AppPackage pack;
    private final Application application;

    private final SnapshotDomain snapshot;

    private final String resourceName;

    // private final ConfigFileReader fileReader;
    private final PropteryGetter getStrategy;

    public static final String JAR_NAME = "jar";

    public DownloadResource(Application application, SnapshotDomain snapshot, String resourceName) {
        super();
        // this.pack = pack;
        this.application = application;
        this.snapshot = snapshot;
        this.resourceName = resourceName;
        // this.fileReader = createConfigFileReader(this);
        this.getStrategy = getGotStrategy(this);
    }

    public static final String XML_CONTENT_TYPE = "text/xml";

    public static final String JAR_CONTENT_TYPE = "application/zip";

    public String getContentType() {
        return JAR_NAME.equals(resourceName) ? JAR_CONTENT_TYPE : XML_CONTENT_TYPE;
    }

    public int getFileLength() {
        return this.getStrategy.getContent(snapshot).length;
    // return (int) fileReader.getFile(this.getStrategy).length();
    }

    public String getFileName() {
        return this.getStrategy.getFileName();
    }

    public String getMd5CodeValue() {
        return getStrategy.getMd5CodeValue(this.snapshot);
    }

    public byte[] read() throws Exception {
        return this.getStrategy.getContent(snapshot);
    }

    // public byte[] read() throws Exception {
    // // return fileReader.read(this.getStrategy);
    //
    // }
    // private static ConfigFileReader createConfigFileReader(
    // DownloadResource downloadRes) {
    // final ConfigFileReader reader = new ConfigFileReader(downloadRes
    // .getSnapshot(), ConfigFileReader.getAppDomainDir(Config
    // .getLocalRepository(), downloadRes.getApplication().getBizId(),
    // downloadRes.getApplication().getAppId()));
    // return reader;
    // }
    private static PropteryGetter getGotStrategy(DownloadResource resource) {
        final String resourceName = resource.getResourceName();
        // }
        if (FILE_SOLR.getFileName().equals(resourceName)) {
            return FILE_SOLR;
        }
        if (FILE_SCHEMA.getFileName().equals(resourceName)) {
            return FILE_SCHEMA;
        }
        throw new IllegalArgumentException("resourceName:" + resourceName + " has not match any file pattern");
    }

    // public AppPackage getPack() {
    // return pack;
    // }
    public Application getApplication() {
        return application;
    }

    // public Snapshot getSnapshot() {
    // return snapshot;
    // }
    public String getResourceName() {
        return resourceName;
    }
}
