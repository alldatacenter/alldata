package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.permission.CreatePermissionCmd;
import com.hw.lineage.server.application.command.permission.UpdatePermissionCmd;
import com.hw.lineage.server.application.dto.PermissionDTO;
import com.hw.lineage.server.domain.query.permission.PermissionCheck;
import com.hw.lineage.server.domain.query.permission.PermissionQuery;

/**
 * @description: PermissionService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface PermissionService {

    Long createPermission(CreatePermissionCmd command);

    PermissionDTO queryPermission(Long permissionId);

    Boolean checkPermissionExist(PermissionCheck permissionCheck);

    PageInfo<PermissionDTO> queryPermissions(PermissionQuery permissionQuery);

    void deletePermission(Long permissionId);

    void updatePermission(UpdatePermissionCmd command);
}
