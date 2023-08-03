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

package org.apache.seatunnel.app.controller;

import org.apache.seatunnel.app.common.Result;
import org.apache.seatunnel.app.dal.dao.IUserDao;
import org.apache.seatunnel.app.dal.entity.User;
import org.apache.seatunnel.app.domain.request.datasource.VirtualTableReq;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableRes;
import org.apache.seatunnel.app.service.IVirtualTableService;

import org.apache.commons.collections4.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.seatunnel.app.common.Constants.SESSION_USER;

@RestController
@RequestMapping("/seatunnel/api/v1/virtual_table")
public class VirtualTableController extends BaseController {

    @Autowired private IVirtualTableService virtualTableService;

    @Resource(name = "userDaoImpl")
    private IUserDao userMapper;

    @ApiOperation(value = "create virtual table", httpMethod = "POST")
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = "tableReq",
                value = "virtual table request",
                required = true,
                dataType = "VirtualTableReq")
    })
    @PostMapping("/create")
    Result<String> createVirtualTable(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @RequestBody VirtualTableReq tableReq) {
        return Result.success(virtualTableService.createVirtualTable(loginUser.getId(), tableReq));
    }

    @ApiOperation(value = "update virtual table", httpMethod = "PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = "id",
                value = "virtual table id",
                required = true,
                dataType = "String"),
        @ApiImplicitParam(
                name = "tableReq",
                value = "virtual table request",
                required = true,
                dataType = "VirtualTableReq")
    })
    @PutMapping("/{id}")
    Result<Boolean> updateVirtualTable(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @PathVariable("id") String id,
            @RequestBody VirtualTableReq tableReq) {
        return Result.success(
                virtualTableService.updateVirtualTable(loginUser.getId(), id, tableReq));
    }

    @ApiOperation(value = "check virtual table valid", httpMethod = "GET")
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = "virtualTableReq",
                value = "virtual table request",
                required = true,
                dataType = "VirtualTableReq")
    })
    @GetMapping("/check/valid")
    Result<Boolean> checkVirtualTableValid(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @RequestBody VirtualTableReq virtualTableReq) {
        return Result.success(virtualTableService.checkVirtualTableValid(virtualTableReq));
    }

    @ApiOperation(value = "get support field type", httpMethod = "GET")
    @GetMapping("/support/field_type")
    Result<List<String>> getSupportFieldType(@RequestParam("pluginName") String pluginName) {
        // return Result.success(virtualTableService.queryTableDynamicTable(pluginName));
        // todo @liuli
        return null;
    }

    @ApiOperation(value = "delete virtual table", httpMethod = "DELETE")
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = "id",
                value = "virtual table id",
                required = true,
                dataType = "String")
    })
    @DeleteMapping("/{id}")
    Result<Boolean> deleteVirtualTable(
            @PathVariable("id") String id,
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser) {
        return Result.success(virtualTableService.deleteVirtualTable(loginUser.getId(), id));
    }

    @ApiOperation(value = "query virtual table detail by id", httpMethod = "GET")
    @ApiImplicitParams({
        @ApiImplicitParam(
                name = "id",
                value = "virtual table id",
                required = true,
                dataType = "String")
    })
    @GetMapping("/{id}")
    Result<VirtualTableDetailRes> queryVirtualTable(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @PathVariable("id") String id) {
        // rsp add plugin name
        return Result.success(virtualTableService.queryVirtualTable(id));
    }

    @GetMapping("/list")
    Result<PageInfo<VirtualTableRes>> getVirtualTableList(
            @RequestParam("pluginName") String pluginName,
            @RequestParam("datasourceName") String datasourceName,
            @RequestParam("pageNo") Integer pageNo,
            @RequestParam("pageSize") Integer pageSize) {
        PageInfo<VirtualTableRes> virtualTableList =
                virtualTableService.getVirtualTableList(
                        pluginName, datasourceName, pageNo, pageSize);
        if (virtualTableList.getTotalCount() == 0
                || CollectionUtils.isEmpty(virtualTableList.getData())) {
            return Result.success(virtualTableList);
        }
        Map<Integer, String> userIdNameMap = userIdNameMap();
        virtualTableList
                .getData()
                .forEach(
                        virtualTableRes -> {
                            virtualTableRes.setCreateUserName(
                                    userIdNameMap.getOrDefault(
                                            virtualTableRes.getCreateUserId(), ""));
                            virtualTableRes.setUpdateUserName(
                                    userIdNameMap.getOrDefault(
                                            virtualTableRes.getUpdateUserId(), ""));
                        });
        return Result.success(virtualTableList);
    }

    @GetMapping("/dynamic_config")
    Result<String> getDynamicConfig(
            @RequestParam("pluginName") String pluginName,
            @RequestParam("datasourceName") String datasourceName) {
        return Result.success(virtualTableService.queryTableDynamicTable(pluginName));
    }

    public Map<Integer, String> userIdNameMap() {
        return userMapper.queryEnabledUsers().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
