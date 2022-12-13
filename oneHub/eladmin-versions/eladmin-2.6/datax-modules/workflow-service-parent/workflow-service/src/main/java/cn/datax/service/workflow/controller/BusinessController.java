package cn.datax.service.workflow.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.workflow.api.dto.BusinessDto;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import cn.datax.service.workflow.api.vo.BusinessVo;
import cn.datax.service.workflow.api.query.BusinessQuery;
import cn.datax.service.workflow.mapstruct.BusinessMapper;
import cn.datax.service.workflow.service.BusinessService;
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
 * 业务流程配置表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Api(tags = {"业务流程配置表"})
@RestController
@RequestMapping("/business")
public class BusinessController extends BaseController {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private BusinessMapper businessMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getBusinessById(@PathVariable String id) {
        BusinessEntity businessEntity = businessService.getBusinessById(id);
        return R.ok().setData(businessMapper.toVO(businessEntity));
    }

    /**
     * 分页查询信息
     *
     * @param businessQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "businessQuery", value = "查询实体businessQuery", required = true, dataTypeClass = BusinessQuery.class)
    })
    @GetMapping("/page")
    public R getBusinessPage(BusinessQuery businessQuery) {
        QueryWrapper<BusinessEntity> queryWrapper = new QueryWrapper<>();
        IPage<BusinessEntity> page = businessService.page(new Page<>(businessQuery.getPageNum(), businessQuery.getPageSize()), queryWrapper);
        List<BusinessVo> collect = page.getRecords().stream().map(businessMapper::toVO).collect(Collectors.toList());
        JsonPage<BusinessVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param business
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据business对象添加信息")
    @ApiImplicitParam(name = "business", value = "详细实体business", required = true, dataType = "BusinessDto")
    @PostMapping()
    public R saveBusiness(@RequestBody @Validated({ValidationGroups.Insert.class}) BusinessDto business) {
        BusinessEntity businessEntity = businessService.saveBusiness(business);
		businessService.refreshBusiness();
        return R.ok().setData(businessMapper.toVO(businessEntity));
    }

    /**
     * 修改
     * @param business
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "business", value = "详细实体business", required = true, dataType = "BusinessDto")
    })
    @PutMapping("/{id}")
    public R updateBusiness(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) BusinessDto business) {
        BusinessEntity businessEntity = businessService.updateBusiness(business);
		businessService.refreshBusiness();
        return R.ok().setData(businessMapper.toVO(businessEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteBusinessById(@PathVariable String id) {
        businessService.deleteBusinessById(id);
		businessService.refreshBusiness();
        return R.ok();
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteBusinessBatch(@PathVariable List<String> ids) {
        businessService.deleteBusinessBatch(ids);
		businessService.refreshBusiness();
        return R.ok();
    }

    /**
     * 刷新缓存
     *
     * @return
     */
    @GetMapping("/refresh")
    public R refreshBusiness() {
        businessService.refreshBusiness();
        return R.ok();
    }
}
