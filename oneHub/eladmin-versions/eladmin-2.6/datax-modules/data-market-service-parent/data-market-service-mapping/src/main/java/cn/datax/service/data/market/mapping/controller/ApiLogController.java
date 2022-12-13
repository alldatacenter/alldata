package cn.datax.service.data.market.mapping.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.market.api.dto.ApiLogDto;
import cn.datax.service.data.market.api.entity.ApiLogEntity;
import cn.datax.service.data.market.api.query.ApiLogQuery;
import cn.datax.service.data.market.api.vo.ApiLogVo;
import cn.datax.service.data.market.mapping.mapstruct.ApiLogMapper;
import cn.datax.service.data.market.mapping.service.ApiLogService;
import cn.hutool.core.util.StrUtil;
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
 * api调用日志信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-21
 */
@Api(tags = {"api调用日志信息表"})
@RestController
@RequestMapping("/apiLogs")
public class ApiLogController extends BaseController {

    @Autowired
    private ApiLogService apiLogService;

    @Autowired
    private ApiLogMapper apiLogMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getApiLogById(@PathVariable String id) {
        ApiLogEntity apiLogEntity = apiLogService.getApiLogById(id);
        return R.ok().setData(apiLogMapper.toVO(apiLogEntity));
    }

    /**
     * 分页查询信息
     *
     * @param apiLogQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "apiLogQuery", value = "查询实体apiLogQuery", required = true, dataTypeClass = ApiLogQuery.class)
    })
    @GetMapping("/page")
    public R getApiLogPage(ApiLogQuery apiLogQuery) {
        QueryWrapper<ApiLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(apiLogQuery.getApiName()), "api.api_name", apiLogQuery.getApiName());
        IPage<ApiLogEntity> page = apiLogService.page(new Page<>(apiLogQuery.getPageNum(), apiLogQuery.getPageSize()), queryWrapper.orderByDesc("id"));
        List<ApiLogVo> collect = page.getRecords().stream().map(apiLogMapper::toVO).collect(Collectors.toList());
        JsonPage<ApiLogVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param apiLog
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据apiLog对象添加信息")
    @ApiImplicitParam(name = "apiLog", value = "详细实体apiLog", required = true, dataType = "ApiLogDto")
    @PostMapping()
    public R saveApiLog(@RequestBody @Validated({ValidationGroups.Insert.class}) ApiLogDto apiLog) {
        ApiLogEntity apiLogEntity = apiLogService.saveApiLog(apiLog);
        return R.ok().setData(apiLogMapper.toVO(apiLogEntity));
    }

    /**
     * 修改
     * @param apiLog
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "apiLog", value = "详细实体apiLog", required = true, dataType = "ApiLogDto")
    })
    @PutMapping("/{id}")
    public R updateApiLog(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ApiLogDto apiLog) {
        ApiLogEntity apiLogEntity = apiLogService.updateApiLog(apiLog);
        return R.ok().setData(apiLogMapper.toVO(apiLogEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteApiLogById(@PathVariable String id) {
        apiLogService.deleteApiLogById(id);
        return R.ok();
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除角色", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteApiLogBatch(@PathVariable List<String> ids) {
        apiLogService.deleteApiLogBatch(ids);
        return R.ok();
    }
}
