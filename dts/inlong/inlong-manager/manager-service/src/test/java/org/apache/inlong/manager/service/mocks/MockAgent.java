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

package org.apache.inlong.manager.service.mocks;

import com.google.common.collect.Sets;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatReportRequest;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.core.HeartbeatService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.service.ServiceBaseTest.GLOBAL_OPERATOR;

public class MockAgent {

    // mock ability
    // 1. Regularly pull tasks from the manager and process the tasks
    // 2. Regularly report the previously executed tasks to the manager (may be successful or fail)
    public static final String LOCAL_IP = "127.0.0.1";
    public static final String LOCAL_PORT = "8008";
    public static final String LOCAL_GROUP = "default_group";
    public static final String CLUSTER_TAG = "default_cluster_tag";
    public static final String CLUSTER_NAME = "1c59ef9e8e1e43cfb25ee8b744c9c81b_2790";

    private AgentService agentService;
    private HeartbeatService heartbeatService;

    private Queue<CommandEntity> commands = new LinkedList<>();
    private Set<String> groups = Sets.newHashSet(LOCAL_GROUP);
    private int jobLimit;

    public MockAgent(AgentService agentService, HeartbeatService heartbeatService, int jobLimit) {
        this.agentService = agentService;
        this.heartbeatService = heartbeatService;
        this.jobLimit = jobLimit;
    }

    public TaskResult pullTask() {
        TaskRequest request = new TaskRequest();
        request.setAgentIp(LOCAL_IP);
        request.setClusterName(CLUSTER_NAME);
        request.setPullJobType(PullJobTypeEnum.NEW.getType());
        request.setCommandInfo(new ArrayList<>());
        while (!commands.isEmpty()) {
            request.getCommandInfo().add(commands.poll());
        }
        agentService.report(request);
        TaskResult result = agentService.getTaskResult(request);
        mockHandleTask(result);
        return result;
    }

    public void sendHeartbeat() {
        HeartbeatReportRequest heartbeat = new HeartbeatReportRequest();
        heartbeat.setIp(LOCAL_IP);
        heartbeat.setPort(LOCAL_PORT);
        heartbeat.setComponentType(ComponentTypeEnum.Agent.getType());
        heartbeat.setClusterName(CLUSTER_NAME);
        heartbeat.setClusterTag(CLUSTER_TAG);
        heartbeat.setNodeGroup(groups.stream().collect(Collectors.joining(InlongConstants.COMMA)));
        heartbeat.setInCharges(GLOBAL_OPERATOR);
        heartbeat.setReportTime(System.currentTimeMillis());
        heartbeatService.reportHeartbeat(heartbeat);
    }

    public void bindGroup(boolean bind, String group) {
        if (bind) {
            groups.add(group);
        } else {
            groups.remove(group);
        }
        sendHeartbeat();
    }

    private void mockHandleTask(TaskResult taskResult) {
        taskResult.getDataConfigs().forEach(dataConfig -> {
            CommandEntity command = new CommandEntity();

            command.setCommandResult(Constants.RESULT_SUCCESS);
            command.setTaskId(dataConfig.getTaskId());
            command.setVersion(dataConfig.getVersion());
            commands.offer(command);
        });
    }
}
