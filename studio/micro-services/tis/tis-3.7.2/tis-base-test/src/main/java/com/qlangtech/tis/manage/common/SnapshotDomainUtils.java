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

import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.utils.MD5Utils;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-04 17:39
 */
public class SnapshotDomainUtils {

    public static SnapshotDomain mockEmployeeSnapshotDomain() throws Exception {

        SnapshotDomain snapshotDomain = new SnapshotDomain();

        try (InputStream i = SnapshotDomainUtils.class.getResourceAsStream("search4employee-schema")) {
            Objects.requireNonNull(i, "schema stream can not be null");
            UploadResource schema = new UploadResource();
            schema.setContent(IOUtils.toByteArray(i));
            schema.setMd5Code(MD5Utils.md5file(schema.getContent()));
            snapshotDomain.setSolrSchema(schema);
        }

        try (InputStream i = SnapshotDomainUtils.class.getResourceAsStream("search4employee-solrconfig")) {
            Objects.requireNonNull(i, "solrconfig stream can not be null");
            UploadResource solrCfg = new UploadResource();
            solrCfg.setContent(IOUtils.toByteArray(i));
            solrCfg.setMd5Code(MD5Utils.md5file(solrCfg.getContent()));
            snapshotDomain.setSolrConfig(solrCfg);
        }


//        String collectionName = ITestDumpCommon.INDEX_COLLECTION;// ITestDumpCommon.INDEX_COLLECTION;
//        SnapshotDomain snapshotDomain = HttpConfigFileReader.getResource(
//                collectionName, 21137, RunEnvironment.getSysRuntime(), ConfigFileReader.FILE_SCHEMA, ConfigFileReader.FILE_SOLR);
//        System.out.println(snapshotDomain);
        return snapshotDomain;
    }

//    public static void main(String[] args) throws Exception {
//        mockEmployeeSnapshotDomain();
//    }
}
