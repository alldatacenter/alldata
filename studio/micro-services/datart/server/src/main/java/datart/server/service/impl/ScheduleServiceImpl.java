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
package datart.server.service.impl;

import datart.core.base.consts.Const;
import datart.core.base.consts.JobType;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.ParamException;
import datart.core.common.UUIDGenerator;
import datart.core.entity.*;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.ScheduleLogMapperExt;
import datart.core.mappers.ext.ScheduleMapperExt;
import datart.security.base.PermissionInfo;
import datart.security.base.ResourceType;
import datart.security.base.SubjectType;
import datart.security.exception.PermissionDeniedException;
import datart.security.manager.shiro.ShiroSecurityManager;
import datart.security.util.PermissionHelper;
import datart.server.base.dto.ScheduleBaseInfo;
import datart.server.base.params.*;
import datart.server.job.EmailJob;
import datart.server.job.ScheduleJob;
import datart.server.job.WeChartJob;
import datart.server.service.BaseService;
import datart.server.service.RoleService;
import datart.server.service.ScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScheduleServiceImpl extends BaseService implements ScheduleService {

    private final ScheduleMapperExt scheduleMapper;

    private final RoleService roleService;

    private final ScheduleLogMapperExt scheduleLogMapper;

    private final Scheduler scheduler;

    private final RelRoleResourceMapperExt rrrMapper;

    public ScheduleServiceImpl(ScheduleMapperExt scheduleMapper,
                               RoleService roleService,
                               ScheduleLogMapperExt scheduleLogMapper,
                               Scheduler scheduler, RelRoleResourceMapperExt rrrMapper) {
        this.scheduleMapper = scheduleMapper;
        this.roleService = roleService;
        this.scheduleLogMapper = scheduleLogMapper;
        this.scheduler = scheduler;
        this.rrrMapper = rrrMapper;
    }

    @Override
    public void requirePermission(Schedule schedule, int permission) {
        if (securityManager.isOrgOwner(schedule.getOrgId())) {
            return;
        }
        List<Role> roles = roleService.listUserRoles(schedule.getOrgId(), getCurrentUser().getId());
        boolean hasPermission = roles.stream().anyMatch(role -> hasPermission(role, schedule, permission));
        if (!hasPermission) {
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied",
                    ResourceType.SCHEDULE + ":" + schedule.getName() + ":" + ShiroSecurityManager.expand2StringPermissions(permission));
        }
    }

    private boolean hasPermission(Role role, Schedule schedule, int permission) {
        if (schedule.getId() == null || rrrMapper.countRolePermission(schedule.getId(), role.getId()) == 0) {
            Schedule parent = scheduleMapper.selectByPrimaryKey(schedule.getParentId());
            if (parent == null) {
                return securityManager.hasPermission(PermissionHelper.schedulePermission(schedule.getOrgId(), role.getId(), ResourceType.SCHEDULE.name(), permission));
            } else {
                return hasPermission(role, parent, permission);
            }
        } else {
            return securityManager.hasPermission(PermissionHelper.schedulePermission(schedule.getOrgId(), role.getId(), schedule.getId(), permission));
        }
    }

    @Override
    public List<ScheduleBaseInfo> listSchedules(String orgId) {

        List<Schedule> schedules = scheduleMapper.selectByOrg(orgId);

        Map<String, Schedule> filtered = new HashMap<>();

        List<Schedule> permitted = schedules.stream().filter(schedule -> {
            try {
                requirePermission(schedule, Const.READ);
                return true;
            } catch (Exception e) {
                filtered.put(schedule.getId(), schedule);
                return false;
            }
        }).collect(Collectors.toList());

        while (!filtered.isEmpty()) {
            boolean updated = false;
            for (Schedule schedule : permitted) {
                Schedule parent = filtered.remove(schedule.getParentId());
                if (parent != null) {
                    permitted.add(parent);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                break;
            }
        }
        return permitted.stream()
                .map(ScheduleBaseInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleBaseInfo> listArchivedSchedules(String orgId) {
        return scheduleMapper.selectArchived(orgId)
                .stream()
                .filter(schedule -> {
                    try {
                        requirePermission(schedule, Const.MANAGE);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(ScheduleBaseInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Schedule create(BaseCreateParam createParam) {
        ScheduleCreateParam scheduleCreateParam = (ScheduleCreateParam) createParam;
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(createParam, schedule);

        requirePermission(schedule, Const.CREATE);

        schedule.setCreateBy(getCurrentUser().getId());
        if (Objects.equals(schedule.getIsFolder(), Boolean.TRUE)) {
            schedule.setType(ResourceType.FOLDER.name());
        } else {
            schedule.setIsFolder(Boolean.FALSE);
            schedule.setType(scheduleCreateParam.getType().name());
        }
        schedule.setCreateTime(new Date());
        schedule.setId(UUIDGenerator.generate());
        schedule.setStatus((byte) 1);
        schedule.setActive(false);
        scheduleMapper.insert(schedule);

        grantDefaultPermission(schedule);

        return schedule;
    }

    @Override
    @Transactional
    public boolean update(BaseUpdateParam updateParam) {
        ScheduleUpdateParam scheduleUpdateParam = (ScheduleUpdateParam) updateParam;
        Schedule schedule = retrieve(updateParam.getId());
        requirePermission(schedule, Const.MANAGE);
        BeanUtils.copyProperties(updateParam, schedule);
        schedule.setUpdateBy(getCurrentUser().getId());
        schedule.setUpdateTime(new Date());
        if (Objects.equals(schedule.getIsFolder(), Boolean.TRUE)) {
            schedule.setType(ResourceType.FOLDER.name());
        } else {
            schedule.setType(scheduleUpdateParam.getType().name());
        }
        return scheduleMapper.updateByPrimaryKey(schedule) == 1;
    }

    @Override
    public boolean execute(String scheduleId) {
        log.info("schedule started.");
        Schedule schedule = retrieve(scheduleId);
        log.info("Executing job [" + schedule.getName() + ",job type :" + schedule.getType() + "]");
        ScheduleJob job;
        if (JobType.WECHART.name().equalsIgnoreCase(schedule.getType())) {
            job = new WeChartJob();
        } else {
            job = new EmailJob();
        }
        boolean success = job.execute(scheduleId);
        log.info("job execute finished with " + (success ? "success" : "failure"));
        return success;
    }

    @Override
    public boolean start(String scheduleId) throws SchedulerException {
        Schedule schedule = retrieve(scheduleId);
        if (schedule.getActive() && scheduler.checkExists(JobKey.jobKey(schedule.getName(), schedule.getOrgId()))) {
            Exceptions.tr(BaseException.class, "message.task.running");
        }
        Date now = new Date();
        if (schedule.getStartDate() != null && now.before(new Date())) {
            Exceptions.tr(BaseException.class, "message.task.not.executable");
        }
        if (schedule.getEndDate() != null && now.after(schedule.getEndDate())) {
            Exceptions.tr(BaseException.class, "message.task.not.executable");
        }

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(scheduleId)
                .withSchedule(CronScheduleBuilder.cronSchedule(schedule.getCronExpression()))
                .startAt(schedule.getStartDate())
                .endAt(schedule.getEndDate())
                .build();

        scheduler.scheduleJob(createJobDetail(schedule), trigger);
        schedule.setActive(true);
        scheduleMapper.updateByPrimaryKey(schedule);
        return true;
    }

    @Override
    public boolean safeDelete(String id) {
        return scheduleMapper.checkReference(id) == 0;
    }

    @Override
    public boolean stop(String scheduleId) throws SchedulerException {
        Schedule schedule = retrieve(scheduleId);
        JobKey jobKey = JobKey.jobKey(schedule.getName(), schedule.getOrgId());
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        schedule.setActive(false);
        scheduleMapper.updateByPrimaryKey(schedule);
        return true;
    }

    @Override
    public List<ScheduleLog> getScheduleLogs(String scheduleId, int count) {
        return scheduleLogMapper.selectByScheduleId(scheduleId, count);
    }

    @Override
    public boolean updateBase(ScheduleBaseUpdateParam updateParam) {
        Schedule schedule = retrieve(updateParam.getId());
        requirePermission(schedule, Const.MANAGE);
        if (!schedule.getName().equals(updateParam.getName())) {
            //check name
            Schedule check = new Schedule();
            check.setParentId(updateParam.getParentId());
            check.setOrgId(schedule.getOrgId());
            check.setName(updateParam.getName());
            checkUnique(check);
        }

        // update base info
        schedule.setId(updateParam.getId());
        schedule.setUpdateBy(getCurrentUser().getId());
        schedule.setUpdateTime(new Date());
        schedule.setName(updateParam.getName());
        schedule.setParentId(updateParam.getParentId());
        schedule.setIndex(updateParam.getIndex());
        return 1 == scheduleMapper.updateByPrimaryKey(schedule);
    }

    @Override
    public boolean unarchive(String id, String newName, String parentId, double index) {
        Schedule schedule = retrieve(id);
        requirePermission(schedule, Const.MANAGE);

        //check name
        if (!schedule.getName().equals(newName)) {
            checkUnique(schedule.getOrgId(), parentId, newName);
        }

        // update status
        schedule.setName(newName);
        schedule.setParentId(parentId);
        schedule.setStatus(Const.DATA_STATUS_ACTIVE);
        schedule.setIndex(index);
        return 1 == scheduleMapper.updateByPrimaryKey(schedule);
    }

    @Override
    public boolean checkUnique(String orgId, String parentId, String name) {
        if (!CollectionUtils.isEmpty(scheduleMapper.checkName(orgId, parentId, name))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        return true;
    }

    @Override
    @Transactional
    public void grantDefaultPermission(Schedule schedule) {
        if (securityManager.isOrgOwner(schedule.getOrgId())) {
            return;
        }
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setOrgId(schedule.getOrgId());
        permissionInfo.setSubjectType(SubjectType.USER);
        permissionInfo.setSubjectId(getCurrentUser().getId());
        permissionInfo.setResourceType(ResourceType.SCHEDULE);
        permissionInfo.setResourceId(schedule.getId());
        permissionInfo.setPermission(Const.MANAGE);
        roleService.grantPermission(Collections.singletonList(permissionInfo));
    }

    private JobDetail createJobDetail(Schedule schedule) {
        Class<? extends Job> clz;
        if (JobType.WECHART.name().equalsIgnoreCase(schedule.getType())) {
            clz = WeChartJob.class;
        } else {
            clz = EmailJob.class;
        }
        JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(JobKey.jobKey(schedule.getName(), schedule.getOrgId()))
                .ofType(clz)
                .build();

        jobDetail.getJobDataMap().put(ScheduleJob.SCHEDULE_KEY, schedule.getId());

        return jobDetail;
    }

}
