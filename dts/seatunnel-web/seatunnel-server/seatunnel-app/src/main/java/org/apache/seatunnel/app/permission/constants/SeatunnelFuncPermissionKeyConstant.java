/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.permission.constants;

public class SeatunnelFuncPermissionKeyConstant {

    /** cluster */
    public static final String CLUSTER_METRICS_VIEW = "monitor:cluster:view";

    /** sync task definition */
    public static final String CONNECTOR_SOURCES = "project:seatunnel-task:sources";

    public static final String CONNECTOR_TRANSFORMS = "project:seatunnel-task:transforms";
    public static final String CONNECTOR_SINKS = "project:seatunnel-task:sinks";
    public static final String CONNECTOR_SYNC = "project:seatunnel-task:sync";
    public static final String CONNECTOR_FORM = "project:seatunnel-task:form";
    public static final String CONNECTOR_DATASOURCE_SOURCES =
            "project:seatunnel-task:datasource-sources";
    public static final String CONNECTOR_DATASOURCE_TRANSFORMS =
            "project:seatunnel-task:datasource-transforms";
    public static final String CONNECTOR_DATASOURCE_SINKS =
            "project:seatunnel-task:datasource-sinks";
    public static final String CONNECTOR_DATASOURCE_FORM = "project:seatunnel-task:datasource-form";
    public static final String ENGIN_LIST = "project:seatunnel-task:engin-list";
    public static final String ENGIN_TYPE = "project:seatunnel-task:engin-type";
    public static final String ENV_VIEW = "project:seatunnel-task:env";
    public static final String JOB_CONFIG_UPDATE = "project:seatunnel-task:job-config-update";
    public static final String JOB_CONFIG_DETAIL = "project:seatunnel-task:job-config-detail";
    public static final String JOB_DEFINITION_VIEW = "project:seatunnel-task:view";
    public static final String JOB_DEFINITION_CREATE = "project:seatunnel-task:create";
    public static final String JOB_DEFINITION_DETAIL = "project:seatunnel-task:detail";
    public static final String JOB_DEFINITION_DELETE = "project:seatunnel-task:delete";
    public static final String JOB_TASK_DAG_CREATE = "project:seatunnel-task:job-dag-create";
    public static final String JOB_TASK_DETAIL = "project:seatunnel-task:job-detail";
    public static final String SINGLE_TASK_CREATE = "project:seatunnel-task:task-create";
    public static final String SINGLE_TASK_DETAIL = "project:seatunnel-task:task-detail";
    public static final String SINGLE_TASK_DELETE = "project:seatunnel-task:task-delete";
    public static final String JOB_TABLE_SCHEMA = "project:seatunnel-task:table-schema";
    public static final String JOB_TABLE_COLUMN_PROJECTION =
            "project:seatunnel-task:column-projection";
    public static final String JOB_EXECUTOR_RESOURCE = "project:seatunnel-task:job-exec-resource";
    public static final String JOB_EXECUTOR_INSTANCE = "project:seatunnel-task:job-exec-instance";
    public static final String JOB_EXECUTOR_COMPLETE = "project:seatunnel-task:job-exec-complete";

    /** sync task instance */
    public static final String JOB_METRICS_SUMMARY = "project:seatunnel-task-instance:summary";

    public static final String JOB_DETAIL = "project:seatunnel-task-instance:detail";
    public static final String JOB_DAG = "project:seatunnel-task-instance:dag";

    /** datasource */
    public static final String DATASOURCE_LIST = "datasource:list";

    public static final String DATASOURCE_CREATE = "datasource:create";
    public static final String DATASOURCE_UPDATE = "datasource:update";
    public static final String DATASOURCE_DELETE = "datasource:delete";
    public static final String DATASOURCE_TEST_CONNECT = "datasource:test-connect";
    public static final String DATASOURCE_DYNAMIC = "datasource:dynamic";
    public static final String DATASOURCE_DATABASES = "datasource:databases";
    public static final String DATASOURCE_TABLE = "datasource:table";
    public static final String DATASOURCE_TABLE_SCHEMA = "datasource:table-schema";
    public static final String DATASOURCE_QUERY_ALL = "datasource:query-all";
    public static final String DATASOURCE_DETAIL_LIST = "datasource:detail-list";
    public static final String DATASOURCE_DETAIL = "datasource:detail";
    public static final String VIRTUAL_TABLE_CREATE = "datasource:virtual-table-create";
    public static final String VIRTUAL_TABLE_UPDATE = "datasource:virtual-table-update";
    public static final String VIRTUAL_TABLE_DELETE = "datasource:virtual-table-delete";
    public static final String VIRTUAL_TABLE_VIEW = "datasource:virtual-table-view";
    public static final String VIRTUAL_TABLE_DETAIL = "datasource:virtual-table-detail";
}
