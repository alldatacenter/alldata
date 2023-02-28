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
package io.datavines.server.api.controller;

import io.datavines.core.aop.RefreshToken;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.core.client.NotificationClient;
import io.datavines.server.api.dto.bo.sla.*;
import io.datavines.server.repository.service.SlaNotificationService;
import io.datavines.server.repository.service.SlaSenderService;
import io.datavines.server.repository.service.SlaService;
import io.datavines.server.repository.service.SlaJobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.*;

@Api(value = "sla", tags = "sla", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/sla", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
@Validated
@Slf4j
public class SlaController {

    @Autowired
    private SlaService slaService;

    @Autowired
    private SlaSenderService slaSenderService;

    @Autowired
    private SlaNotificationService slaNotificationService;

    @Autowired
    private NotificationClient client;

    @Autowired
    private SlaJobService slaJobService;

    @ApiOperation(value = "list job")
    @GetMapping(value = "/job/list")
    public Object listSlaJob(@RequestParam("slaId") Long id){
        return slaJobService.listSlaJob(id);
    }

    @ApiOperation(value = "create or update sla job")
    @PostMapping(value = "/job/createOrUpdate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createOrUpdateSlaJob(@Valid @RequestBody SlaJobCreateOrUpdate createOrUpdate){
        return slaJobService.createOrUpdateSlaJob(createOrUpdate);
    }

    @ApiOperation(value = "create sla job")
    @DeleteMapping(value = "/job/{slaJobId}")
    public Object deleteSlaJob(@PathVariable("slaJobId") Long  slaJobId){
        return slaJobService.removeById(slaJobId);
    }

    @ApiOperation(value = "test sla")
    @GetMapping(value = "/test/{slaId}")
    public Object test(@PathVariable("slaId") Long slaId){
        SlaNotificationMessage message = new SlaNotificationMessage();
        message.setMessage("[\"test\"]");
        message.setSubject("just test slaId");
        Map<SlaSenderMessage, Set<SlaConfigMessage>> configuration = slaNotificationService.getSlasNotificationConfigurationBySlasId(slaId);
        return client.notify(message, configuration);
    }

    @ApiOperation(value = "page list sla")
    @GetMapping(value = "/page")
    public Object listSlas(@RequestParam("workspaceId") Long workspaceId,
                           @RequestParam(value = "searchVal", required = false) String searchVal,
                           @RequestParam("pageNumber") Integer pageNumber,
                           @RequestParam("pageSize") Integer pageSize){
        return slaService.listSlas(workspaceId, searchVal, pageNumber, pageSize);
    }

    @ApiOperation(value = "create sla")
    @PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createSla(@Valid @RequestBody SlaCreate create){
        return slaService.createSla(create);
    }

    @ApiOperation(value = "update sla")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object updateSla(@Valid @RequestBody SlaUpdate update){
        return slaService.updateSla(update);
    }

    @ApiOperation(value = "get sla")
    @GetMapping(value = "{slaId}")
    public Object getSla(@PathVariable Long slaId){
        return slaService.getById(slaId);
    }

    @ApiOperation(value = "delete sla")
    @DeleteMapping(value = "/{id}")
    public Object deleteSla(@PathVariable("id") Long id){
        return slaService.deleteById(id);
    }

    @ApiOperation(value = "get support plugin")
    @GetMapping(value = "/plugin/support")
    public Object getSupportPlugin(){
        return slaService.getSupportPlugin();
    }

    @ApiOperation(value = "get config param of sender")
    @GetMapping(value = "/sender/config/{type}")
    public Object getSenderConfigJson(@PathVariable("type") String type){
        return slaService.getSenderConfigJson(type);
    }

    @ApiOperation(value = "get config param of notification")
    @GetMapping(value = "/notification/config/{type}")
    public Object getNotificationConfigJson(@PathVariable("type") String type){
        return slaNotificationService.getConfigJson(type);
    }

    @ApiOperation(value = "page list sender")
    @GetMapping(value = "/sender/page")
    public Object listSenders(@RequestParam("workspaceId") Long workspaceId,
                              @RequestParam(value = "searchVal", required = false) String searchVal,
                              @RequestParam("pageNumber") Integer pageNumber,
                              @RequestParam("pageSize") Integer pageSize){
        return slaSenderService.pageListSender(workspaceId, searchVal, pageNumber, pageSize);
    }

    @ApiOperation(value = " list sender")
    @GetMapping(value = "/sender/list")
    public Object listSenders(@RequestParam("workspaceId") Long workspaceId,
                              @RequestParam(value = "type") String type,
                              @RequestParam(value = "searchVal", required = false) String searchVal){
        return slaSenderService.listSenders(workspaceId, searchVal, type);
    }

    @ApiOperation(value = "create sender")
    @PostMapping(value = "/sender",consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createSender(@Valid @RequestBody SlaSenderCreate create){
        return slaSenderService.createSender(create);
    }

    @ApiOperation(value = "update sender")
    @PutMapping(value = "/sender",consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object updateSender(@Valid @RequestBody SlaSenderUpdate update){
        return slaSenderService.updateSender(update);
    }

    @ApiOperation(value = "delete sender")
    @DeleteMapping(value = "/sender/{id}")
    public Object deleteSender(@PathVariable("id") Long id){
        return slaSenderService.removeById(id);
    }

    @ApiOperation(value = "create notification")
    @PostMapping(value = "/notification")
    public Object createNotification(@RequestBody SlaNotificationCreate create){
        return slaNotificationService.createNotification(create);
    }

    @ApiOperation(value = "update notification")
    @PutMapping(value = "/notification")
    public Object updateNotification(@RequestBody SlaNotificationUpdate update){
        return slaNotificationService.updateNotification(update);
    }

    @ApiOperation(value = "delete notification")
    @DeleteMapping(value = "/notification/{id}")
    public Object deleteNotification(@PathVariable("id") Long id){
        return slaNotificationService.removeById(id);
    }

    @ApiOperation(value = "page list notification")
    @GetMapping("/notification/page")
    public Object pageListNotification(@RequestParam("workspaceId") Long workspaceId,
                                   @RequestParam(value = "searchVal", required = false) String searchVal,
                                   @RequestParam("pageNumber") Integer pageNumber,
                                   @RequestParam("pageSize") Integer pageSize){
        return slaNotificationService.pageListNotification(workspaceId, searchVal, pageNumber, pageSize);
    }
}