package cn.datax.service.data.standard.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.standard.api.dto.TypeDto;
import cn.datax.service.data.standard.api.entity.TypeEntity;
import cn.datax.service.data.standard.api.vo.TypeVo;
import cn.datax.service.data.standard.api.query.TypeQuery;
import cn.datax.service.data.standard.mapstruct.TypeMapper;
import cn.datax.service.data.standard.service.TypeService;
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
 * 数据标准类别表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Api(tags = {"标准类别信息表"})
@RestController
@RequestMapping("/types")
public class TypeController extends BaseController {

    @Autowired
    private TypeService typeService;

    @Autowired
    private TypeMapper typeMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getTypeById(@PathVariable String id) {
        TypeEntity typeEntity = typeService.getTypeById(id);
        return R.ok().setData(typeMapper.toVO(typeEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getTypeList() {
        QueryWrapper<TypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        List<TypeEntity> list = typeService.list(queryWrapper);
        List<TypeVo> collect = list.stream().map(typeMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param typeQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "typeQuery", value = "查询实体typeQuery", required = true, dataTypeClass = TypeQuery.class)
    })
    @GetMapping("/page")
    public R getTypePage(TypeQuery typeQuery) {
        QueryWrapper<TypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(typeQuery.getGbTypeCode()), "gb_type_code", typeQuery.getGbTypeCode());
        queryWrapper.like(StrUtil.isNotBlank(typeQuery.getGbTypeName()), "gb_type_name", typeQuery.getGbTypeName());
        IPage<TypeEntity> page = typeService.page(new Page<>(typeQuery.getPageNum(), typeQuery.getPageSize()), queryWrapper);
        List<TypeVo> collect = page.getRecords().stream().map(typeMapper::toVO).collect(Collectors.toList());
        JsonPage<TypeVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param type
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据type对象添加信息")
    @ApiImplicitParam(name = "type", value = "详细实体type", required = true, dataType = "TypeDto")
    @PostMapping()
    public R saveType(@RequestBody @Validated({ValidationGroups.Insert.class}) TypeDto type) {
        TypeEntity typeEntity = typeService.saveType(type);
        return R.ok().setData(typeMapper.toVO(typeEntity));
    }

    /**
     * 修改
     * @param type
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "type", value = "详细实体type", required = true, dataType = "TypeDto")
    })
    @PutMapping("/{id}")
    public R updateType(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) TypeDto type) {
        TypeEntity typeEntity = typeService.updateType(type);
        return R.ok().setData(typeMapper.toVO(typeEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteTypeById(@PathVariable String id) {
        typeService.deleteTypeById(id);
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
    public R deleteTypeBatch(@PathVariable List<String> ids) {
        typeService.deleteTypeBatch(ids);
        return R.ok();
    }
}
