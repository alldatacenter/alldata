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

package org.apache.inlong.tubemq.manager.controller.topic;

import com.google.gson.Gson;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.node.request.BatchAddTopicReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneTopicReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.DeleteTopicReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.ModifyTopicReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.QueryCanWriteReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.SetAuthControlReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.SetPublishReq;
import org.apache.inlong.tubemq.manager.controller.topic.request.SetSubscribeReq;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.apache.inlong.tubemq.manager.service.interfaces.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/topic")
@Slf4j
public class TopicWebController {

    @Autowired
    private NodeService nodeService;

    private Gson gson = new Gson();

    @Autowired
    private MasterService masterService;

    @Autowired
    private TopicService topicService;

    /**
     * broker method proxy
     * divides the operation on broker to different method
     */
    @RequestMapping(value = "")
    public @ResponseBody TubeMQResult topicMethodProxy(@RequestParam String method, @RequestBody String req)
            throws Exception {
        switch (method) {
            case TubeConst.ADD:
                return masterService.baseRequestMaster(gson.fromJson(req, BatchAddTopicReq.class));
            case TubeConst.CLONE:
                return nodeService.cloneTopicToBrokers(gson.fromJson(req, CloneTopicReq.class));
            case TubeConst.AUTH_CONTROL:
                return setAuthControl(gson.fromJson(req, SetAuthControlReq.class));
            case TubeConst.MODIFY:
                return masterService.baseRequestMaster(gson.fromJson(req, ModifyTopicReq.class));
            case TubeConst.DELETE:
            case TubeConst.REMOVE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteTopicReq.class));
            case TubeConst.QUERY_CAN_WRITE:
                return queryCanWrite(gson.fromJson(req, QueryCanWriteReq.class));
            case TubeConst.PUBLISH:
                return masterService.baseRequestMaster(gson.fromJson(req, SetPublishReq.class));
            case TubeConst.SUBSCRIBE:
                return masterService.baseRequestMaster(gson.fromJson(req, SetSubscribeReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    private TubeMQResult setAuthControl(SetAuthControlReq req) {
        req.setMethod(TubeConst.SET_AUTH_CONTROL);
        req.setType(TubeConst.OP_MODIFY);
        req.setCreateUser(TubeConst.TUBEADMIN);
        return masterService.baseRequestMaster(req);
    }

    private TubeMQResult queryCanWrite(QueryCanWriteReq req) {
        if (!req.legal()) {
            return TubeMQResult.errorResult(TubeMQErrorConst.PARAM_ILLEGAL);
        }
        return topicService.queryCanWrite(req.getTopicName(), req.getClusterId());
    }

    /**
     * query consumer auth control, shows all consumer groups
     *
     * @param req
     * @return
     *
     * @throws Exception the exception
     */
    @GetMapping("/consumerAuth")
    public @ResponseBody String queryConsumerAuth(
            @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }

    /**
     * query topic config info
     *
     * @param req
     * @return
     *
     * @throws Exception the exception
     */
    @GetMapping("/topicConfig")
    public @ResponseBody String queryTopicConfig(
            @RequestParam Map<String, String> req) throws Exception {
        String url = masterService.getQueryUrl(req);
        return masterService.queryMaster(url);
    }

}
