package cn.datax.service.system.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.ConfigDto;
import cn.datax.service.system.api.entity.ConfigEntity;
import cn.datax.service.system.api.vo.ConfigVo;
import cn.datax.service.system.api.query.ConfigQuery;
import cn.datax.service.system.mapstruct.ConfigMapper;
import cn.datax.service.system.service.ConfigService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
 * 系统参数配置信息表 前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-05-19
 */
@Api(tags = {"系统参数配置信息表"})
@RestController
@RequestMapping("/configs")
public class ConfigController extends BaseController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ConfigMapper configMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getConfigById(@PathVariable String id) {
        ConfigEntity configEntity = configService.getConfigById(id);
        return R.ok().setData(configMapper.toVO(configEntity));
    }

    /**
     * 分页查询信息
     *
     * @param configQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "configQuery", value = "查询实体configQuery", required = true, dataTypeClass = ConfigQuery.class)
    })
    @GetMapping("/page")
    public R getConfigPage(ConfigQuery configQuery) {
        QueryWrapper<ConfigEntity> queryWrapper = new QueryWrapper<>();
        IPage<ConfigEntity> page = configService.page(new Page<>(configQuery.getPageNum(), configQuery.getPageSize()), queryWrapper);
        List<ConfigVo> collect = page.getRecords().stream().map(configMapper::toVO).collect(Collectors.toList());
        JsonPage<ConfigVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param config
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据config对象添加信息")
    @ApiImplicitParam(name = "config", value = "详细实体config", required = true, dataType = "ConfigDto")
    @PostMapping()
    public R saveConfig(@RequestBody @Validated({ValidationGroups.Insert.class}) ConfigDto config) {
        ConfigEntity configEntity = configService.saveConfig(config);
        return R.ok().setData(configMapper.toVO(configEntity));
    }

    /**
     * 修改
     * @param config
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "config", value = "详细实体config", required = true, dataType = "ConfigDto")
    })
    @PutMapping("/{id}")
    public R updateConfig(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ConfigDto config) {
        ConfigEntity configEntity = configService.updateConfig(config);
        return R.ok().setData(configMapper.toVO(configEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteConfigById(@PathVariable String id) {
        configService.deleteConfigById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteConfigBatch(@PathVariable List<String> ids) {
        configService.deleteConfigBatch(ids);
        return R.ok();
    }

    /**
     * 获取参数
     *
     * @return
     */
    @GetMapping("/key/{key}")
    public R getConfig(@PathVariable String key) {
        String val = configService.getConfig(key);
        return R.ok().setData(val);
    }

    /**
     * 刷新参数缓存
     *
     * @return
     */
    @GetMapping("/refresh")
    public R refreshConfig() {
        configService.refreshConfig();
        return R.ok();
    }
}
