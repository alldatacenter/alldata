package cn.datax.service.data.market.integration.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.market.api.dto.ServiceLogDto;
import cn.datax.service.data.market.api.entity.ServiceLogEntity;
import cn.datax.service.data.market.api.vo.ServiceLogVo;
import cn.datax.service.data.market.api.query.ServiceLogQuery;
import cn.datax.service.data.market.integration.mapstruct.ServiceLogMapper;
import cn.datax.service.data.market.integration.service.ServiceLogService;
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
 * 服务集成调用日志表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Api(tags = {"服务集成调用日志表"})
@RestController
@RequestMapping("/serviceLogs")
public class ServiceLogController extends BaseController {

    @Autowired
    private ServiceLogService serviceLogService;

    @Autowired
    private ServiceLogMapper serviceLogMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getServiceLogById(@PathVariable String id) {
        ServiceLogEntity serviceLogEntity = serviceLogService.getServiceLogById(id);
        return R.ok().setData(serviceLogMapper.toVO(serviceLogEntity));
    }

    /**
     * 分页查询信息
     *
     * @param serviceLogQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceLogQuery", value = "查询实体serviceLogQuery", required = true, dataTypeClass = ServiceLogQuery.class)
    })
    @GetMapping("/page")
    public R getServiceLogPage(ServiceLogQuery serviceLogQuery) {
        QueryWrapper<ServiceLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(serviceLogQuery.getServiceName()), "service_name", serviceLogQuery.getServiceName());
        IPage<ServiceLogEntity> page = serviceLogService.page(new Page<>(serviceLogQuery.getPageNum(), serviceLogQuery.getPageSize()), queryWrapper);
        List<ServiceLogVo> collect = page.getRecords().stream().map(serviceLogMapper::toVO).collect(Collectors.toList());
        JsonPage<ServiceLogVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param serviceLog
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据serviceLog对象添加信息")
    @ApiImplicitParam(name = "serviceLog", value = "详细实体serviceLog", required = true, dataType = "ServiceLogDto")
    @PostMapping()
    public R saveServiceLog(@RequestBody @Validated({ValidationGroups.Insert.class}) ServiceLogDto serviceLog) {
        ServiceLogEntity serviceLogEntity = serviceLogService.saveServiceLog(serviceLog);
        return R.ok().setData(serviceLogMapper.toVO(serviceLogEntity));
    }

    /**
     * 修改
     * @param serviceLog
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "serviceLog", value = "详细实体serviceLog", required = true, dataType = "ServiceLogDto")
    })
    @PutMapping("/{id}")
    public R updateServiceLog(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ServiceLogDto serviceLog) {
        ServiceLogEntity serviceLogEntity = serviceLogService.updateServiceLog(serviceLog);
        return R.ok().setData(serviceLogMapper.toVO(serviceLogEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteServiceLogById(@PathVariable String id) {
        serviceLogService.deleteServiceLogById(id);
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
    public R deleteServiceLogBatch(@PathVariable List<String> ids) {
        serviceLogService.deleteServiceLogBatch(ids);
        return R.ok();
    }
}
