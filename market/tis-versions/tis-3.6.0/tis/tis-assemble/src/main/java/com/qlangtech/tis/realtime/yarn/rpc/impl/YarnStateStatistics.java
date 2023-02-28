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
package com.qlangtech.tis.realtime.yarn.rpc.impl;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class YarnStateStatistics {

    private long tbTPS;

    private long sorlTPS;

    private long queueRC;

    private String from;

    private boolean paused;

    private long tis30sAvgRT;

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public long getTbTPS() {
        return tbTPS;
    }

    public void setTbTPS(long tbTPS) {
        this.tbTPS = tbTPS;
    }

    public long getSorlTPS() {
        return sorlTPS;
    }

    public void setSorlTPS(long sorlTPS) {
        this.sorlTPS = sorlTPS;
    }

    public long getQueueRC() {
        return queueRC;
    }

    public void setQueueRC(long queueRC) {
        this.queueRC = queueRC;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getTis30sAvgRT() {
        return tis30sAvgRT;
    }

    public void setTis30sAvgRT(long tis30sAvgRT) {
        this.tis30sAvgRT = tis30sAvgRT;
    }
}
