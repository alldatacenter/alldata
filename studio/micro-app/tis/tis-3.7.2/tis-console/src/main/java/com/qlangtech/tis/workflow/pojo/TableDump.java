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
package com.qlangtech.tis.workflow.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TableDump implements Serializable {

    private Integer id;

    private Integer datasourceTableId;

    private String hiveTableName;

    /**
     * prop:1???ɹ?
     *     2??ʧ??
     *     3???????
     */
    private Byte state;

    private Byte isValid;

    private Date createTime;

    private Date opTime;

    /**
     * prop:??????¼dump????????dump?˶??????
     */
    private String info;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDatasourceTableId() {
        return datasourceTableId;
    }

    public void setDatasourceTableId(Integer datasourceTableId) {
        this.datasourceTableId = datasourceTableId;
    }

    public String getHiveTableName() {
        return hiveTableName;
    }

    public void setHiveTableName(String hiveTableName) {
        this.hiveTableName = hiveTableName == null ? null : hiveTableName.trim();
    }

    /**
     * get:1???ɹ?
     *     2??ʧ??
     *     3???????
     */
    public Byte getState() {
        return state;
    }

    /**
     * set:1???ɹ?
     *     2??ʧ??
     *     3???????
     */
    public void setState(Byte state) {
        this.state = state;
    }

    public Byte getIsValid() {
        return isValid;
    }

    public void setIsValid(Byte isValid) {
        this.isValid = isValid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    /**
     * get:??????¼dump????????dump?˶??????
     */
    public String getInfo() {
        return info;
    }

    /**
     * set:??????¼dump????????dump?˶??????
     */
    public void setInfo(String info) {
        this.info = info == null ? null : info.trim();
    }
}
