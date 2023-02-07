package cn.datax.service.data.masterdata.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.masterdata.api.dto.ModelDto;
import cn.datax.service.data.masterdata.api.entity.ModelDataEntity;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import cn.datax.service.data.masterdata.api.vo.ModelVo;
import cn.datax.service.data.masterdata.api.query.ModelQuery;
import cn.datax.service.data.masterdata.mapstruct.ModelMapstruct;
import cn.datax.service.data.masterdata.service.ModelService;
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
 * 主数据模型表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Api(tags = {"主数据模型表"})
@RestController
@RequestMapping("/models")
public class ModelController extends BaseController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelMapstruct modelMapstruct;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getModelById(@PathVariable String id) {
        ModelEntity modelEntity = modelService.getModelById(id);
        return R.ok().setData(modelMapstruct.toVO(modelEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getModelList() {
        QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        queryWrapper.eq("flow_status", DataConstant.AuditState.AGREE.getKey());
        List<ModelEntity> list = modelService.list(queryWrapper);
        List<ModelVo> collect = list.stream().map(modelMapstruct::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param modelQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelQuery", value = "查询实体modelQuery", required = true, dataTypeClass = ModelQuery.class)
    })
    @GetMapping("/page")
    public R getModelPage(ModelQuery modelQuery) {
        QueryWrapper<ModelEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(modelQuery.getModelName()), "model_name", modelQuery.getModelName());
        IPage<ModelEntity> page = modelService.page(new Page<>(modelQuery.getPageNum(), modelQuery.getPageSize()), queryWrapper);
        List<ModelVo> collect = page.getRecords().stream().map(modelMapstruct::toVO).collect(Collectors.toList());
        JsonPage<ModelVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param model
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据model对象添加信息")
    @ApiImplicitParam(name = "model", value = "详细实体model", required = true, dataType = "ModelDto")
    @PostMapping()
    public R saveModel(@RequestBody @Validated({ValidationGroups.Insert.class}) ModelDto model) {
        ModelEntity modelEntity = modelService.saveModel(model);
        return R.ok().setData(modelMapstruct.toVO(modelEntity));
    }

    /**
     * 修改
     * @param model
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "model", value = "详细实体model", required = true, dataType = "ModelDto")
    })
    @PutMapping("/{id}")
    public R updateModel(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ModelDto model) {
        ModelEntity modelEntity = modelService.updateModel(model);
        return R.ok().setData(modelMapstruct.toVO(modelEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteModelById(@PathVariable String id) {
        modelService.deleteModelById(id);
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
    public R deleteModelBatch(@PathVariable List<String> ids) {
        modelService.deleteModelBatch(ids);
        return R.ok();
    }

    /**
     * 工作流提交
     * @param id
     * @return
     */
    @PostMapping("/submit/{id}")
    public R submitModelById(@PathVariable String id) {
        modelService.submitModelById(id);
        return R.ok();
    }

    @PostMapping("/table/create/{id}")
    public R createTable(@PathVariable String id) {
        modelService.createTable(id);
        return R.ok();
    }

    @DeleteMapping("/table/drop/{id}")
    public R dropTable(@PathVariable String id) {
        modelService.dropTable(id);
        return R.ok();
    }

    @GetMapping("/table/param/{id}")
    public R getTableParamById(@PathVariable String id) {
        Map<String, Object> map = modelService.getTableParamById(id);
        return R.ok().setData(map);
    }

    @GetMapping("/form/param/{id}")
    public R getFormParamById(@PathVariable String id) {
        Map<String, Object> map = modelService.getFormParamById(id);
        return R.ok().setData(map);
    }
}
