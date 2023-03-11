package com.hw.lineage.server.domain.repository;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.domain.entity.Permission;
import com.hw.lineage.server.domain.query.permission.PermissionQuery;
import com.hw.lineage.server.domain.repository.basic.Repository;
import com.hw.lineage.server.domain.vo.PermissionId;

/**
 * @description: PermissionRepository
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface PermissionRepository extends Repository<Permission, PermissionId> {

    boolean check(String permissionName,String permissionCode);
    PageInfo<Permission> findAll(PermissionQuery permissionQuery);
}