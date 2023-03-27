package cn.datax.service.system.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.MenuDto;
import cn.datax.service.system.api.entity.MenuEntity;
import cn.datax.service.system.api.entity.RoleEntity;
import cn.datax.service.system.api.vo.MenuVo;
import cn.datax.service.system.mapstruct.MenuMapper;
import cn.datax.service.system.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
 * @date 2022-09-11
 */
@Api(value="系统管理接口", tags = {"系统管理"})
@RestController
@RequestMapping("/menus")
public class MenuController extends BaseController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuMapper menuMapper;

    @ApiOperation(value = "获取资源详细信息", notes = "根据url的id来获取资源详细信息")
    @ApiImplicitParam(name = "id", value = "资源ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getMenuById(@PathVariable String id) {
        MenuEntity menuEntity = menuService.getById(id);
        return R.ok().setData(menuMapper.toVO(menuEntity));
    }

    @ApiOperation(value = "获取资源列表", notes = "")
    @GetMapping("/list")
    public R getMenuList() {
        List<MenuEntity> list = menuService.list(Wrappers.emptyWrapper());
        List<MenuVo> collect = list.stream().map(menuMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    @ApiOperation(value = "创建资源", notes = "根据menu对象创建资源")
    @ApiImplicitParam(name = "menu", value = "资源详细实体menu", required = true, dataType = "MenuDto")
    @PostMapping()
    public R saveMenu(@RequestBody @Validated({ValidationGroups.Insert.class}) MenuDto menu) {
        MenuEntity menuEntity = menuService.saveMenu(menu);
        return R.ok().setData(menuMapper.toVO(menuEntity));
    }

    @ApiOperation(value = "更新资源详细信息", notes = "根据url的id来指定更新对象，并根据传过来的menu信息来更新资源详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "资源ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "menu", value = "资源详细实体menu", required = true, dataType = "MenuDto")
    })
    @PutMapping("/{id}")
    public R updateMenu(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) MenuDto menu) {
        MenuEntity menuEntity = menuService.updateMenu(menu);
        return R.ok().setData(menuMapper.toVO(menuEntity));
    }

    @ApiOperation(value = "删除资源", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "资源ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteMenu(@PathVariable String id) {
        menuService.deleteMenuById(id);
        return R.ok();
    }

    @ApiOperation(value = "获取工作流资源列表", notes = "")
    @GetMapping("/list/flow")
    public R getMenuListForFlow() {
        QueryWrapper<MenuEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        queryWrapper.eq("menu_type", DataConstant.MenuType.MENU.getKey());
        queryWrapper.eq("menu_hidden", DataConstant.EnableState.DISABLE.getKey());
        queryWrapper.isNotNull("menu_code");
        List<MenuEntity> list = menuService.list(queryWrapper);
        List<MenuVo> collect = list.stream().map(menuMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }
}

