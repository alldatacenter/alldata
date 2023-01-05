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
package com.qlangtech.tis.order.center;

import com.qlangtech.tis.exec.ExecutePhaseRange;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月11日 上午11:09:21
 */
public interface IParamContext {

    public static String getCurrentTimeStamp() {
        DateTimeFormatter yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.now().format(yyyyMMddHHmmss);
    }

    String yyyyMMddHHmmssMMMPattern = "yyyyMMddHHmmssSSS";
    DateTimeFormatter yyyyMMddHHmmssMMM = DateTimeFormatter.ofPattern(yyyyMMddHHmmssMMMPattern);

    public static String getCurrentMillisecTimeStamp() {
        return LocalDateTime.now().format(yyyyMMddHHmmssMMM);
    }

    public static void main(String[] args) {
        System.out.println(getCurrentMillisecTimeStamp());

        LocalDateTime.parse(getCurrentMillisecTimeStamp(),yyyyMMddHHmmssMMM);

       // yyyyMMddHHmmssMMM.parse(getCurrentMillisecTimeStamp());
    }

    public String KEY_PARTITION = "ps";

    public String COMPONENT_START = "component.start";

    public String COMPONENT_END = "component.end";

    public String KEY_ASYN_JOB_NAME = "asynJobName";
    public String KEY_ASYN_JOB_SUCCESS = "success";
    public String KEY_ASYN_JOB_COMPLETE = "complete";

    public String KEY_EXEC_RESULT = "execresult";

    public String KEY_BUILD_TARGET_TABLE_NAME = "targetTableName";

    public String KEY_BUILD_INDEXING_ALL_ROWS_COUNT = "indexing.all.rows.count";
    String KEY_REQUEST_DISABLE_TRANSACTION = "disableTransaction";

    ExecutePhaseRange getExecutePhaseRange();

    public String getString(String key);

    public boolean getBoolean(String key);

    public int getInt(String key);

    public long getLong(String key);

    public String getPartitionTimestamp();
}
