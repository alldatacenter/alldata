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

package org.apache.inlong.tubemq.manager.controller.group;

import com.google.gson.Gson;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.group.request.AddBlackGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.BatchDeleteGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.DeleteBlackGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.inlong.tubemq.manager.controller.group.request.FilterCondGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.FlowControlGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.QueryConsumerGroupReq;
import org.apache.inlong.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneOffsetReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.BatchAddGroupAuthReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.DeleteGroupReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.RebalanceConsumerReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.RebalanceGroupReq;
import org.apache.inlong.tubemq.manager.service.TopicServiceImpl;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/group")
@Slf4j
public class GroupController {

    private Gson gson = new Gson();

    @Autowired
    private MasterService masterService;

    @Autowired
    private TopicServiceImpl topicService;

    /**
     * Consumer group related request operations
     *
     * @param method method type
     * @param req incoming data
     * @return return request data
     */
    @ResponseBody
    @PostMapping("")
    public TubeMQResult groupMethodProxy(@RequestParam String method, @RequestBody String req) {
        switch (method) {
            case TubeConst.ADD:
                return batchAddGroup(gson.fromJson(req, BatchAddGroupAuthReq.class));
            case TubeConst.DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteGroupReq.class));
            case TubeConst.BATCH_DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, BatchDeleteGroupReq.class));
            case TubeConst.REBALANCE_CONSUMER_GROUP:
                return topicService.rebalanceGroup(gson.fromJson(req, RebalanceGroupReq.class));
            case TubeConst.REBALANCE_CONSUMER:
                return masterService.baseRequestMaster(gson.fromJson(req, RebalanceConsumerReq.class));
            case TubeConst.FILTER_CONDITION:
                return masterService.baseRequestMaster(gson.fromJson(req, FilterCondGroupReq.class));
            case TubeConst.FLOW_CONTROL:
                return masterService.baseRequestMaster(gson.fromJson(req, FlowControlGroupReq.class));
            case TubeConst.QUERY:
                return queryGroupExist(gson.fromJson(req, QueryConsumerGroupReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    /**
     * query group exist
     * @param req
     * @return
     */
    private TubeMQResult queryGroupExist(QueryConsumerGroupReq req) {
        if (!req.legal()) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        return topicService.queryGroupExist(req);
    }

    /**
     * add groups in one batch
     *
     * @param req
     * @return
     */
    private TubeMQResult batchAddGroup(BatchAddGroupAuthReq req) {
        req.setMethod(TubeConst.BATCH_ADD_GROUP_METHOD);
        req.setType(TubeConst.OP_MODIFY);
        req.setCreateUser(TubeConst.TUBEADMIN);
        return masterService.baseRequestMaster(req);
    }

    /**
     * query the consumer group for certain topic
     *
     * @param req
     * @return
     *
     * @throws Exception the exception
     */
    @GetMapping("/")
    public @ResponseBody String queryConsumer(
            @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }

    @PostMapping("/offset")
    public @ResponseBody TubeMQResult offsetProxy(
            @RequestParam String method, @RequestBody String req) {
        switch (method) {
            case TubeConst.CLONE:
                return topicService.cloneOffsetToOtherGroups(gson.fromJson(req, CloneOffsetReq.class));
            case TubeConst.DELETE:
                return topicService.deleteOffset(gson.fromJson(req, DeleteOffsetReq.class));
            case TubeConst.QUERY:
                return topicService.queryOffset(gson.fromJson(req, QueryOffsetReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    @PostMapping("/blackGroup")
    public @ResponseBody TubeMQResult blackGroupProxy(
            @RequestParam String method, @RequestBody String req) {
        switch (method) {
            case TubeConst.ADD:
                return masterService.baseRequestMaster(gson.fromJson(req, AddBlackGroupReq.class));
            case TubeConst.DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteBlackGroupReq.class));
            case TubeConst.BATCH_DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, BatchDeleteGroupReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    /**
     * query the black list for certain topic
     *
     * @param req
     * @return
     *
     * @throws Exception the exception
     */
    @GetMapping("/blackGroup")
    public @ResponseBody String queryBlackGroup(
            @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }

}
