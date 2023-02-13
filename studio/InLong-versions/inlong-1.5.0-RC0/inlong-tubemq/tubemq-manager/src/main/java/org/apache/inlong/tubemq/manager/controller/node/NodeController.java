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

package org.apache.inlong.tubemq.manager.controller.node;

import com.google.gson.Gson;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.node.dto.MasterDto;
import org.apache.inlong.tubemq.manager.controller.node.request.AddBrokersReq;
import org.apache.inlong.tubemq.manager.controller.node.request.BrokerSetReadOrWriteReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.inlong.tubemq.manager.controller.node.request.DeleteBrokerReq;
import org.apache.inlong.tubemq.manager.controller.node.request.ModifyBrokerReq;
import org.apache.inlong.tubemq.manager.controller.node.request.OnlineOfflineBrokerReq;
import org.apache.inlong.tubemq.manager.controller.node.request.ReloadBrokerReq;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.service.TubeConst;
import org.apache.inlong.tubemq.manager.service.TubeMQErrorConst;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/node")
@Slf4j
public class NodeController {

    private final Gson gson = new Gson();

    @Autowired
    NodeService nodeService;

    @Autowired
    MasterRepository masterRepository;

    @Autowired
    MasterService masterService;

    /**
     * query brokers' run status
     * this method supports batch operation
     */
    @RequestMapping(value = "/broker/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryBrokerDetail(
            @RequestParam Map<String, String> queryBody) throws Exception {
        String url = masterService.getQueryUrl(queryBody);
        return masterService.queryMaster(url);
    }

    /**
     * query brokers' configuration
     * this method supports batch operation
     */
    @RequestMapping(value = "/broker/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String queryBrokerConfig(
            @RequestParam Map<String, String> queryBody) throws Exception {
        String url = masterService.getQueryUrl(queryBody);
        return masterService.queryMaster(url);
    }

    /**
     * broker method proxy
     * divides the operation on broker to different method
     */
    @RequestMapping(value = "/broker")
    public @ResponseBody TubeMQResult brokerMethodProxy(@RequestParam String method, @RequestBody String req)
            throws Exception {
        switch (method) {
            case TubeConst.CLONE:
                return nodeService.cloneBrokersWithTopic(gson.fromJson(req, CloneBrokersReq.class));
            case TubeConst.ADD:
                return masterService.baseRequestMaster(gson.fromJson(req, AddBrokersReq.class));
            case TubeConst.MODIFY:
                return masterService.baseRequestMaster(gson.fromJson(req, ModifyBrokerReq.class));
            case TubeConst.ONLINE:
            case TubeConst.OFFLINE:
                return masterService.baseRequestMaster(gson.fromJson(req, OnlineOfflineBrokerReq.class));
            case TubeConst.RELOAD:
                return masterService.baseRequestMaster(gson.fromJson(req, ReloadBrokerReq.class));
            case TubeConst.DELETE:
                return masterService.baseRequestMaster(gson.fromJson(req, DeleteBrokerReq.class));
            case TubeConst.SET_READ_OR_WRITE:
                return masterService.baseRequestMaster(gson.fromJson(req, BrokerSetReadOrWriteReq.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

    @RequestMapping(value = "/master")
    public @ResponseBody TubeMQResult masterMethodProxy(@RequestParam String method, @RequestBody String req) {
        switch (method) {
            case TubeConst.MODIFY:
                return nodeService.modifyMasterNode(gson.fromJson(req, MasterDto.class));
            default:
                return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_METHOD);
        }
    }

}
