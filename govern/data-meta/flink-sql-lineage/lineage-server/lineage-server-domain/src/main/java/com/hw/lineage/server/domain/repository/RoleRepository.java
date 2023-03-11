package com.hw.lineage.server.domain.repository;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.domain.entity.Permission;
import com.hw.lineage.server.domain.entity.Role;
import com.hw.lineage.server.domain.entity.User;
import com.hw.lineage.server.domain.query.role.RoleQuery;
import com.hw.lineage.server.domain.repository.basic.Repository;
import com.hw.lineage.server.domain.vo.RoleId;

import java.util.List;

/**
 * @description: RoleRepository
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface RoleRepository extends Repository<Role, RoleId> {

    List<User> findUsers(RoleId roleId);
    List<Permission> findPermissions(RoleId roleId);

    PageInfo<Role> findAll(RoleQuery roleQuery);
}