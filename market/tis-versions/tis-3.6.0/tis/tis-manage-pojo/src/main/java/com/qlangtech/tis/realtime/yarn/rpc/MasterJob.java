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
package com.qlangtech.tis.realtime.yarn.rpc;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月9日
 */
public class MasterJob {

    private JobType jobType;

    private boolean stop;

    private String indexName;

    private String uuid;

    private long createTime;

    // 构造函数用于反序列化，不能删除！！！！
    public MasterJob() {
    }

    public MasterJob(JobType jobType, String indexName, String uuid) {
        super();
        this.jobType = jobType;
        this.indexName = indexName;
        this.uuid = uuid;
        this.createTime = ConsumeDataKeeper.getCurrentTimeInSec();
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getIndexName() {
        return indexName;
    }

    public JobType getJobType() {
        return this.jobType;
    }

    public boolean isCollectionIncrProcessCommand(String indexName) {
        return (this.getJobType() == JobType.IndexJobRunning) && StringUtils.equals(indexName, this.getIndexName());
    }

    public String getUUID() {
        return uuid;
    }

    public long getCreateTime() {
        return createTime;
    }

    // @Override
    // public void write(DataOutput out) throws IOException {
    // out.writeInt(jobType.getValue());
    // out.writeBoolean(this.isStop());
    // WritableUtils.writeString(out, indexName);
    // WritableUtils.writeString(out, uuid);
    // out.writeLong(createTime);
    // }
    //
    // @Override
    // public void readFields(DataInput in) throws IOException {
    // this.jobType = JobType.parseJobType(in.readInt());
    // this.setStop(in.readBoolean());
    // this.indexName = WritableUtils.readString(in);
    // this.uuid = WritableUtils.readString(in);
    // this.createTime = in.readLong();
    // }
    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
