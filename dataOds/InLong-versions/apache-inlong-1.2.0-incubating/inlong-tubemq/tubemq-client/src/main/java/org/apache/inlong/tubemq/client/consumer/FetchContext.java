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

import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.cluster.Partition;

public class FetchContext {

    private Partition partition;
    private long usedToken;
    private boolean lastConsumed = false;
    private boolean success = false;
    private int errCode = 0;
    private String errMsg = "";
    private long currOffset = TBaseConstants.META_VALUE_UNDEFINED;
    private String confirmContext = "";
    private List<Message> messageList = new ArrayList<>();
    private long maxOffset = TBaseConstants.META_VALUE_UNDEFINED;

    public FetchContext(PartitionSelectResult selectResult) {
        this.partition = selectResult.getPartition();
        this.usedToken = selectResult.getUsedToken();
        this.lastConsumed = selectResult.isLastPackConsumed();
    }

    public void setFailProcessResult(int errCode, String errMsg) {
        this.success = false;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public void setSuccessProcessResult(long currOffset,
                                        String confirmContext,
                                        List<Message> messageList,
                                        long maxOffset) {
        this.success = true;
        this.errCode = TErrCodeConstants.SUCCESS;
        this.errMsg = "Ok!";
        if (currOffset >= 0) {
            this.currOffset = currOffset;
        }
        this.confirmContext = confirmContext;
        this.messageList = messageList;
        if (maxOffset >= 0) {
            this.maxOffset = maxOffset;
        }
    }

    public Partition getPartition() {
        return partition;
    }

    public String getPartitionKey() {
        return partition.getPartitionKey();
    }

    public long getUsedToken() {
        return usedToken;
    }

    public boolean isLastConsumed() {
        return lastConsumed;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public long getCurrOffset() {
        return currOffset;
    }

    public String getConfirmContext() {
        return confirmContext;
    }

    public long getMaxOffset() {
        return maxOffset;
    }
}
