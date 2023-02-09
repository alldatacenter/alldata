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
 * Udf Image Info
 * 
 */
public class UdfApplicationInfo extends GenericResult {

    public UdfApplicationInfo(String name, String id, String region, String status, Integer imageVersion,
            Integer instanceNum, Date creationDate, InstanceFlavor flavor) {
        this.name = name;
        this.id = id;
        this.region = region;
        this.status = status;
        this.imageVersion = imageVersion;
        this.instanceNum = instanceNum;
        this.creationDate = creationDate;
        this.flavor = flavor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(Integer imageVersion) {
        this.imageVersion = imageVersion;
    }

    public Integer getInstanceNum() {
        return instanceNum;
    }

    public void setInstanceNum(Integer instanceNum) {
        this.instanceNum = instanceNum;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public InstanceFlavor getFlavor() {
        return flavor;
    }

    public void setFlavor(InstanceFlavor flavor) {
        this.flavor = flavor;
    }

    @Override
    public String toString() {
        return "UdfApplicationInfo [name=" + name + ", id=" + id + ", region=" + region + ", status=" + status
                + ", imageVersion=" + imageVersion + ", instanceNum=" + instanceNum + ", creationDate=" + creationDate
                + ", flavor=" + flavor + "]";
    }

    private String name;
    private String id;
    private String region;
    private String status;
    private Integer imageVersion;
    private Integer instanceNum;
    private Date creationDate;
    private InstanceFlavor flavor;

}
