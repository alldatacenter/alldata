/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeRebInfo {

    private String clientId;
    private int status;
    private long createTime;
    private int reqType;
    private AtomicInteger waitDuration = new AtomicInteger(0);

    public NodeRebInfo(String clientId,
                       int waitDuration) {
        this.status = 0;
        this.clientId = clientId;
        this.waitDuration.set(waitDuration);
        this.createTime = System.currentTimeMillis();
        if (waitDuration <= 0) {
            this.reqType = 0;
        } else {
            this.reqType = 1;
        }
    }

    public NodeRebInfo(String clientId,
                       int status,
                       long createTime,
                       int reqType,
                       int waitDuration) {
        this.clientId = clientId;
        this.status = status;
        this.createTime = createTime;
        this.reqType = reqType;
        this.waitDuration.set(waitDuration);
    }

    public String getClientId() {
        return clientId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getReqType() {
        return reqType;
    }

    public int getWaitDuration() {
        return waitDuration.get();
    }

    public int decrAndGetWaitDuration() {
        return this.waitDuration.decrementAndGet();
    }

    @Override
    public NodeRebInfo clone() {
        return new NodeRebInfo(this.clientId,
                this.status, this.createTime,
                this.reqType, this.waitDuration.get());
    }

    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"clientId\":\"").append(clientId)
                .append("\",\"status\":").append(status)
                .append(",\"createTime\":").append(createTime)
                .append(",\"reqType\":").append(reqType)
                .append(",\"waitDuration\":").append(waitDuration.get())
                .append("}");
    }
}
