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

package org.apache.inlong.manager.web.controller;

import org.apache.inlong.manager.pojo.audit.AuditRequest;
import org.apache.inlong.manager.pojo.audit.AuditSourceRequest;
import org.apache.inlong.manager.pojo.audit.AuditSourceResponse;
import org.apache.inlong.manager.pojo.audit.AuditVO;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.service.core.AuditService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

/**
 * Audit controller.
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Audit-API")
public class AuditController {

    @Lazy
    @Autowired
    private AuditService auditService;

    @PostMapping(value = "/audit/list")
    @ApiOperation(value = "Query audit list according to conditions")
    public Response<List<AuditVO>> listByCondition(@Valid @RequestBody AuditRequest request) throws Exception {
        return Response.success(auditService.listByCondition(request));
    }

    @ApiOperation(value = "Refresh audit base item cache")
    @PostMapping("/audit/refreshCache")
    public Response<Boolean> refreshCache() {
        return Response.success(auditService.refreshBaseItemCache());
    }

    @ApiOperation(value = "Update the audit source")
    @PostMapping(value = "/audit/updateSource")
    public Response<Integer> updateAuditSource(@RequestBody AuditSourceRequest request) {
        return Response.success(auditService.updateAuditSource(request, LoginUserUtils.getLoginUser().getName()));
    }

    @ApiOperation(value = "Get the audit source")
    @GetMapping("/audit/getSource")
    public Response<AuditSourceResponse> getAuditSource() {
        // TODO support more parameters to query
        return Response.success(auditService.getAuditSource());
    }

}
