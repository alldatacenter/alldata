package cn.datax.service.data.market.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.market.api.dto.ApiMaskDto;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.query.ApiMaskQuery;
import cn.datax.service.data.market.api.vo.ApiMaskVo;
import cn.datax.service.data.market.mapstruct.ApiMaskMapper;
import cn.datax.service.data.market.service.ApiMaskService;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据API脱敏信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Api(tags = {"数据API脱敏信息表"})
@RestController
@RequestMapping("/apiMasks")
public class ApiMaskController extends BaseController {

    @Autowired
    private ApiMaskService apiMaskService;

    @Autowired
    private ApiMaskMapper apiMaskMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getApiMaskById(@PathVariable String id) {
        ApiMaskEntity apiMaskEntity = apiMaskService.getApiMaskById(id);
        return R.ok().setData(apiMaskMapper.toVO(apiMaskEntity));
    }

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/api/{id}")
    public R getApiMaskByApiId(@PathVariable String id) {
        ApiMaskEntity apiMaskEntity = apiMaskService.getApiMaskByApiId(id);
        return R.ok().setData(apiMaskMapper.toVO(apiMaskEntity));
    }

    /**
     * 分页查询信息
     *
     * @param apiMaskQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "apiMaskQuery", value = "查询实体apiMaskQuery", required = true, dataTypeClass = ApiMaskQuery.class)
    })
    @GetMapping("/page")
    public R getApiMaskPage(ApiMaskQuery apiMaskQuery) {
        QueryWrapper<ApiMaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(apiMaskQuery.getMaskName()), "mask_name", apiMaskQuery.getMaskName());
        IPage<ApiMaskEntity> page = apiMaskService.page(new Page<>(apiMaskQuery.getPageNum(), apiMaskQuery.getPageSize()), queryWrapper);
        List<ApiMaskVo> collect = page.getRecords().stream().map(apiMaskMapper::toVO).collect(Collectors.toList());
        JsonPage<ApiMaskVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param apiMask
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据apiMask对象添加信息")
    @ApiImplicitParam(name = "apiMask", value = "详细实体apiMask", required = true, dataType = "ApiMaskDto")
    @PostMapping()
    public R saveApiMask(@RequestBody @Validated({ValidationGroups.Insert.class}) ApiMaskDto apiMask) {
        apiMaskService.saveApiMask(apiMask);
        return R.ok();
    }

    /**
     * 修改
     * @param apiMask
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "apiMask", value = "详细实体apiMask", required = true, dataType = "ApiMaskDto")
    })
    @PutMapping("/{id}")
    public R updateApiMask(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ApiMaskDto apiMask) {
        apiMaskService.updateApiMask(apiMask);
        return R.ok();
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteApiMaskById(@PathVariable String id) {
        apiMaskService.deleteApiMaskById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteApiMaskBatch(@PathVariable List<String> ids) {
        apiMaskService.deleteApiMaskBatch(ids);
        return R.ok();
    }
}
