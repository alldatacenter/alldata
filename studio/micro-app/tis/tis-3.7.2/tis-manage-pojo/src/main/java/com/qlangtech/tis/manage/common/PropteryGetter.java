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

import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.solrdao.ISchemaPluginContext;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface PropteryGetter {

    String KEY_PROP_CONFIG_SNAPSHOTID = "property.configsnapshotid";
    String KEY_PROP_TIS_REPOSITORY_HOST = "property.tisRepositoryHost";

    /**
     * 取得在FS中的文件路径
     *
     * @param fs
     * @param coreName
     * @return
     */
    default String getFsPath(ITISFileSystem fs, String coreName) {
        String path = fs.getRootDir() + "/" + coreName + "/config/" + this.getFileName();
        return path;
    }

    public String getFileName();

    public String getMd5CodeValue(SnapshotDomain domain);

    public Long getFileSufix(SnapshotDomain domain);

    public byte[] getContent(SnapshotDomain domain);

    public UploadResource getUploadResource(SnapshotDomain snapshotDomain);

    /**
     * 判断文件格式是否合法
     *
     * @param
     * @return
     */
    public ConfigFileValidateResult validate(ISchemaPluginContext schemaPlugin, UploadResource resource);

    public ConfigFileValidateResult validate(ISchemaPluginContext schemaPlugin, byte[] resource);

    /**
     * 更新配置文件的时，当更新成功之后需要创建一条新的snapshot事体对象
     *
     * @param
     * @param
     * @return
     */
    public Snapshot createNewSnapshot(Integer newResourceId, Snapshot snapshot);
}
