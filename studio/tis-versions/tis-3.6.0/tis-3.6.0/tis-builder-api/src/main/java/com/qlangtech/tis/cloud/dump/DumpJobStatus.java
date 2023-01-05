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
package com.qlangtech.tis.cloud.dump;

/**
 * @description
 * @version 1.0.0
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-7 下午07:50:54
 */
public class DumpJobStatus {

    // 业务Core名 serviceName-group
    private String coreName;

    // 任务提交人
    private String userName;

    // 时间点
    private String timepoint;

    // 完成百分比
    private float dumpProgressPercent;

    // 执行条数
    protected long excuteCount;

    // 开始时间
    protected long startTime;

    // 结束时间
    protected long endTime;

    // 全部Dump数目
    protected long alldumpCount;

    // dump种类，远程还是本地
    protected String dumpType;

    private static final String DEFAULT_MSG = "SUC";

    // 错误信息
    protected String failureInfo = DEFAULT_MSG;

    private int runState;

    /**
     * @return the dumpType
     */
    public String getDumpType() {
        return dumpType;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the timepoint
     */
    public String getTimepoint() {
        return timepoint;
    }

    /**
     * @param timepoint the timepoint to set
     */
    public void setTimepoint(String timepoint) {
        this.timepoint = timepoint;
    }

    /**
     * @param failureInfo the failureInfo to set
     */
    public void setFailureInfo(String failureInfo) {
        this.failureInfo = failureInfo;
    }

    /**
     * @param dumpType the dumpType to set
     */
    public void setDumpType(String dumpType) {
        this.dumpType = dumpType;
    }

    /**
     * @return the runstates
     */
    public static String[] getRunstates() {
        return runStates;
    }

    /**
     * @return the excuteCount
     */
    public long getExcuteCount() {
        return excuteCount;
    }

    /**
     * @param excuteCount the excuteCount to set
     */
    public void setExcuteCount(long excuteCount) {
        this.excuteCount = excuteCount;
    }

    /**
     * @return the coreName
     */
    public String getCoreName() {
        return coreName;
    }

    /**
     * @param coreName the coreName to set
     */
    public void setCoreName(String coreName) {
        this.coreName = coreName;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return the alldumpCount
     */
    public long getAlldumpCount() {
        return alldumpCount;
    }

    /**
     * @param alldumpCount the alldumpCount to set
     */
    public void setAlldumpCount(long alldumpCount) {
        this.alldumpCount = alldumpCount;
    }

    /**
     * @param runState the runState to set
     */
    public void setRunState(int runState) {
        this.runState = runState;
    }

    private DumpJobId jobid;

    /**
     * @param jobid the jobid to set
     */
    public void setDumpJobID(DumpJobId jobid) {
        this.jobid = jobid;
    }

    public static final int RUNNING = 1;

    public static final int FAILED = 2;

    public static final int SUCCEEDED = 3;

    public static final int PREP = 4;

    public static final int KILLED = 5;

    private static final String UNKNOWN = "UNKNOWN";

    private static final String[] runStates = { UNKNOWN, "RUNNING", "SUCCEEDED", "FAILED", "PREP", "KILLED" };

    public static String getJobRunState(int state) {
        if (state < 1 || state >= runStates.length) {
            return UNKNOWN;
        }
        return runStates[state];
    }

    public synchronized float getDumpProgressPercent() {
        return dumpProgressPercent;
    }

    public synchronized void setDumpProgressPercent(float percent) {
        this.dumpProgressPercent = percent;
    }

    public DumpJobId getDumpJobID() {
        return jobid;
    }

    public synchronized int getRunState() {
        return runState;
    }

    public synchronized String getFailureInfo() {
        return this.failureInfo;
    }
}
