package com.hw.lineage.server.application.service.impl;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.application.assembler.DtoAssembler;
import com.hw.lineage.server.application.command.permission.CreatePermissionCmd;
import com.hw.lineage.server.application.command.permission.UpdatePermissionCmd;
import com.hw.lineage.server.application.dto.PermissionDTO;
import com.hw.lineage.server.application.service.PermissionService;
import com.hw.lineage.server.domain.entity.Permission;
import com.hw.lineage.server.domain.query.permission.PermissionCheck;
import com.hw.lineage.server.domain.query.permission.PermissionQuery;
import com.hw.lineage.server.domain.repository.PermissionRepository;
import com.hw.lineage.server.domain.vo.PermissionId;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: PermissionServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private PermissionRepository repository;

    @Resource
    private DtoAssembler assembler;

    @Override
    public Long createPermission(CreatePermissionCmd command) {
        Permission permission = new Permission()
                .setPermissionGroup(command.getPermissionGroup())
                .setPermissionName(command.getPermissionName())
                .setPermissionCode(command.getPermissionCode());

        permission.setCreateTime(System.currentTimeMillis())
                .setModifyTime(System.currentTimeMillis())
                .setInvalid(false);

        permission = repository.save(permission);
        return permission.getPermissionId().getValue();
    }

    @Override
    public PermissionDTO queryPermission(Long permissionId) {
        Permission permission = repository.find(new PermissionId(permissionId));
        return assembler.fromPermission(permission);
    }

    @Override
    public Boolean checkPermissionExist(PermissionCheck permissionCheck) {
        return repository.check(permissionCheck.getPermissionName(), permissionCheck.getPermissionCode());
    }

    @Override
    public PageInfo<PermissionDTO> queryPermissions(PermissionQuery permissionQuery) {
        PageInfo<Permission> pageInfo = repository.findAll(permissionQuery);
        return PageUtils.convertPage(pageInfo, assembler::fromPermission);
    }

    @Override
    public void deletePermission(Long permissionId) {
        repository.remove(new PermissionId(permissionId));
    }

    @Override
    public void updatePermission(UpdatePermissionCmd command) {
        Permission permission = new Permission()
                .setPermissionId(new PermissionId(command.getPermissionId()))
                .setPermissionGroup(command.getPermissionGroup())
                .setPermissionName(command.getPermissionName())
                .setPermissionCode(command.getPermissionCode());

        permission.setModifyTime(System.currentTimeMillis());
        repository.save(permission);
    }
}
