/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.server.controller;


import datart.core.entity.Schedule;
import datart.core.entity.ScheduleLog;
import datart.server.base.dto.ResponseData;
import datart.server.base.dto.ScheduleBaseInfo;
import datart.server.base.params.*;
import datart.server.service.ScheduleService;
import io.swagger.annotations.ApiOperation;
import org.quartz.SchedulerException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/schedules")
public class ScheduleController extends BaseController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @ApiOperation(value = "check schedule name")
    @PostMapping(value = "/check/name")
    public ResponseData<Boolean> checkName(@Validated @RequestBody CheckNameParam param) {
        Schedule schedule = new Schedule();
        schedule.setOrgId(param.getOrgId());
        schedule.setName(param.getName());
        return ResponseData.success(scheduleService.checkUnique(schedule));
    }

    @ApiOperation(value = "list schedules")
    @GetMapping
    public ResponseData<List<ScheduleBaseInfo>> listSchedules(String orgId) {
        return ResponseData.success(scheduleService.listSchedules(orgId));
    }

    @ApiOperation(value = "list archived schedules")
    @GetMapping("/archived")
    public ResponseData<List<ScheduleBaseInfo>> listArchivedSchedules(String orgId) {
        return ResponseData.success(scheduleService.listArchivedSchedules(orgId));
    }

    @ApiOperation(value = "create a schedule")
    @PostMapping
    public ResponseData<Schedule> createSchedule(@Validated @RequestBody ScheduleCreateParam createParam) {
        return ResponseData.success(scheduleService.create(createParam));
    }

    @ApiOperation(value = "get a schedule")
    @GetMapping(value = "/{scheduleId}")
    public ResponseData<Schedule> getSchedule(@PathVariable String scheduleId) {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.retrieve(scheduleId));
    }


    @ApiOperation(value = "update a schedule")
    @PutMapping(value = "/{scheduleId}")
    public ResponseData<Boolean> updateSchedule(@PathVariable String scheduleId, @Validated @RequestBody ScheduleUpdateParam updateParam) {
        updateParam.setId(scheduleId);
        return ResponseData.success(scheduleService.update(updateParam));
    }

    @ApiOperation(value = "update a schedule base info")
    @PutMapping(value = "/{scheduleId}/base")
    public ResponseData<Boolean> updateScheduleBaseInfo(@PathVariable String scheduleId,
                                                        @Validated @RequestBody ScheduleBaseUpdateParam updateParam) {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.updateBase(updateParam));
    }

    @ApiOperation(value = "delete a schedule")
    @DeleteMapping(value = "/{scheduleId}")
    public ResponseData<Boolean> deleteSchedule(@PathVariable String scheduleId, @RequestParam boolean archive) {
        return ResponseData.success(scheduleService.delete(scheduleId, archive, true));
    }

    @ApiOperation(value = "execute a schedule")
    @PostMapping(value = "/execute/{scheduleId}")
    public ResponseData<Boolean> execute(@PathVariable String scheduleId) {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.execute(scheduleId));
    }

    @ApiOperation(value = "start a schedule")
    @PutMapping(value = "/start/{scheduleId}")
    public ResponseData<Boolean> start(@PathVariable String scheduleId) throws SchedulerException {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.start(scheduleId));
    }

    @ApiOperation(value = "stop a schedule")
    @PutMapping(value = "/stop/{scheduleId}")
    public ResponseData<Boolean> stop(@PathVariable String scheduleId) throws SchedulerException {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.stop(scheduleId));
    }

    @ApiOperation(value = "get schedule logs")
    @GetMapping(value = "/logs/{scheduleId}")
    public ResponseData<List<ScheduleLog>> getScheduleLogs(@PathVariable String scheduleId,
                                                           @RequestParam int count) {
        checkBlank(scheduleId, "scheduleId");
        return ResponseData.success(scheduleService.getScheduleLogs(scheduleId, count));
    }

    @ApiOperation(value = "unarchive schedule")
    @PutMapping(value = "/unarchive/{scheduleId}")
    public ResponseData<Boolean> unarchiveSchedule(@PathVariable String scheduleId,
                                                   @RequestParam String name,
                                                   @RequestParam Double index,
                                                   @RequestParam(required = false) String parentId) {
        return ResponseData.success(scheduleService.unarchive(scheduleId, name, parentId, index));
    }

}
