package cn.datax.service.data.market.integration.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.market.api.dto.ServiceIntegrationDto;
import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import cn.datax.service.data.market.api.vo.ServiceIntegrationVo;
import cn.datax.service.data.market.api.query.ServiceIntegrationQuery;
import cn.datax.service.data.market.integration.mapstruct.ServiceIntegrationMapper;
import cn.datax.service.data.market.integration.service.ServiceIntegrationService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务集成表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Api(tags = {"服务集成表"})
@RestController
@RequestMapping("/services")
public class ServiceIntegrationController extends BaseController {

    @Autowired
    private ServiceIntegrationService serviceIntegrationService;

    @Autowired
    private ServiceIntegrationMapper serviceIntegrationMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getServiceIntegrationById(@PathVariable String id) {
        ServiceIntegrationEntity serviceIntegrationEntity = serviceIntegrationService.getServiceIntegrationById(id);
        return R.ok().setData(serviceIntegrationMapper.toVO(serviceIntegrationEntity));
    }

    /**
     * 分页查询信息
     *
     * @param serviceIntegrationQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceIntegrationQuery", value = "查询实体serviceIntegrationQuery", required = true, dataTypeClass = ServiceIntegrationQuery.class)
    })
    @GetMapping("/page")
    public R getServiceIntegrationPage(ServiceIntegrationQuery serviceIntegrationQuery) {
        QueryWrapper<ServiceIntegrationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(serviceIntegrationQuery.getServiceName()), "service_name", serviceIntegrationQuery.getServiceName());
        IPage<ServiceIntegrationEntity> page = serviceIntegrationService.page(new Page<>(serviceIntegrationQuery.getPageNum(), serviceIntegrationQuery.getPageSize()), queryWrapper);
        List<ServiceIntegrationVo> collect = page.getRecords().stream().map(serviceIntegrationMapper::toVO).collect(Collectors.toList());
        JsonPage<ServiceIntegrationVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param serviceIntegration
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据serviceIntegration对象添加信息")
    @ApiImplicitParam(name = "serviceIntegration", value = "详细实体serviceIntegration", required = true, dataType = "ServiceIntegrationDto")
    @PostMapping()
    public R saveServiceIntegration(@RequestBody @Validated({ValidationGroups.Insert.class}) ServiceIntegrationDto serviceIntegration) {
        ServiceIntegrationEntity serviceIntegrationEntity = serviceIntegrationService.saveServiceIntegration(serviceIntegration);
        return R.ok().setData(serviceIntegrationMapper.toVO(serviceIntegrationEntity));
    }

    /**
     * 修改
     * @param serviceIntegration
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "serviceIntegration", value = "详细实体serviceIntegration", required = true, dataType = "ServiceIntegrationDto")
    })
    @PutMapping("/{id}")
    public R updateServiceIntegration(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ServiceIntegrationDto serviceIntegration) {
        ServiceIntegrationEntity serviceIntegrationEntity = serviceIntegrationService.updateServiceIntegration(serviceIntegration);
        return R.ok().setData(serviceIntegrationMapper.toVO(serviceIntegrationEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteServiceIntegrationById(@PathVariable String id) {
        serviceIntegrationService.deleteServiceIntegrationById(id);
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
    public R deleteServiceIntegrationBatch(@PathVariable List<String> ids) {
        serviceIntegrationService.deleteServiceIntegrationBatch(ids);
        return R.ok();
    }

    @GetMapping("/detail/{id}")
    public R getServiceIntegrationDetailById(@PathVariable String id) {
        Map<String, Object> map =  serviceIntegrationService.getServiceIntegrationDetailById(id);
        return R.ok().setData(map);
    }
}
