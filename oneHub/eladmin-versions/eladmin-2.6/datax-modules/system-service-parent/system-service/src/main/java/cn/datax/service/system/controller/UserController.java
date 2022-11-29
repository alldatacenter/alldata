package cn.datax.service.system.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.log.annotation.LogAop;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.UserDto;
import cn.datax.service.system.api.dto.UserPasswordDto;
import cn.datax.service.system.api.entity.UserEntity;
import cn.datax.service.system.api.query.UserQuery;
import cn.datax.service.system.api.vo.UserVo;
import cn.datax.service.system.mapstruct.UserMapper;
import cn.datax.service.system.service.UserService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yuwei
 * @since 2019-09-04
 */
@Slf4j
@Api(value="系统管理接口", tags = {"系统管理"})
@RestController
@RequestMapping("/users")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @LogAop(module = "datax-service-system", value = "根据id获取用户详细信息")
    @ApiOperation(value = "获取用户详细信息", notes = "根据url的id来获取用户详细信息")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getUserById(@PathVariable String id) {
        UserEntity userEntity = userService.getById(id);
        return R.ok().setData(userMapper.toVO(userEntity));
    }

    @ApiOperation(value = "用户分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userQuery", value = "用户查询实体userQuery", required = true, dataTypeClass = UserQuery.class)
    })
    @GetMapping("/page")
    public R getUserPage(UserQuery userQuery) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(userQuery.getUsername()), "username", userQuery.getUsername());
        queryWrapper.like(StrUtil.isNotBlank(userQuery.getNickname()), "nickname", userQuery.getNickname());
        queryWrapper.eq(StrUtil.isNotBlank(userQuery.getDeptId()), "dept_id", userQuery.getDeptId());
        if(CollUtil.isNotEmpty(userQuery.getOrderList())){
            userQuery.getOrderList().stream().forEach(orderItem -> {
                queryWrapper.orderBy(StrUtil.isNotBlank(orderItem.getColumn()), orderItem.isAsc(), orderItem.getColumn());
            });
        }
        IPage<UserEntity> page = userService.page(new Page<>(userQuery.getPageNum(), userQuery.getPageSize()), queryWrapper);
        List<UserVo> collect = page.getRecords().stream().map(userMapper::toVO).collect(Collectors.toList());
        JsonPage<UserVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "创建用户", notes = "根据user对象创建用户")
    @ApiImplicitParam(name = "user", value = "用户详细实体user", required = true, dataTypeClass = UserDto.class)
    @PostMapping()
    public R saveUser(@RequestBody @Validated({ValidationGroups.Insert.class}) UserDto user) {
        UserEntity userEntity = userService.saveUser(user);
        return R.ok().setData(userMapper.toVO(userEntity));
    }

    @ApiOperation(value = "更新用户详细信息", notes = "根据url的id来指定更新对象，并根据传过来的user信息来更新用户详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "user", value = "用户详细实体user", required = true, dataTypeClass = UserDto.class)
    })
    @PutMapping("/{id}")
    public R updateUser(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) UserDto user) {
        UserEntity userEntity = userService.updateUser(user);
        return R.ok().setData(userMapper.toVO(userEntity));
    }

    @ApiOperation(value = "删除用户", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteUser(@PathVariable String id) {
        userService.deleteUserById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除用户", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "用户ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deletePostBatch(@PathVariable List<String> ids) {
        userService.deleteUserBatch(ids);
        return R.ok();
    }

    @ApiOperation(value = "更新用户密码", notes = "根据url的id来指定更新对象，并根据传过来的user信息来更新用户密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "user", value = "用户详细实体user", required = true, dataTypeClass = UserPasswordDto.class)
    })
    @PutMapping("/password")
    public R updateUserPassword(@RequestBody @Validated UserPasswordDto user) {
        userService.updateUserPassword(user);
        return R.ok();
    }

    @PutMapping("/reset/password")
    public R resetUserPassword(@RequestBody @Validated UserPasswordDto user) {
        userService.resetUserPassword(user);
        return R.ok();
    }

    @GetMapping("/route")
    public R getUserRouteById() {
        Map<String, Object> result =  userService.getRouteById();
        return R.ok().setData(result);
    }

    @ApiOperation(value = "获取审核用户", notes = "获取审核用户")
    @GetMapping("/audit")
    public R getAuditUsers() {
        List<UserEntity> list = userService.getAuditUsers();
        List<UserVo> collect = list.stream().map(userMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }
}

