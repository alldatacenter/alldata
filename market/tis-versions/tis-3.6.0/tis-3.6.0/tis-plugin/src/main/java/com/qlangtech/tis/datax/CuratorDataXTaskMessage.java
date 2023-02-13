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

package com.qlangtech.tis.datax;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-06 15:01
 **/
public class CuratorDataXTaskMessage {
    private String dataXName;

    private Integer jobId;

    private String jobName;
    // 估计总记录数目
    private Integer allRowsApproximately;

    private String execTimeStamp;

    public String getExecTimeStamp() {
        return execTimeStamp;
    }

    public void setExecTimeStamp(String execTimeStamp) {
        this.execTimeStamp = execTimeStamp;
    }

    public Integer getAllRowsApproximately() {
        return allRowsApproximately;
    }

    public void setAllRowsApproximately(Integer allRowsApproximately) {
        this.allRowsApproximately = allRowsApproximately;
    }

    public String getDataXName() {
        return dataXName;
    }

    public Integer getJobId() {
        return jobId;
    }


    public String getJobName() {
        return jobName;
    }

    public void setDataXName(String dataXName) {
        this.dataXName = dataXName;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

//    public void setJobPath(String jobPath) {
//        this.jobPath = jobPath;
//    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
}
