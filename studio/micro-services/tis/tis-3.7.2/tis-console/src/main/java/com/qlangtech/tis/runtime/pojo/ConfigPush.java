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
package com.qlangtech.tis.runtime.pojo;

import java.io.Serializable;
import java.util.List;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年2月22日
 */
public class ConfigPush implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<UploadResource> uploadResources;

    private String collection;

    // local snapshotid,本地上传id
    private Integer snapshotId;

    // 远端snapshotID
    private Integer remoteSnapshotId;

    // 对接人员
    private String reception;

    // 部门信息
    private Department department;

    public Integer getRemoteSnapshotId() {
        return remoteSnapshotId;
    }

    public void setRemoteSnapshotId(Integer remoteSnapshotId) {
        this.remoteSnapshotId = remoteSnapshotId;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    }

    public List<UploadResource> getUploadResources() {
        return uploadResources;
    }

    public void setUploadResources(List<UploadResource> uploadResources) {
        this.uploadResources = uploadResources;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Integer getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Integer snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getReception() {
        return reception;
    }

    public void setReception(String reception) {
        this.reception = reception;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}
