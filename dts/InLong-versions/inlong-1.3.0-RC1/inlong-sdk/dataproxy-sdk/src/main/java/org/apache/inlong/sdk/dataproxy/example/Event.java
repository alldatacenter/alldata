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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.example;

import java.util.ArrayList;

public class Event {
    private byte[] body;
    private String groupId;
    private String streamId;
    private long dt;
    private int tryTimes = 0;
    ArrayList<byte[]> bodylist = new ArrayList<byte[]>();

    public Event(byte[] body, String groupId, String streamId, long dt) {
        super();
        this.body = body;
        this.groupId = groupId;
        this.streamId = streamId;
        this.dt = dt;
        this.setTryTimes(0);
    }

    public Event(ArrayList<byte[]> bodylist, String groupId, String streamId, long dt) {
        super();
        this.bodylist = bodylist;
        this.groupId = groupId;
        this.streamId = streamId;
        this.dt = dt;
        this.setTryTimes(0);
    }

    public ArrayList<byte[]> getBodylist() {
        return bodylist;
    }

    public void setBodylist(ArrayList<byte[]> bodylist) {
        this.bodylist = bodylist;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public int getTryTimes() {
        return tryTimes;
    }

    public void setTryTimes(int tryTimes) {
        this.tryTimes = tryTimes;
    }

}
