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

/**
 *
 */
package com.qlangtech.tis.trigger;

import com.alibaba.fastjson.JSON;


/**
 * @date 2014年10月30日下午12:02:48
 */
public class TriggerTaskConfig extends SourceType {

    public static final String DS_TYPE_TDDL = "tddl";
    public static final String DS_TYPE_RDS = "rds";
    public static final String DS_TYPE_ODPS = "odps";
    // 任务执行类型
    private ExecType execType;

    /**
     * 用户创建索引的时候定义规格（qps）
     */
    private Integer maxQPS;

    public static void main(String[] arg) {
        String value = "{\"appName\":\"JST_TERMHOME_APP\",\"cols\":[\"dpt_id\",\"is_auto_deploy\",\"update_time\",\"create_time\",\"price\"],\"dbs\":{\"JST_TERMINATORHOME_GROUP\":[\"application\"]},\"logicTableName\":\"application\",\"maxDumpCount\":50,\"odpsConfig\":{\"accessId\":\"07mbAZ3it1QaLTE8\",\"accessKey\":\"BEGxJuH9JY7E6AYR2qWoHSjvufN0cV\",\"dailyPartition\":{\"key\":\"dp\",\"value\":\"20141101\"},\"datatunelEndPoint\":\"http://dt.odps.aliyun.com\",\"groupPartition\":\"gp\",\"project\":\"jst_tsearcher\",\"serviceEndPoint\":\"http://service.odps.aliyun.com/api\",\"shallIgnorPartition\":false},\"shareId\":\"dpt_id\",\"taskId\":1111,\"type\":\"tddl\"}";
        TriggerTaskConfig.parse(value);
    }

    /**
     * 序列化
     *
     * @param config
     * @return
     */
    public static String serialize(TriggerTaskConfig config) {
        JSON json = (JSON) JSON.toJSON(config);
        return json.toJSONString();
    }

    /**
     * 解析一个对象
     *
     * @param value
     * @return
     */
    public static TriggerTaskConfig parse(String value) {

        SourceType type = JSON.parseObject(value, SourceType.class);

//        if (DS_TYPE_TDDL.equals(type.getType())) {
//            return JSON.parseObject(value, TddlTaskConfig.class);
//        } else if (DS_TYPE_RDS.equals(type.getType())) {
//            return JSON.parseObject(value, RDSTaskConfig.class);
//        } else if (DS_TYPE_ODPS.equals(type.getType())
//                || "bcrds".equals(type.getType())) {
//            return JSON.parseObject(value, ODPSTaskConfig.class);
//        } else {
        throw new IllegalArgumentException("illegal source type:" + type
                + ",value:" + value);
        //}
    }

    public Integer getMaxQPS() {
        return maxQPS;
    }

    public void setMaxQPS(Integer maxQPS) {
        this.maxQPS = maxQPS;
    }

    public ExecType getExecType() {
        return execType;
    }

    public void setExecType(ExecType execType) {
        this.execType = execType;
    }

    public final String getAppName() {
        return appName;
    }

    public final void setAppName(String appName) {
        this.appName = appName;
    }

    private int taskid;
    protected String appName;
    private Long maxDumpCount;

    /**
     *
     */
    public TriggerTaskConfig() {
        super();
    }

    public int getTaskId() {
        return taskid;
    }

    public void setTaskId(int taskId) {
        this.taskid = taskId;
    }

    public Long getMaxDumpCount() {
        return maxDumpCount;
    }

    public void setMaxDumpCount(Long maxDumpCount) {
        this.maxDumpCount = maxDumpCount;
    }

}
