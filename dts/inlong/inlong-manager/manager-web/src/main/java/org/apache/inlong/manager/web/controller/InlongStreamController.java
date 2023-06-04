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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.tool.excel.ExcelTool;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.sink.ParseFieldRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.stream.InlongStreamProcessService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Inlong stream control layer
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "Inlong-Stream-API")
public class InlongStreamController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongStreamProcessService streamProcessOperation;

    @RequestMapping(value = "/stream/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save inlong stream")
    public Response<Integer> save(@RequestBody InlongStreamRequest request) {
        int result = streamService.save(request, LoginUserUtils.getLoginUser().getName());
        return Response.success(result);
    }

    @RequestMapping(value = "/stream/exist/{groupId}/{streamId}", method = RequestMethod.GET)
    @ApiOperation(value = "Is the inlong stream exists")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> exist(@PathVariable String groupId, @PathVariable String streamId) {
        return Response.success(streamService.exist(groupId, streamId));
    }

    @RequestMapping(value = "/stream/get", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<InlongStreamInfo> get(@RequestParam String groupId, @RequestParam String streamId) {
        return Response.success(streamService.get(groupId, streamId));
    }

    @RequestMapping(value = "/stream/list", method = RequestMethod.POST)
    @ApiOperation(value = "List inlong stream briefs by paginating")
    public Response<PageResult<InlongStreamBriefInfo>> listByCondition(@RequestBody InlongStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(LoginUserUtils.getLoginUser().getRoles().contains(UserRoleCode.ADMIN));
        return Response.success(streamService.listBrief(request));
    }

    @RequestMapping(value = "/stream/listAll", method = RequestMethod.POST)
    @ApiOperation(value = "List inlong streams with sources and sinks by paginating")
    public Response<PageResult<InlongStreamInfo>> listAllWithGroupId(@RequestBody InlongStreamPageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(LoginUserUtils.getLoginUser().getRoles().contains(UserRoleCode.ADMIN));
        return Response.success(streamService.listAll(request));
    }

    @RequestMapping(value = "/stream/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update inlong stream")
    public Response<Boolean> update(@Validated(UpdateValidation.class) @RequestBody InlongStreamRequest request) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamService.update(request, username));
    }

    @RequestMapping(value = "/stream/startProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Start inlong stream process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> startProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamProcessOperation.startProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/stream/suspendProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Suspend inlong stream process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> suspendProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamProcessOperation.suspendProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/stream/restartProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Restart inlong stream process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> restartProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamProcessOperation.restartProcess(groupId, streamId, operator, sync));
    }

    @RequestMapping(value = "/stream/deleteProcess/{groupId}/{streamId}", method = RequestMethod.POST)
    @ApiOperation(value = "Delete inlong stream process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> deleteProcess(@PathVariable String groupId, @PathVariable String streamId,
            @RequestParam boolean sync) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamProcessOperation.deleteProcess(groupId, streamId, operator, sync));
    }

    @Deprecated
    @RequestMapping(value = "/stream/delete", method = RequestMethod.DELETE)
    @OperationLog(operation = OperationType.DELETE)
    @ApiOperation(value = "Delete inlong stream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "streamId", dataTypeClass = String.class, required = true)
    })
    public Response<Boolean> delete(@RequestParam String groupId, @RequestParam String streamId) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(streamService.delete(groupId, streamId, username));
    }

    @RequestMapping(value = "/stream/parseFields", method = RequestMethod.POST)
    @ApiOperation(value = "Parse inlong stream fields from statement")
    public Response<List<StreamField>> parseFields(@RequestBody ParseFieldRequest parseFieldRequest) {
        return Response.success(streamService.parseFields(parseFieldRequest));
    }

    @RequestMapping(value = "/stream/parseFieldsByExcel", method = RequestMethod.POST)
    @ApiOperation(value = "Parse inlong stream fields by update excel file", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "file object", required = true, dataType = "__FILE", dataTypeClass = MultipartFile.class, paramType = "query")
    })
    public Response<List<StreamField>> parseFieldsByExcel(@RequestParam(value = "file") MultipartFile file) {
        return Response.success(streamService.parseFields(file));
    }

    @RequestMapping(value = "/stream/fieldsImportTemplate", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation(value = "Download fields import template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadFieldsImportTemplate(HttpServletResponse response) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = String.format("InLong-stream-fields-template-%s.xlsx", date);
        response.setHeader("Content-Disposition",
                "attachment;filename=" + fileName);
        response.setHeader(HttpHeaders.CONTENT_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try {
            ServletOutputStream outputStream = response.getOutputStream();
            ExcelTool.write(StreamField.class, outputStream);
        } catch (IOException e) {
            log.error("Can not properly download Excel file", e);
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    String.format("can not properly download template file: %s", e.getMessage()));
        }
    }

}
