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

package org.apache.inlong.tubemq.manager.controller.task;

import com.google.gson.Gson;

import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.topic.request.AddTopicTask;
import org.apache.inlong.tubemq.manager.controller.topic.request.BatchAddTopicTaskReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.apache.inlong.tubemq.manager.service.interfaces.ClusterService;
import org.apache.inlong.tubemq.manager.service.interfaces.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/task")
@Slf4j
public class TaskController {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private TaskService taskService;

    private Gson gson = new Gson();

    @RequestMapping(value = "")
    public @ResponseBody
        TubeMQResult taskMethodProxy(@RequestParam String method,
                                 @RequestBody String req) {
        switch (method) {
            case TubeConst.ADD_TOPIC_TASK:
                return addTask(gson.fromJson(req, BatchAddTopicTaskReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    public TubeMQResult addTask(BatchAddTopicTaskReq req) {
        if (!req.legal()) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        ClusterEntry clusterEntry = clusterService.getOneCluster(req
                .getClusterId());
        if (clusterEntry == null) {
            return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_CLUSTER);
        }
        Set<String> topicNames = req.getAddTopicTasks().stream()
                .map(AddTopicTask::getTopicName).collect(
                        Collectors.toSet());

        return taskService.addTopicCreateTask(req.getClusterId(), topicNames);
    }

}
