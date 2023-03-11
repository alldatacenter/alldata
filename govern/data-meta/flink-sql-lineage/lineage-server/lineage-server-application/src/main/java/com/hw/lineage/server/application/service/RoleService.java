package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.role.CreateRoleCmd;
import com.hw.lineage.server.application.command.role.UpdateRoleCmd;
import com.hw.lineage.server.application.dto.RoleDTO;
import com.hw.lineage.server.domain.query.role.RoleCheck;
import com.hw.lineage.server.domain.query.role.RoleQuery;

/**
 * @description: RoleService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface RoleService {

    Long createRole(CreateRoleCmd command);

    RoleDTO queryRole(Long roleId);

    Boolean checkRoleExist(RoleCheck roleCheck);

    PageInfo<RoleDTO> queryRoles(RoleQuery roleQuery);

    void deleteRole(Long roleId);

    void updateRole(UpdateRoleCmd command);
}
