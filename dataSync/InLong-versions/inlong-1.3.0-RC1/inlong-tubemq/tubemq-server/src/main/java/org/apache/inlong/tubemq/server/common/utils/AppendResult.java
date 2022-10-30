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

package org.apache.inlong.tubemq.server.common.utils;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

// append result return
public class AppendResult {
    private boolean isSuccess = false;
    private long appendTime = TBaseConstants.META_VALUE_UNDEFINED;
    private long msgId;
    private long appendIndexOffset = TBaseConstants.META_VALUE_UNDEFINED;
    private long appendDataOffset = TBaseConstants.META_VALUE_UNDEFINED;

    public AppendResult() {

    }

    public void putReceivedInfo(long msgId, long appendTime) {
        this.msgId = msgId;
        this.appendTime = appendTime;
    }

    public void putAppendResult(long appendIndexOffset, long appendDataOffset) {
        this.isSuccess = true;
        this.appendIndexOffset = appendIndexOffset;
        this.appendDataOffset = appendDataOffset;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public long getMsgId() {
        return msgId;
    }

    public long getAppendTime() {
        return appendTime;
    }

    public long getAppendIndexOffset() {
        return appendIndexOffset;
    }

    public long getAppendDataOffset() {
        return appendDataOffset;
    }
}
