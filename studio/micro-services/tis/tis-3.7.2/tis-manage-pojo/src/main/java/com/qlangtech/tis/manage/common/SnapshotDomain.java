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

import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-3-26
 */
public class SnapshotDomain {

    private UploadResource solrSchema = new UploadResource();

    private UploadResource solrConfig = new UploadResource();

    private final Snapshot snapshot;

    public SnapshotDomain() {
        super();
        snapshot = new Snapshot();
    }

    public SnapshotDomain(Snapshot snapshot) {
        super();
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot can not be null");
        }
        this.snapshot = snapshot;
    }


    /**
     * @param
     * @param coreName
     * @return
     * @throws
     */
    public void writeResource2fs(ITISFileSystem fs, String coreName, PropteryGetter getter) {
        String path = getter.getFsPath(fs, coreName);// fs.getRootDir() + "/" + coreName + "/config/" + getter.getFileName();
        IPath dst = fs.getPath(path);
        if (dst == null) {
            throw new IllegalStateException("path can not be create:" + path);
        }
        OutputStream dstoutput = null;
        try {
            dstoutput = fs.create(dst, true);
            IOUtils.write(getter.getContent(this), dstoutput);
        } catch (IOException e1) {
            throw new RuntimeException("[ERROR] Submit Service Core  Schema.xml to HDFS Failure !!!!", e1);
        } finally {
            IOUtils.closeQuietly(dstoutput);
        }
    }

    public Integer getAppId() {
        return snapshot.getAppId();
    }

    public Snapshot getSnapshot() {
        if (this.snapshot == null) {
            throw new NullPointerException("this.snapshot can not be null");
        }
        return snapshot;
    }

    public void setSolrSchema(UploadResource solrSchema) {
        this.solrSchema = solrSchema;
    }

    public void setSolrConfig(UploadResource solrConfig) {
        this.solrConfig = solrConfig;
    }


    public UploadResource getSolrSchema() {
        return solrSchema;
    }

    public UploadResource getSolrConfig() {
        return solrConfig;
    }
}
