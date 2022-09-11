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

package org.apache.inlong.agent.pojo;

import com.google.gson.Gson;
import lombok.Data;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;

import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;

@Data
public class SqlJobProfileDto {

    public static final String SQL_JOB = "SQL_JOB";
    public static final String DEFAULT_CHANNEL = "org.apache.inlong.agent.plugin.channel.MemoryChannel";
    public static final String DEFAULT_DATAPROXY_SINK = "org.apache.inlong.agent.plugin.sinks.ProxySink";
    public static final String DEFAULT_SOURCE = "org.apache.inlong.agent.plugin.sources.DatabaseSqlSource";
    private static final Gson GSON = new Gson();

    private Job job;
    private Proxy proxy;

    private static Job getJob(DbCollectorTask task) {
        Sql sql = new Sql();
        sql.setHostname(task.getIp());
        sql.setPort(task.getDbport());
        sql.setUser(task.getUser());
        sql.setPassword(task.getPassword());
        sql.setCommand(task.getSqlStatement());
        Job job = new Job();
        job.setId(Integer.parseInt(task.getId()));
        job.setName(SQL_JOB);
        job.setSource(DEFAULT_SOURCE);
        job.setSink(DEFAULT_DATAPROXY_SINK);
        job.setChannel(DEFAULT_CHANNEL);
        job.setDbName(task.getDbName());
        job.setTotalLimit(task.getTotalLimit());
        job.setOnceLimit(task.getOnceLimit());
        job.setTimeLimit(task.getTimeLimit());
        job.setRetryTimes(task.getRetryTimes());
        job.setSql(sql);
        return job;
    }

    private static Proxy getProxy(DbCollectorTask task) {
        Proxy proxy = new Proxy();
        Manager manager = new Manager();
        AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
        manager.setHost(agentConf.get(AGENT_MANAGER_VIP_HTTP_HOST));
        manager.setPort(agentConf.get(AGENT_MANAGER_VIP_HTTP_PORT));
        proxy.setInlongGroupId(task.getInlongGroupId());
        proxy.setInlongStreamId(task.getInlongStreamId());
        proxy.setManager(manager);
        return proxy;
    }

    /**
     * convert DbCollectorTask to JobProfile
     */
    public static JobProfile convertToJobProfile(DbCollectorTask task) {
        if (!task.isValid()) {
            throw new IllegalArgumentException("input task" + task + "is invalid please check");
        }
        SqlJobProfileDto profileDto = new SqlJobProfileDto();
        Proxy proxy = getProxy(task);
        Job job = getJob(task);
        profileDto.setProxy(proxy);
        profileDto.setJob(job);
        return JobProfile.parseJsonStr(GSON.toJson(profileDto));
    }

    @Data
    public static class Sql {

        private String user;
        private String password;
        private String hostname;
        private Integer port;
        private String command;
    }

    @Data
    public static class Job {

        private int id;
        private String name;
        private String source;
        private String sink;
        private String channel;
        private String dbName;
        private Integer totalLimit;
        private Integer onceLimit;
        private Integer timeLimit;
        private Integer retryTimes;
        private Sql sql;
    }

    @Data
    public static class Manager {

        private String port;
        private String host;
    }

    @Data
    public static class Proxy {

        private String inlongGroupId;
        private String inlongStreamId;
        private Manager manager;
    }
}