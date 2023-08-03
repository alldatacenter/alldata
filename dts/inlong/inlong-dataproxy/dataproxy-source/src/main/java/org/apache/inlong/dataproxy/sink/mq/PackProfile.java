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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;

import org.apache.flume.Event;

/**
 * 
 * DispatchProfile
 */
public abstract class PackProfile {

    private final String inlongGroupId;
    private final String inlongStreamId;
    private final long dispatchTime;
    private final long createTime = System.currentTimeMillis();
    private final String uid;
    protected long count = 0;
    protected long size = 0;
    protected final boolean enableRetryAfterFailure;
    protected final int maxRetries;
    protected int retries = 0;
    /**
     * Constructor
     *
     * @param uid  the inlong id
     * @param inlongGroupId   the group id
     * @param inlongStreamId  the stream id
     * @param dispatchTime the dispatch time
     */
    public PackProfile(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        this.uid = uid;
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        this.dispatchTime = dispatchTime;
        this.enableRetryAfterFailure = CommonConfigHolder.getInstance().isEnableSendRetryAfterFailure();
        this.maxRetries = CommonConfigHolder.getInstance().getMaxRetriesAfterFailure();
    }

    /**
     * get uid
     * 
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * get inlongGroupId
     * 
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * get inlongStreamId
     * 
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * getDispatchTime
     * 
     * @return the dispatch time
     */
    public long getDispatchTime() {
        return dispatchTime;
    }

    /**
     * get count
     *
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * set count
     *
     * @param count the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * get size
     *
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * set size
     *
     * @param size the size to set
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * isTimeout
     *
     * @param  createThreshold  the creation threshold
     * @return whether time out
     */
    public boolean isTimeout(long createThreshold) {
        return createThreshold >= createTime;
    }

    /**
     * ack
     */
    public abstract void ack();

    /**
     * fail
     */
    public abstract void fail(DataProxyErrCode errCode, String errMsg);

    /**
     * isResend
     * @return whether resend message
     */
    public abstract boolean isResend();

    /**
     * addEvent
     *
     * @param  event   the event need to add
     * @param  maxPackCount   the max package count
     * @param  maxPackSize    the max package size
     * @return whether add success
     */
    public abstract boolean addEvent(Event event, long maxPackCount, long maxPackSize);

}
