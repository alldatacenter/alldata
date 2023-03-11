package com.hw.lineage.server.interfaces.controller;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.role.CreateRoleCmd;
import com.hw.lineage.server.application.command.role.UpdateRoleCmd;
import com.hw.lineage.server.application.dto.RoleDTO;
import com.hw.lineage.server.application.service.RoleService;
import com.hw.lineage.server.domain.query.role.RoleCheck;
import com.hw.lineage.server.domain.query.role.RoleQuery;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @description: RoleController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Roles API")
@RequestMapping("/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping("/{roleId}")
    public Result<RoleDTO> queryRole(@PathVariable("roleId") Long roleId) {
        RoleDTO roleDTO = roleService.queryRole(roleId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, roleDTO);
    }

    @GetMapping("")
    public Result<PageInfo<RoleDTO>> queryRoles(RoleQuery roleQuery) {
        PageInfo<RoleDTO> pageInfo = roleService.queryRoles(roleQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("")
    public Result<Long> createRole(@Valid @RequestBody CreateRoleCmd command) {
        Long roleId = roleService.createRole(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, roleId);
    }

    @GetMapping("/exist")
    public Result<Boolean> checkRoleExist(@Valid RoleCheck roleCheck) {
        return Result.success(ResultMessage.CHECK_SUCCESS, roleService.checkRoleExist(roleCheck));
    }

    @PutMapping("/{roleId}")
    public Result<Boolean> updateRole(@PathVariable("roleId") Long roleId,
                                        @Valid @RequestBody UpdateRoleCmd command) {
        command.setRoleId(roleId);
        roleService.updateRole(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{roleId}")
    public Result<Boolean> deleteRole(@PathVariable("roleId") Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

}
