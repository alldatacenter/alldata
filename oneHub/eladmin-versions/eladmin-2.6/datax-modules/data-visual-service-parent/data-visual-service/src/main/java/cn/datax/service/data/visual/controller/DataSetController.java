package cn.datax.service.data.visual.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.visual.api.dto.SqlParseDto;
import cn.datax.service.data.visual.api.dto.DataSetDto;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.vo.DataSetVo;
import cn.datax.service.data.visual.api.query.DataSetQuery;
import cn.datax.service.data.visual.mapstruct.DataSetMapper;
import cn.datax.service.data.visual.service.DataSetService;
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
 * 数据集信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Api(tags = {"数据集信息表"})
@RestController
@RequestMapping("/dataSets")
public class DataSetController extends BaseController {

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataSetMapper dataSetMapper;

	/**
	 * 通过ID查询信息
	 *
	 * @param sourceId
	 * @return
	 */
	@DataInner
	@GetMapping("/source/{sourceId}")
	public DataSetEntity getBySourceId(@PathVariable String sourceId) {
		return dataSetService.getBySourceId(sourceId);
	}

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getVisualDataSetById(@PathVariable String id) {
        DataSetEntity dataSetEntity = dataSetService.getDataSetById(id);
        return R.ok().setData(dataSetMapper.toVO(dataSetEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getVisualDataSetList() {
        QueryWrapper<DataSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        queryWrapper.select("id", "set_name");
        List<DataSetEntity> list = dataSetService.list(queryWrapper);
        List<DataSetVo> collect = list.stream().map(dataSetMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param dataSetQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "visualDataSetQuery", value = "查询实体visualDataSetQuery", required = true, dataTypeClass = DataSetQuery.class)
    })
    @GetMapping("/page")
    public R getVisualDataSetPage(DataSetQuery dataSetQuery) {
        QueryWrapper<DataSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(dataSetQuery.getSetName()), "set_name", dataSetQuery.getSetName());
        IPage<DataSetEntity> page = dataSetService.page(new Page<>(dataSetQuery.getPageNum(), dataSetQuery.getPageSize()), queryWrapper);
        List<DataSetVo> collect = page.getRecords().stream().map(dataSetMapper::toVO).collect(Collectors.toList());
        JsonPage<DataSetVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param dataSet
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据dataSet对象添加信息")
    @ApiImplicitParam(name = "dataSet", value = "详细实体dataSet", required = true, dataType = "DataSetDto")
    @PostMapping()
    public R saveVisualDataSet(@RequestBody @Validated({ValidationGroups.Insert.class}) DataSetDto dataSet) {
        DataSetEntity dataSetEntity = dataSetService.saveDataSet(dataSet);
        return R.ok().setData(dataSetMapper.toVO(dataSetEntity));
    }

    /**
     * 修改
     * @param dataSet
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "dataSet", value = "详细实体dataSet", required = true, dataType = "DataSetDto")
    })
    @PutMapping("/{id}")
    public R updateVisualDataSet(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) DataSetDto dataSet) {
        DataSetEntity dataSetEntity = dataSetService.updateDataSet(dataSet);
        return R.ok().setData(dataSetMapper.toVO(dataSetEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteVisualDataSetById(@PathVariable String id) {
        dataSetService.deleteDataSetById(id);
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
    public R deleteVisualDataSetBatch(@PathVariable List<String> ids) {
        dataSetService.deleteDataSetBatch(ids);
        return R.ok();
    }

    /**
     * SQL解析
     * @param sqlParseDto
     * @return
     */
    @ApiOperation(value = "SQL解析")
    @ApiImplicitParam(name = "sqlParseDto", value = "SQL解析实体sqlParseDto", required = true, dataType = "SqlParseDto")
    @PostMapping("/sql/analyse")
    public R sqlAnalyse(@RequestBody @Validated SqlParseDto sqlParseDto) {
        List<String> list = dataSetService.sqlAnalyse(sqlParseDto);
        return R.ok().setData(list);
    }
}
