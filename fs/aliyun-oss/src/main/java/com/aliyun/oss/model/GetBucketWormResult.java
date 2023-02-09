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

public class GetBucketWormResult extends GenericResult {
    private String wormId;
    private String wormState;
    private int retentionPeriodInDays;

    public String getWormId() {
        return wormId;
    }

    public void setWormId(String wormId) {
        this.wormId = wormId;
    }

    public String getWormState() {
        return wormState;
    }

    public void setWormState(String wormState) {
        this.wormState = wormState;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }

    public void setRetentionPeriodInDays(int retentionPeriodInDays) {
        this.retentionPeriodInDays = retentionPeriodInDays;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    private Date creationDate;
}
