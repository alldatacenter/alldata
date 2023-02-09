/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.util.Date;

/**
 * Udf info
 */
public class UdfInfo extends GenericResult {

    public UdfInfo(String name, String owner, String id, String desc, CannedUdfAcl acl, Date creationDate) {
        this.name = name;
        this.owner = owner;
        this.id = id;
        this.desc = desc;
        this.acl = acl;
        this.creationDate = creationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public CannedUdfAcl getAcl() {
        return acl;
    }

    public void setAcl(CannedUdfAcl acl) {
        this.acl = acl;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String toString() {
        return "UdfInfo [name=" + name + ", owner=" + owner + ", id=" + id + ", desc=" + desc + ", acl=" + acl
                + ", creationDate=" + creationDate + "]";
    }

    private String name;
    private String owner;
    private String id;
    private String desc;
    private CannedUdfAcl acl;
    private Date creationDate;
}
