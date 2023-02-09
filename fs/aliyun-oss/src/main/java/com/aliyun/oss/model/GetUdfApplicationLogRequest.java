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

public class GetUdfApplicationLogRequest extends UdfGenericRequest {

    public GetUdfApplicationLogRequest(String udfName) {
        super(udfName);
    }

    public GetUdfApplicationLogRequest(String udfName, Date startTime) {
        super(udfName);
        this.startTime = startTime;
    }

    public GetUdfApplicationLogRequest(String udfName, Long endLines) {
        super(udfName);
        this.endLines = endLines;
    }

    public GetUdfApplicationLogRequest(String udfName, Date startTime, Long endLines) {
        super(udfName);
        this.startTime = startTime;
        this.endLines = endLines;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Long getEndLines() {
        return endLines;
    }

    public void setEndLines(Long endLines) {
        this.endLines = endLines;
    }

    private Date startTime;
    private Long endLines;

}
