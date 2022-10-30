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
import org.apache.inlong.tubemq.client.common.PeerInfo;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.rv.RetValue;

public class ConsumerResult extends RetValue {
    private String topicName = "";
    private PeerInfo peerInfo = new PeerInfo();
    private String confirmContext = "";
    private List<Message> messageList = new ArrayList<>();

    public ConsumerResult() {
        super();
    }

    public ConsumerResult(int errCode, String errMsg) {
        super(errCode, errMsg);
    }

    public ConsumerResult(FetchContext taskContext) {
        super(taskContext.isSuccess(),
                taskContext.getErrCode(),
                taskContext.getErrMsg());
        this.topicName = taskContext.getPartition().getTopic();
        peerInfo.setMsgSourceInfo(taskContext.getPartition(),
                taskContext.getCurrOffset(), taskContext.getMaxOffset());
        if (this.isSuccess()) {
            this.messageList = taskContext.getMessageList();
            this.confirmContext = taskContext.getConfirmContext();
        }
    }

    public ConsumerResult(boolean isSuccess, int errCode, String errMsg,
                          String topicName, Partition partition,
                          long currOffset, long maxOffset) {
        super(isSuccess, errCode, errMsg);
        this.topicName = topicName;
        this.peerInfo.setMsgSourceInfo(partition, currOffset, maxOffset);
    }

    public void setSuccResult(String topicName, Partition partition,
                              long currOffset, long maxOffset) {
        super.setSuccResult();
        this.topicName = topicName;
        this.peerInfo.setMsgSourceInfo(partition, currOffset, maxOffset);
    }

    public void setProcessResult(boolean isSuccess, int errCode, String errMsg,
                                 String topicName, Partition partition,
                                 long currOffset, long maxOffset) {
        super.setFullInfo(isSuccess, errCode, errMsg);
        this.topicName = topicName;
        this.peerInfo.setMsgSourceInfo(partition, currOffset, maxOffset);
    }

    public String getTopicName() {
        return topicName;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public String getPartitionKey() {
        return peerInfo.getPartitionKey();
    }

    public final String getConfirmContext() {
        return confirmContext;
    }

    public long getCurrOffset() {
        return peerInfo.getCurrOffset();
    }

    public final List<Message> getMessageList() {
        return messageList;
    }

    public long getMaxOffset() {
        return peerInfo.getMaxOffset();
    }
}
