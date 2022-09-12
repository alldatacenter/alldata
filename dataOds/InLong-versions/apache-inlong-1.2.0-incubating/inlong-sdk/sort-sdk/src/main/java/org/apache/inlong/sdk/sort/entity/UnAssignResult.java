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

public class UnAssignResult {

    public final boolean succ;

    public final long costTime;

    public final String errorMsg;

    public long lastCommitOffset;

    public boolean callListenerSucc;

    public UnAssignResult(boolean succ, long costTime, String errorMsg) {
        this.succ = succ;
        this.costTime = costTime;
        this.errorMsg = errorMsg;
    }

    public UnAssignResult(boolean succ, long costTime, String errorMsg,
            long lastCommitOffset) {
        this.succ = succ;
        this.costTime = costTime;
        this.errorMsg = errorMsg;
        this.lastCommitOffset = lastCommitOffset;
    }

    @Override
    public String toString() {
        return "UnAssignResult ["
                + "succ=" + succ
                + ", costTime=" + costTime
                + ", errorMsg=" + errorMsg
                + ", lastCommitOffset=" + lastCommitOffset
                + ", callListenerSucc=" + callListenerSucc
                + "]";
    }

}
