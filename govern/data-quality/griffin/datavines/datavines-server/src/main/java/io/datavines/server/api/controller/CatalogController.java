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

import io.datavines.common.utils.DateUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.core.aop.RefreshToken;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.server.api.dto.bo.catalog.CatalogRefresh;
import io.datavines.server.api.dto.bo.catalog.OptionItem;
import io.datavines.server.api.dto.bo.catalog.profile.RunProfileRequest;
import io.datavines.server.api.dto.bo.issue.IssueUpdate;
import io.datavines.server.api.dto.bo.job.JobCreateWithEntityUuid;
import io.datavines.server.api.dto.vo.DataTime2ValueItem;
import io.datavines.server.api.dto.vo.MetricExecutionDashBoard;
import io.datavines.server.api.dto.vo.catalog.CatalogColumnDetailVO;
import io.datavines.server.api.dto.vo.catalog.CatalogDatabaseDetailVO;
import io.datavines.server.api.dto.vo.catalog.CatalogEntityMetricParameter;
import io.datavines.server.api.dto.vo.catalog.CatalogTableDetailVO;
import io.datavines.server.repository.entity.catalog.CatalogSchemaChange;
import io.datavines.server.repository.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@Api(value = "catalog", tags = "catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class CatalogController {

    @Autowired
    private CatalogMetaDataFetchTaskService catalogMetaDataFetchTaskService;

    @Autowired
    private CatalogEntityInstanceService catalogEntityInstanceService;

    @Autowired
    private CatalogSchemaChangeService catalogSchemaChangeService;

    @Autowired
    private CatalogEntityProfileService catalogEntityProfileService;

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private IssueService issueService;

    @ApiOperation(value = "refresh", response = Long.class)
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object refreshCatalog(@Valid @RequestBody CatalogRefresh catalogRefresh) {
        return catalogMetaDataFetchTaskService.refreshCatalog(catalogRefresh);
    }

    @ApiOperation(value = "execute data profile job", response = Long.class)
    @PostMapping(value = "/profile/execute")
    public Object executeDataProfileJob(@RequestParam String uuid) {
        RunProfileRequest runProfileRequest = new RunProfileRequest();
        runProfileRequest.setUuid(uuid);
        runProfileRequest.setSelectAll(true);
        return catalogEntityInstanceService.executeDataProfileJob(runProfileRequest);
    }

    @ApiOperation(value = "profile job execute history", response = Long.class)
    @GetMapping(value = "/profile/execute/history")
    public Object profileExecuteHistory(@RequestParam String uuid,
                                        @RequestParam("pageNumber") Integer pageNumber,
                                        @RequestParam("pageSize") Integer pageSize) {
        return catalogEntityInstanceService.profileJobExecutionPage(uuid, pageNumber, pageSize);
    }

    @ApiOperation(value = "execute data profile job", response = Long.class)
    @PostMapping(value = "/profile/execute-select-columns")
    public Object executeDataProfileJobWithColumns(@Valid @RequestBody RunProfileRequest runProfileRequest) {
        return catalogEntityInstanceService.executeDataProfileJob(runProfileRequest);
    }

    @ApiOperation(value = "get selected column list", response = OptionItem.class, responseContainer = "list")
    @GetMapping(value = "/profile/selected-columns/{uuid}")
    public Object getProfileJobSelectedColumns(@PathVariable String uuid) {
        return catalogEntityInstanceService.getProfileJobSelectedColumns(uuid);
    }

    @ApiOperation(value = "get database list", response = OptionItem.class, responseContainer = "list")
    @GetMapping(value = "/list/database/{upstreamUuid}")
    public Object getDatabaseList(@PathVariable String upstreamUuid) {
        return catalogEntityInstanceService.getEntityList(upstreamUuid);
    }

    @ApiOperation(value = "get table list", response = OptionItem.class, responseContainer = "list")
    @GetMapping(value = "/list/table/{upstreamUuid}")
    public Object getTableList(@PathVariable String upstreamUuid) {
        return catalogEntityInstanceService.getEntityList(upstreamUuid);
    }

    @ApiOperation(value = "get column list", response = OptionItem.class, responseContainer = "list")
    @GetMapping(value = "/list/column/{upstreamUuid}")
    public Object getColumnList(@PathVariable String upstreamUuid) {
        return catalogEntityInstanceService.getEntityList(upstreamUuid);
    }

    @ApiOperation(value = "get table with detail list", response = CatalogTableDetailVO.class, responseContainer = "list")
    @GetMapping(value = "/list/table-with-detail/{upstreamUuid}")
    public Object getTableWithDetailList(@PathVariable String upstreamUuid) {
        return catalogEntityInstanceService.getCatalogTableWithDetailList(upstreamUuid);
    }

    @ApiOperation(value = "get table with detail page", response = CatalogTableDetailVO.class, responseContainer = "page")
    @GetMapping(value = "/page/table-with-detail")
    public Object getTableWithDetailPage(@RequestParam("upstreamUuid") String upstreamUuid,
                                         @RequestParam(value = "name",required = false) String name,
                                         @RequestParam("pageNumber") Integer pageNumber,
                                         @RequestParam("pageSize") Integer pageSize) {
        return catalogEntityInstanceService.getCatalogTableWithDetailPage(upstreamUuid, name, pageNumber, pageSize);
    }

    @ApiOperation(value = "get column with detail list", response = CatalogColumnDetailVO.class, responseContainer = "list")
    @GetMapping(value = "/list/column-with-detail/{upstreamUuid}")
    public Object getColumnWithDetailList(@PathVariable String upstreamUuid) {
        return catalogEntityInstanceService.getCatalogColumnWithDetailList(upstreamUuid);
    }

    @ApiOperation(value = "get column with detail page", response = CatalogColumnDetailVO.class, responseContainer = "page")
    @GetMapping(value = "/page/column-with-detail")
    public Object getColumnWithDetailPage(@RequestParam("upstreamUuid") String upstreamUuid,
                                          @RequestParam(value = "name",required = false) String name,
                                          @RequestParam("pageNumber") Integer pageNumber,
                                          @RequestParam("pageSize") Integer pageSize) {
        return catalogEntityInstanceService.getCatalogColumnWithDetailPage(upstreamUuid, name, pageNumber, pageSize);
    }

    @ApiOperation(value = "get database entity detail", response = CatalogDatabaseDetailVO.class)
    @GetMapping(value = "/detail/database/{uuid}")
    public Object getDatabaseEntityDetail(@PathVariable String uuid) {
        return catalogEntityInstanceService.getDatabaseEntityDetail(uuid);
    }

    @ApiOperation(value = "get table entity detail", response = CatalogTableDetailVO.class)
    @GetMapping(value = "/detail/table/{uuid}")
    public Object getTableEntityDetail(@PathVariable String uuid) {
        return catalogEntityInstanceService.getTableEntityDetail(uuid);
    }

    @ApiOperation(value = "get column entity detail", response = CatalogColumnDetailVO.class)
    @GetMapping(value = "/detail/column/{uuid}")
    public Object getColumnEntityDetail(@PathVariable String uuid) {
        return catalogEntityInstanceService.getColumnEntityDetail(uuid);
    }

    @ApiOperation(value = "get table entity profile", response = CatalogTableDetailVO.class)
    @GetMapping(value = "/profile/table/{uuid}")
    public Object getTableEntityProfile(@PathVariable String uuid) {
        return catalogEntityInstanceService.getTableEntityProfile(uuid);
    }

    @ApiOperation(value = "get column entity profile", response = CatalogTableDetailVO.class)
    @GetMapping(value = "/profile/column/{uuid}")
    public Object getColumnEntityProfile(@PathVariable String uuid) {
        return catalogEntityInstanceService.getColumnEntityProfile(uuid);
    }

    @ApiOperation(value = "get entity metric dashboard", response = DataTime2ValueItem.class, responseContainer = "list")
    @GetMapping(value = "/profile/table/records")
    public Object getTableRecordList(@RequestParam String uuid,
                                     @RequestParam(value = "startTime", required = false) String startTime,
                                     @RequestParam(value = "endTime",required = false) String endTime) {
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            endTime = DateUtils.getCurrentTime();
            startTime = DateUtils.format(DateUtils.getSomeDay(new Date(), -7), DateUtils.YYYY_MM_DD_HH_MM_SS);
        }

        return catalogEntityProfileService.listTableRecords(uuid, startTime ,endTime);
    }

    @ApiOperation(value = "get schema change list", response = CatalogSchemaChange.class, responseContainer = "list")
    @GetMapping(value = "/list/schema-change/{uuid}")
    public Object getSchemaChangeList(@PathVariable String uuid) {
        return catalogSchemaChangeService.getSchemaChangeList(uuid);
    }

    @ApiOperation(value = "get schema change page", response = CatalogSchemaChange.class, responseContainer = "page")
    @GetMapping(value = "/page/schema-change")
    public Object getSchemaChangePage(@RequestParam("uuid") String uuid,
                                      @RequestParam("pageNumber") Integer pageNumber,
                                      @RequestParam("pageSize") Integer pageSize) {
        return catalogSchemaChangeService.getSchemaChangePage(uuid, pageNumber, pageSize);
    }

    @ApiOperation(value = "entity add metric", response = Long.class)
    @PostMapping(value = "/add-metric", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object entityAddMetric(@Valid @RequestBody JobCreateWithEntityUuid jobCreateWithEntityUuid) {
        return catalogEntityInstanceService.entityAddMetric(jobCreateWithEntityUuid);
    }

    @ApiOperation(value = "get entity metric parameter", response = CatalogEntityMetricParameter.class)
    @GetMapping(value = "/entity/metric/parameter/{uuid}")
    public Object getEntityMetricParameter(@PathVariable String uuid) {
        return catalogEntityInstanceService.getEntityMetricParameter(uuid);
    }

    @ApiOperation(value = "get entity metric page", response = CatalogSchemaChange.class, responseContainer = "list")
    @GetMapping(value = "/page/entity/metric")
    public Object getEntityMetricList(@RequestParam String uuid,
                                      @RequestParam("pageNumber") Integer pageNumber,
                                      @RequestParam("pageSize") Integer pageSize) {
        return catalogEntityInstanceService.getEntityMetricList(uuid, pageNumber, pageSize);
    }

    @ApiOperation(value = "get entity issue page", response = CatalogSchemaChange.class, responseContainer = "list")
    @GetMapping(value = "/page/entity/issue")
    public Object getEntityIssueList(@RequestParam String uuid,
                                      @RequestParam("pageNumber") Integer pageNumber,
                                      @RequestParam("pageSize") Integer pageSize) {
        return catalogEntityInstanceService.getEntityIssueList(uuid, pageNumber, pageSize);
    }

    @ApiOperation(value = "update entity issue status", response = Long.class)
    @PostMapping(value = "/issue/update-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object issueUpdateStatus(@Valid @RequestBody IssueUpdate issueUpdate) {
        return issueService.updateStatus(issueUpdate);
    }

    @ApiOperation(value = "get entity metric dashboard", response = MetricExecutionDashBoard.class, responseContainer = "list")
    @GetMapping(value = "/list/entity/metric/dashboard")
    public Object getEntityMetricDashBoard(@RequestParam Long jobId,
                                           @RequestParam(value = "startTime", required = false) String startTime,
                                           @RequestParam(value = "endTime",required = false) String endTime) {
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            endTime = DateUtils.getCurrentTime();
            startTime = DateUtils.format(DateUtils.getSomeDay(new Date(), -7), DateUtils.YYYY_MM_DD_HH_MM_SS);
        }

        return jobExecutionService.getMetricExecutionDashBoard(jobId, startTime ,endTime);
    }
}
