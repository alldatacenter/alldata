/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.entity;

import java.util.List;

public class ConsumeStatusParams {

    private String subscribedId;
    private String token;
    private String ip;
    private int recvCmdPort;
    private int consumeType;
    private List<ConsumeState> consumeStates;

    public String getSubscribedId() {
        return subscribedId;
    }

    public void setSubscribedId(String subscribedId) {
        this.subscribedId = subscribedId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getRecvCmdPort() {
        return recvCmdPort;
    }

    public void setRecvCmdPort(int recvCmdPort) {
        this.recvCmdPort = recvCmdPort;
    }

    public int getConsumeType() {
        return consumeType;
    }

    public void setConsumeType(int consumeType) {
        this.consumeType = consumeType;
    }

    public List<ConsumeState> getConsumeStates() {
        return consumeStates;
    }

    public void setConsumeStates(List<ConsumeState> consumeStates) {
        this.consumeStates = consumeStates;
    }
}
