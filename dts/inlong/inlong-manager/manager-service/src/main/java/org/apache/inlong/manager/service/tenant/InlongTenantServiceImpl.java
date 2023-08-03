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

package org.apache.inlong.manager.service.tenant;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongTenantEntity;
import org.apache.inlong.manager.dao.mapper.InlongTenantEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantPageRequest;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.service.user.TenantRoleService;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.apache.inlong.manager.pojo.user.UserRoleCode.INLONG_ADMIN;
import static org.apache.inlong.manager.pojo.user.UserRoleCode.INLONG_OPERATOR;
import static org.apache.inlong.manager.service.workflow.WorkflowDefinition.UT_ADMIN_NAME;

@Service
@Slf4j
public class InlongTenantServiceImpl implements InlongTenantService {

    @Autowired
    private InlongTenantEntityMapper inlongTenantEntityMapper;
    @Autowired
    private TenantRoleService tenantRoleService;
    @Autowired
    private WorkflowApproverService workflowApproverService;
    private ExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Override
    public InlongTenantInfo getByName(String name) {
        InlongTenantEntity entity = inlongTenantEntityMapper.selectByName(name);
        if (entity == null) {
            log.warn("not found valid inlong tenant by name={}", name);
            return null;
        }
        return CommonBeanUtils.copyProperties(entity, InlongTenantInfo::new);
    }

    @Override
    public Integer save(InlongTenantRequest request) {
        String name = request.getName();
        InlongTenantEntity existEntity = inlongTenantEntityMapper.selectByName(name);
        if (existEntity != null) {
            String errMsg = String.format("tenant already exist for name=%s)", name);
            log.error(errMsg);
            throw new BusinessException(errMsg);
        }
        InlongTenantEntity entity = CommonBeanUtils.copyProperties(request, InlongTenantEntity::new);
        String operator = LoginUserUtils.getLoginUser().getName();
        entity.setCreator(operator);
        entity.setModifier(operator);
        inlongTenantEntityMapper.insert(entity);

        UserInfo loginUserInfo = LoginUserUtils.getLoginUser();
        executorService.submit(() -> {
            loginUserInfo.setTenant(request.getName());
            LoginUserUtils.setUserLoginInfo(loginUserInfo);
            saveDefaultWorkflowApprovers(ProcessName.APPLY_GROUP_PROCESS.name(),
                    UT_ADMIN_NAME, operator);
            saveDefaultWorkflowApprovers(ProcessName.APPLY_CONSUME_PROCESS.name(),
                    UT_ADMIN_NAME, operator);
            LoginUserUtils.removeUserLoginInfo();
        });

        return entity.getId();
    }

    private Integer saveDefaultWorkflowApprovers(String processName, String taskName, String approver) {
        ApproverRequest request = new ApproverRequest();
        request.setProcessName(processName);
        request.setApprovers(approver);
        request.setTaskName(taskName);
        return workflowApproverService.save(request, approver);
    }

    @Override
    public PageResult<InlongTenantInfo> listByCondition(InlongTenantPageRequest request, UserInfo userInfo) {
        if (request.getListByLoginUser()) {
            setTargetTenantList(request, userInfo);
        }

        PageHelper.startPage(request.getPageNum(), request.getPageSize());

        Page<InlongTenantEntity> entityPage = inlongTenantEntityMapper.selectByCondition(request);

        List<InlongTenantInfo> tenantList = CommonBeanUtils.copyListProperties(entityPage, InlongTenantInfo::new);
        PageResult<InlongTenantInfo> pageResult = new PageResult<>(tenantList,
                entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());

        return pageResult;
    }

    @Override
    public Boolean update(InlongTenantRequest request) {
        InlongTenantEntity exist = inlongTenantEntityMapper.selectByName(request.getName());
        if (exist == null) {
            throw new BusinessException(ErrorCodeEnum.RECORD_NOT_FOUND,
                    String.format("tenant record not found by name=%s", request.getName()));
        }
        if (!exist.getId().equals(request.getId())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("tenant already exist for name=%s, required id=%s, exist id=%s",
                            request.getName(), request.getId(), exist.getId()));
        }
        InlongTenantEntity entity = CommonBeanUtils.copyProperties(request, InlongTenantEntity::new);
        String operator = LoginUserUtils.getLoginUser().getName();
        entity.setModifier(operator);
        int rowCount = inlongTenantEntityMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format(
                            "failure to update tenant with name=%s, request version=%d, updated row=%d",
                            request.getName(), request.getVersion(), rowCount));
        }
        return true;
    }

    @Override
    public Boolean delete(String name) {
        String operator = LoginUserUtils.getLoginUser().getName();
        log.info("begin to delete inlong tenant name={} by user={}", name, operator);
        InlongTenantEntity inlongTenantEntity = inlongTenantEntityMapper.selectByName(name);
        int success = inlongTenantEntityMapper.deleteById(inlongTenantEntity.getId());
        Preconditions.expectTrue(success == 1, "delete failed");
        log.info("success delete inlong tenant name={} by user={}", name, operator);
        return true;
    }

    private void setTargetTenantList(InlongTenantPageRequest request, UserInfo userInfo) {
        if (isInlongRoles(userInfo)) {
            // for inlong roles, they can get all tenant info.
            request.setTenantList(null);
            return;
        }

        List<String> tenants = tenantRoleService.listTenantByUsername(userInfo.getName());
        if (CollectionUtils.isEmpty(tenants)) {
            String errMsg = String.format("user=[%s] doesn't belong to any tenant, please contact administrator " +
                    "and get one tenant at least", userInfo.getName());
            log.error(errMsg);
            throw new BusinessException(errMsg);
        }
        request.setTenantList(tenants);
    }

    private boolean isInlongRoles(UserInfo userInfo) {
        return userInfo.getRoles().contains(INLONG_ADMIN) || userInfo.getRoles().contains(INLONG_OPERATOR);
    }
}
