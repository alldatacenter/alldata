package com.hw.lineage.server.application.service.impl;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.application.assembler.DtoAssembler;
import com.hw.lineage.server.application.command.role.CreateRoleCmd;
import com.hw.lineage.server.application.command.role.UpdateRoleCmd;
import com.hw.lineage.server.application.dto.RoleDTO;
import com.hw.lineage.server.application.service.RoleService;
import com.hw.lineage.server.domain.entity.Role;
import com.hw.lineage.server.domain.query.role.RoleCheck;
import com.hw.lineage.server.domain.query.role.RoleQuery;
import com.hw.lineage.server.domain.repository.RoleRepository;
import com.hw.lineage.server.domain.vo.RoleId;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: RoleServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleRepository repository;

    @Resource
    private DtoAssembler assembler;

    @Override
    public Long createRole(CreateRoleCmd command) {
        Role role = new Role().setRoleName(command.getRoleName());

        role.setCreateTime(System.currentTimeMillis())
                .setModifyTime(System.currentTimeMillis())
                .setInvalid(false);

        role = repository.save(role);
        return role.getRoleId().getValue();
    }

    @Override
    public RoleDTO queryRole(Long roleId) {
        Role role = repository.find(new RoleId(roleId));
        return assembler.fromRole(role);
    }

    @Override
    public Boolean checkRoleExist(RoleCheck roleCheck) {
        return repository.check(roleCheck.getRoleName());
    }

    @Override
    public PageInfo<RoleDTO> queryRoles(RoleQuery roleQuery) {
        PageInfo<Role> pageInfo = repository.findAll(roleQuery);
        return PageUtils.convertPage(pageInfo, assembler::fromRole);
    }

    @Override
    public void deleteRole(Long roleId) {
        repository.remove(new RoleId(roleId));
    }

    @Override
    public void updateRole(UpdateRoleCmd command) {
        Role role = new Role()
                .setRoleId(new RoleId(command.getRoleId()))
                .setRoleName(command.getRoleName());

        role.setModifyTime(System.currentTimeMillis());
        repository.save(role);
    }
}
