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

package org.apache.inlong.tubemq.client.consumer;

import org.apache.inlong.tubemq.corebase.cluster.Partition;

public class PartitionSelectResult {
    private boolean success;
    private int errCode;
    private String errMsg;
    private Partition partition;
    private long usedToken;
    private boolean isLastPackConsumed;

    public PartitionSelectResult(boolean success, int errCode, String errMsg) {
        this.success = success;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public PartitionSelectResult(boolean success, int errCode,
                                 String errMsg, Partition partition,
                                 long usedToken, boolean isLastPackConsumed) {
        this.success = success;
        this.errCode = errCode;
        this.errMsg = errMsg;
        this.partition = partition;
        this.usedToken = usedToken;
        this.isLastPackConsumed = isLastPackConsumed;
    }

    public PartitionSelectResult(Partition partition,
                                 long usedToken,
                                 boolean isLastPackConsumed) {
        this.success = true;
        this.partition = partition;
        this.usedToken = usedToken;
        this.isLastPackConsumed = isLastPackConsumed;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public int getErrCode() {
        return this.errCode;
    }

    public long getUsedToken() {
        return usedToken;
    }

    public Partition getPartition() {
        return partition;
    }

    public boolean isLastPackConsumed() {
        return isLastPackConsumed;
    }

}
