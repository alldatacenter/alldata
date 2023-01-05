/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.fullbuild.indexbuild;

import com.google.common.collect.Maps;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.order.dump.task.ITestDumpCommon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-03 16:29
 */
public class MockTaskContextUtils {

    public static int TEST_TASK_ID = 1234567;
    public static final ThreadLocal<SimpleDateFormat> timeFormatYyyyMMddHHmmss = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            return format;
        }
    };

    public static TaskContext create(Date startTime) {

        final String startTimeStamp = timeFormatYyyyMMddHHmmss.get().format(startTime);

        Map<String, String> params = Maps.newHashMap();

        params.put(ITableDumpConstant.DUMP_START_TIME, startTimeStamp);
        params.put(ITableDumpConstant.JOB_NAME, ITestDumpCommon.DB_EMPLOYEES + "." + ITestDumpCommon.TABLE_EMPLOYEES);
        params.put(ITableDumpConstant.DUMP_TABLE_NAME, ITestDumpCommon.TABLE_EMPLOYEES);
        params.put(ITableDumpConstant.DUMP_DBNAME, ITestDumpCommon.DB_EMPLOYEES);
        params.put(JobCommon.KEY_TASK_ID, String.valueOf(TEST_TASK_ID));
        // 有已经导入的数据存在是否有必要重新导入
        params.put(ITableDumpConstant.DUMP_FORCE, "true");
        return TaskContext.create(params);
    }
}
