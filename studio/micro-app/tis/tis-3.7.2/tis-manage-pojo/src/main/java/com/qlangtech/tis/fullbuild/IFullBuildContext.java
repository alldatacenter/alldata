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
package com.qlangtech.tis.fullbuild;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年8月20日
 */
public interface IFullBuildContext {

    // 在执行Hive任务时候，不执行具体任务只执行建表任务
    String DRY_RUN = "dryRun";
    // 索引分区数目

    String KEY_APP_SHARD_COUNT = "index_shard_count";

    String KEY_APP_SHARD_COUNT_SINGLE = "1";

    String KEY_APP_NAME = "appname";

    String KEY_WORKFLOW_ID = "workflow_id";

    String KEY_WORKFLOW_NAME = "workflow_name";

    String KEY_ER_RULES = "er_rules";

    // com.qlangtech.tis.assemble.FullbuildPhase
    // String COMPONENT_START = "component.start";
    // String COMPONENT_END = "component.end";
    // String KEY_WORKFLOW_DETAIL = "workflowDetail";
    // 定时或者手动？
    String KEY_TRIGGER_TYPE = "triggertype";

    String KEY_BUILD_HISTORY_TASK_ID = "history.task.id";
    String NAME_APP_DIR = "ap";
    // public static final File parent = new File(
    // "D:\\j2ee_solution\\eclipse-standard-kepler-SR2-win32-x86_64\\workspace\\tis-saturn\\tis-sql-parser\\src\\main\\resources\\totalpaytest");
    String NAME_DATAFLOW_DIR = "df";
}
