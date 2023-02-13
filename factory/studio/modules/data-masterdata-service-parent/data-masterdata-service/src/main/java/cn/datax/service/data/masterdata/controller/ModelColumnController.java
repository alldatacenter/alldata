package cn.datax.service.data.masterdata.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.masterdata.api.dto.ModelColumnDto;
import cn.datax.service.data.masterdata.api.entity.ModelColumnEntity;
import cn.datax.service.data.masterdata.api.vo.ModelColumnVo;
import cn.datax.service.data.masterdata.api.query.ModelColumnQuery;
import cn.datax.service.data.masterdata.mapstruct.ModelColumnMapstruct;
import cn.datax.service.data.masterdata.service.ModelColumnService;
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
 * 主数据模型列信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Api(tags = {"主数据模型列信息表"})
@RestController
@RequestMapping("/columns")
public class ModelColumnController extends BaseController {

    @Autowired
    private ModelColumnService modelColumnService;

    @Autowired
    private ModelColumnMapstruct modelColumnMapstruct;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getModelColumnById(@PathVariable String id) {
        ModelColumnEntity modelColumnEntity = modelColumnService.getModelColumnById(id);
        return R.ok().setData(modelColumnMapstruct.toVO(modelColumnEntity));
    }

    /**
     * 分页查询信息
     *
     * @param modelColumnQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelColumnQuery", value = "查询实体modelColumnQuery", required = true, dataTypeClass = ModelColumnQuery.class)
    })
    @GetMapping("/page")
    public R getModelColumnPage(ModelColumnQuery modelColumnQuery) {
        QueryWrapper<ModelColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(modelColumnQuery.getModelId()), "model_id", modelColumnQuery.getModelId());
        queryWrapper.like(StrUtil.isNotBlank(modelColumnQuery.getColumnName()), "column_name", modelColumnQuery.getColumnName());
        IPage<ModelColumnEntity> page = modelColumnService.page(new Page<>(modelColumnQuery.getPageNum(), modelColumnQuery.getPageSize()), queryWrapper);
        List<ModelColumnVo> collect = page.getRecords().stream().map(modelColumnMapstruct::toVO).collect(Collectors.toList());
        JsonPage<ModelColumnVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param modelColumn
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据modelColumn对象添加信息")
    @ApiImplicitParam(name = "modelColumn", value = "详细实体modelColumn", required = true, dataType = "ModelColumnDto")
    @PostMapping()
    public R saveModelColumn(@RequestBody @Validated({ValidationGroups.Insert.class}) ModelColumnDto modelColumn) {
        ModelColumnEntity modelColumnEntity = modelColumnService.saveModelColumn(modelColumn);
        return R.ok().setData(modelColumnMapstruct.toVO(modelColumnEntity));
    }

    /**
     * 修改
     * @param modelColumn
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "modelColumn", value = "详细实体modelColumn", required = true, dataType = "ModelColumnDto")
    })
    @PutMapping("/{id}")
    public R updateModelColumn(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ModelColumnDto modelColumn) {
        ModelColumnEntity modelColumnEntity = modelColumnService.updateModelColumn(modelColumn);
        return R.ok().setData(modelColumnMapstruct.toVO(modelColumnEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteModelColumnById(@PathVariable String id) {
        modelColumnService.deleteModelColumnById(id);
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
    public R deleteModelColumnBatch(@PathVariable List<String> ids) {
        modelColumnService.deleteModelColumnBatch(ids);
        return R.ok();
    }
}
