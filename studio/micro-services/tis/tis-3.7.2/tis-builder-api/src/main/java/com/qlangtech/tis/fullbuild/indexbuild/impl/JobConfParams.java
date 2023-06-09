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
package com.qlangtech.tis.fullbuild.indexbuild.impl;

import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.TaskContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.IndexBuildParam;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-23 14:04
 */
public class JobConfParams {

    private static final Logger logger = LoggerFactory.getLogger(JobConfParams.class);

    private Map<String, String> params = new HashMap<>();

    /**
     * @param table
     * @param startTime
     * @param dumpFactoryName 对应ParamsConfig的名称
     * @return
     */
    public static JobConfParams createTabDumpParams(TaskContext taskContext, IDumpTable table, long startTime, String dumpFactoryName) {
        JobConfParams jobConf = new JobConfParams();
        Objects.requireNonNull(dumpFactoryName, "paramConfigName can not be null");
        final String jobName = table.getDbName() + "." + table.getTableName();
        jobConf.set(ITableDumpConstant.DUMP_TABLE_DUMP_FACTORY_NAME, dumpFactoryName);
        jobConf.set(IndexBuildParam.JOB_TYPE, IndexBuildParam.JOB_TYPE_DUMP);
        jobConf.set(ITableDumpConstant.DUMP_START_TIME, String.valueOf(startTime));
        jobConf.set(ITableDumpConstant.JOB_NAME, jobName);
        jobConf.set(ITableDumpConstant.DUMP_TABLE_NAME, table.getTableName());
        jobConf.set(ITableDumpConstant.DUMP_DBNAME, table.getDbName());
        jobConf.set(JobCommon.KEY_TASK_ID, String.valueOf(taskContext.getTaskId()));
        // 有已经导入的数据存在是否有必要重新导入
        jobConf.set(ITableDumpConstant.DUMP_FORCE, "true");
        return jobConf;
    }

    public void set(String key, String value) {
        this.params.put(key, value);
    }

    public String[] paramsArray() {
        List<String> params = paramsList();
        return params.toArray(new String[params.size()]);
    }

    private List<String> paramsList() {
        List<String> plist = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            plist.add("-" + entry.getKey());
            plist.add(entry.getValue());
            // buffer.append(" -").append(entry.getKey()).append(" ").append(entry.getValue());
        }
        return plist;
        // logger.info("main(String[] args),param:" + buffer.toString());
        // return plist.toArray(new String[plist.size()]);
        // return buffer;
    }

    public String paramSerialize() {
        return paramsList().stream().collect(Collectors.joining(" "));
        // StringBuffer buffer = new StringBuffer();
        // for (Map.Entry<String, String> entry : params.entrySet()) {
        // if (StringUtils.isBlank(entry.getValue())) {
        // continue;
        // }
        // buffer.append(" -").append(entry.getKey()).append(" ").append(entry.getValue());
        // }
        // logger.info("main(String[] args),param:" + buffer.toString());
        // return buffer;
    }
}
