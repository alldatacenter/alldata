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
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.core.enums.Status;
import io.datavines.server.api.dto.bo.workspace.InviteUserIntoWorkspace;
import io.datavines.server.api.dto.bo.workspace.RemoveUserOutWorkspace;
import io.datavines.server.api.dto.bo.workspace.WorkSpaceCreate;
import io.datavines.server.api.dto.bo.workspace.WorkSpaceUpdate;
import io.datavines.server.api.dto.vo.UserVO;
import io.datavines.server.api.dto.vo.WorkSpaceVO;
import io.datavines.server.repository.entity.User;
import io.datavines.server.repository.entity.UserWorkSpace;
import io.datavines.server.repository.entity.WorkSpace;
import io.datavines.server.repository.mapper.WorkSpaceMapper;
import io.datavines.server.repository.service.UserService;
import io.datavines.server.repository.service.UserWorkSpaceService;
import io.datavines.server.repository.service.WorkSpaceService;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service("workSpaceService")
public class WorkSpaceServiceImpl extends ServiceImpl<WorkSpaceMapper, WorkSpace> implements WorkSpaceService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserWorkSpaceService userWorkSpaceService;

    @Override
    public long insert(WorkSpaceCreate workSpaceCreate) throws DataVinesServerException {
        if (isWorkSpaceExist(workSpaceCreate.getName())) {
            throw new DataVinesServerException(Status.WORKSPACE_EXIST_ERROR, workSpaceCreate.getName());
        }
        WorkSpace workSpace = new WorkSpace();
        BeanUtils.copyProperties(workSpaceCreate, workSpace);
        workSpace.setCreateBy(ContextHolder.getUserId());
        workSpace.setCreateTime(LocalDateTime.now());
        workSpace.setUpdateBy(ContextHolder.getUserId());
        workSpace.setUpdateTime(LocalDateTime.now());

        if (baseMapper.insert(workSpace) <= 0) {
            log.info("create workspace fail : {}", workSpaceCreate);
            throw new DataVinesServerException(Status.CREATE_WORKSPACE_ERROR, workSpaceCreate.getName());
        }

        UserWorkSpace userWorkSpace = new UserWorkSpace();
        userWorkSpace.setUserId(ContextHolder.getUserId());
        userWorkSpace.setWorkspaceId(workSpace.getId());
        userWorkSpace.setRoleId(1L);
        userWorkSpace.setCreateBy(ContextHolder.getUserId());
        userWorkSpace.setCreateTime(LocalDateTime.now());
        userWorkSpace.setUpdateBy(ContextHolder.getUserId());
        userWorkSpace.setUpdateTime(LocalDateTime.now());
        userWorkSpaceService.save(userWorkSpace);

        return workSpace.getId();
    }

    @Override
    public int update(WorkSpaceUpdate workSpaceUpdate) throws DataVinesServerException {

        WorkSpace workSpace = getById(workSpaceUpdate.getId());
        if ( workSpace == null) {
            throw new DataVinesServerException(Status.WORKSPACE_NOT_EXIST_ERROR, workSpaceUpdate.getName());
        }

        BeanUtils.copyProperties(workSpaceUpdate, workSpace);
        workSpace.setUpdateBy(ContextHolder.getUserId());
        workSpace.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(workSpace) <= 0) {
            log.info("update workspace fail : {}", workSpaceUpdate);
            throw new DataVinesServerException(Status.UPDATE_WORKSPACE_ERROR, workSpaceUpdate.getName());
        }

        return 1;
    }

    @Override
    public WorkSpace getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<WorkSpaceVO> listByUserId() {
        return userWorkSpaceService.listWorkSpaceByUserId(ContextHolder.getUserId());
    }

    @Override
    public IPage<UserVO> listUserByWorkspaceId(Long workspaceId, Integer pageNumber, Integer pageSize) {
        Page<UserVO> page = new Page<>(pageNumber, pageSize);
        IPage<UserVO> users = userWorkSpaceService.getWorkSpaceUserPage(page, workspaceId);
        return users;
    }

    @Override
    public int deleteById(long id) {
        return baseMapper.deleteById(id);
    }

    private boolean isWorkSpaceExist(String name) {
        WorkSpace user = baseMapper.selectOne(new QueryWrapper<WorkSpace>().eq("name", name));
        return user != null;
    }

    @Override
    public long inviteUserIntoWorkspace(InviteUserIntoWorkspace inviteUserIntoWorkspace) {

        User user = userService.getOne(new QueryWrapper<User>()
                .eq("username",inviteUserIntoWorkspace.getUsername())
                .eq("email",inviteUserIntoWorkspace.getEmail()));

        if (user == null) {
            throw new DataVinesServerException(Status.USER_IS_NOT_EXIST_ERROR);
        }

        UserWorkSpace userWorkSpace = userWorkSpaceService.getOne(new QueryWrapper<UserWorkSpace>()
                .eq("user_id",user.getId())
                .eq("workspace_id",inviteUserIntoWorkspace.getWorkspaceId()));

        if (userWorkSpace != null) {
            throw new DataVinesServerException(Status.USER_IS_IN_WORKSPACE_ERROR);
        }

        userWorkSpace = new UserWorkSpace();
        userWorkSpace.setUserId(user.getId());
        userWorkSpace.setWorkspaceId(inviteUserIntoWorkspace.getWorkspaceId());
        userWorkSpace.setCreateBy(ContextHolder.getUserId());
        userWorkSpace.setCreateTime(LocalDateTime.now());
        userWorkSpace.setUpdateBy(ContextHolder.getUserId());
        userWorkSpace.setUpdateTime(LocalDateTime.now());

        userWorkSpaceService.save(userWorkSpace);

        return userWorkSpace.getId();
    }

    @Override
    public boolean removeUser(RemoveUserOutWorkspace removeUserOutWorkspace) {
        List<UserWorkSpace> userWorkSpaceList = userWorkSpaceService.list(new QueryWrapper<UserWorkSpace>().eq("user_id", removeUserOutWorkspace.getUserId()));

        if (CollectionUtils.isNotEmpty(userWorkSpaceList) && userWorkSpaceList.size() == 1) {
            throw new DataVinesServerException(Status.USER_HAS_ONLY_ONE_WORKSPACE);
        }

        UserWorkSpace userWorkSpace = userWorkSpaceService.getOne(new QueryWrapper<UserWorkSpace>()
                .eq("user_id",ContextHolder.getUserId()).eq("workspace_id", removeUserOutWorkspace.getWorkspaceId()));

        if (userWorkSpace != null &&
                ( (userWorkSpace.getRoleId() != null && userWorkSpace.getRoleId() == 1)
                        || removeUserOutWorkspace.getUserId().equals(ContextHolder.getUserId()))) {
            return userWorkSpaceService.remove(new QueryWrapper<UserWorkSpace>()
                    .eq("user_id",removeUserOutWorkspace.getUserId()).eq("workspace_id", removeUserOutWorkspace.getWorkspaceId()));
        }

        throw new DataVinesServerException(Status.USER_HAS_NO_AUTHORIZE_TO_REMOVE);
    }
}
