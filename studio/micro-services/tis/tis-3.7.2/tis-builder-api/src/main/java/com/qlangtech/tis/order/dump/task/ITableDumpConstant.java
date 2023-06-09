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
package com.qlangtech.tis.order.dump.task;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public interface ITableDumpConstant {

    // 在导但表时会将数据随机分到16个分区中
    int RAND_GROUP_NUMBER = 16;

    // 最多保存两个历史PT
    int MAX_PARTITION_SAVE = 2;

    String DUMP_START_TIME = "dump_starttime";

    // TableDumpFactory
    String DUMP_TABLE_DUMP_FACTORY_NAME = "dump_table_dump_factory_name";

    String JOB_NAME = "dump_job_name";

    String DUMP_TABLE_NAME = "dump_tableName";

    String DUMP_DBNAME = "dump_db_name";

    // String DUMP_TASK_ID = "dump_taskid";
    String DUMP_FORCE = "dump_force";
}
