/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.constant;

/**
 * Constants of job fetcher.
 */
public class FetcherConstants {

    public static final String AGENT_FETCHER_INTERVAL = "agent.fetcher.interval";
    public static final int DEFAULT_AGENT_FETCHER_INTERVAL = 10;

    public static final String AGENT_HEARTBEAT_INTERVAL = "agent.heartbeat.interval";
    public static final int DEFAULT_AGENT_HEARTBEAT_INTERVAL = 10;
    public static final String AGENT_MANAGER_REQUEST_TIMEOUT = "agent.manager.request.timeout";
    // default is 30s
    public static final int DEFAULT_AGENT_MANAGER_REQUEST_TIMEOUT = 30;

    // required config
    public static final String AGENT_MANAGER_VIP_HTTP_HOST = "agent.manager.vip.http.host";
    public static final String AGENT_MANAGER_VIP_HTTP_PORT = "agent.manager.vip.http.port";

    public static final String AGENT_MANAGER_VIP_HTTP_PATH = "agent.manager.vip.http.managerIp.path";
    public static final String DEFAULT_AGENT_TDM_VIP_HTTP_PATH = "/agent/getInLongManagerIp";

    public static final String AGENT_MANAGER_VIP_HTTP_PREFIX_PATH = "agent.manager.vip.http.prefix.path";
    public static final String DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH = "/api/inlong/manager/openapi";

    public static final String AGENT_MANAGER_TASK_HTTP_PATH = "agent.manager.task.http.path";
    public static final String DEFAULT_AGENT_MANAGER_TASK_HTTP_PATH = "/agent/reportAndGetTask";

    public static final String AGENT_MANAGER_IP_CHECK_HTTP_PATH = "agent.manager.vip.http.checkIP.path";
    public static final String DEFAULT_AGENT_TDM_IP_CHECK_HTTP_PATH = "/fileAgent/confirmAgentIp";

    public static final String AGENT_MANAGER_DBCOLLECT_GETTASK_HTTP_PATH = "agent.manager.dbcollect.gettask.http.path";
    public static final String DEFAULT_AGENT_MANAGER_DBCOLLECTOR_GETTASK_HTTP_PATH = "/dbCollector/getTask";

    public static final String AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH = "agent.manager.reportsnapshot.http.path";
    public static final String DEFAULT_AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH = "/agent/reportSnapshot";

    public static final String AGENT_HTTP_APPLICATION_JSON = "application/json";

    public static final int AGENT_HTTP_SUCCESS_CODE = 200;

    public static final String DEFAULT_LOCAL_IP = "127.0.0.1";

    public static final String AGENT_MANAGER_RETURN_PARAM_IP = "ip";
    public static final String AGENT_MANAGER_RETURN_PARAM_DATA = "data";

    public static final String VERSION = "1.0";

}
