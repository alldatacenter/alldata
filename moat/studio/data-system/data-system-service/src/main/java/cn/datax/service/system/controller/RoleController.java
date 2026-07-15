package cn.datax.service.system.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.RoleDto;
import cn.datax.service.system.api.entity.RoleEntity;
import cn.datax.service.system.api.query.RoleQuery;
import cn.datax.service.system.api.vo.RoleVo;
import cn.datax.service.system.mapstruct.RoleMapper;
import cn.datax.service.system.service.RoleService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-09-04
 */
@Api(value="系统管理接口", tags = {"系统管理"})
@RestController
@RequestMapping("/roles")
public class RoleController extends BaseController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    @ApiOperation(value = "获取角色详细信息", notes = "根据url的id来获取角色详细信息")
    @ApiImplicitParam(name = "id", value = "角色ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getRoleById(@PathVariable String id) {
        RoleEntity roleEntity = roleService.getById(id);
        return R.ok().setData(roleMapper.toVO(roleEntity));
    }

    @ApiOperation(value = "获取角色列表", notes = "")
    @GetMapping("/list")
    public R getPostList() {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        List<RoleEntity> list = roleService.list(queryWrapper);
        List<RoleVo> collect = list.stream().map(roleMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    @ApiOperation(value = "角色分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleQuery", value = "查询实体roleQuery", required = true, dataTypeClass = RoleQuery.class)
    })
    @GetMapping("/page")
    public R getRolePage(RoleQuery roleQuery) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(roleQuery.getRoleName()), "role_name", roleQuery.getRoleName());
        IPage<RoleEntity> page = roleService.page(new Page<>(roleQuery.getPageNum(), roleQuery.getPageSize()), queryWrapper);
        List<RoleVo> collect = page.getRecords().stream().map(roleMapper::toVO).collect(Collectors.toList());
        JsonPage<RoleVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "创建角色", notes = "根据role对象创建角色")
    @ApiImplicitParam(name = "role", value = "角色详细实体role", required = true, dataType = "RoleDto")
    @PostMapping()
    public R saveRole(@RequestBody @Validated({ValidationGroups.Insert.class}) RoleDto role) {
        RoleEntity roleEntity = roleService.saveRole(role);
        return R.ok().setData(roleMapper.toVO(roleEntity));
    }

    @ApiOperation(value = "更新角色详细信息", notes = "根据url的id来指定更新对象，并根据传过来的role信息来更新角色详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "角色ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "role", value = "角色详细实体role", required = true, dataType = "RoleDto")
    })
    @PutMapping("/{id}")
    public R updateRole(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) RoleDto role) {
        RoleEntity roleEntity = roleService.updateRole(role);
        return R.ok().setData(roleMapper.toVO(roleEntity));
    }

    @ApiOperation(value = "删除角色", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "角色ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteRole(@PathVariable String id) {
        roleService.deleteRoleById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除角色", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "角色ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deletePostBatch(@PathVariable List<String> ids) {
        roleService.deleteRoleBatch(ids);
        return R.ok();
    }
}

