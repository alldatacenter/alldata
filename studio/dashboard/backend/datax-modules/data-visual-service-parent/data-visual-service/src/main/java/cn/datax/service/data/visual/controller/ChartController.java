package cn.datax.service.data.visual.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.visual.api.dto.ChartConfig;
import cn.datax.service.data.visual.api.dto.ChartDto;
import cn.datax.service.data.visual.api.entity.ChartEntity;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.vo.ChartVo;
import cn.datax.service.data.visual.api.query.ChartQuery;
import cn.datax.service.data.visual.api.vo.DataSetVo;
import cn.datax.service.data.visual.mapstruct.ChartMapper;
import cn.datax.service.data.visual.service.ChartService;
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
 * 可视化图表配置信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Api(tags = {"可视化图表配置信息表"})
@RestController
@RequestMapping("/charts")
public class ChartController extends BaseController {

    @Autowired
    private ChartService chartService;

    @Autowired
    private ChartMapper chartMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getChartById(@PathVariable String id) {
        ChartEntity chartEntity = chartService.getChartById(id);
        return R.ok().setData(chartMapper.toVO(chartEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getChartList() {
        QueryWrapper<ChartEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        queryWrapper.select("id", "chart_name");
        List<ChartEntity> list = chartService.list(queryWrapper);
        List<ChartVo> collect = list.stream().map(chartMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param chartQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chartQuery", value = "查询实体chartQuery", required = true, dataTypeClass = ChartQuery.class)
    })
    @GetMapping("/page")
    public R getChartPage(ChartQuery chartQuery) {
        QueryWrapper<ChartEntity> queryWrapper = new QueryWrapper<>();
        IPage<ChartEntity> page = chartService.page(new Page<>(chartQuery.getPageNum(), chartQuery.getPageSize()), queryWrapper);
        List<ChartVo> collect = page.getRecords().stream().map(chartMapper::toVO).collect(Collectors.toList());
        JsonPage<ChartVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param chart
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据chart对象添加信息")
    @ApiImplicitParam(name = "chart", value = "详细实体chart", required = true, dataType = "ChartDto")
    @PostMapping()
    public R saveChart(@RequestBody @Validated({ValidationGroups.Insert.class}) ChartDto chart) {
        ChartEntity chartEntity = chartService.saveChart(chart);
        return R.ok().setData(chartMapper.toVO(chartEntity));
    }

    /**
     * 修改
     * @param chart
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "chart", value = "详细实体chart", required = true, dataType = "ChartDto")
    })
    @PutMapping("/{id}")
    public R updateChart(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ChartDto chart) {
        ChartEntity chartEntity = chartService.updateChart(chart);
        return R.ok().setData(chartMapper.toVO(chartEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteChartById(@PathVariable String id) {
        chartService.deleteChartById(id);
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
    public R deleteChartBatch(@PathVariable List<String> ids) {
        chartService.deleteChartBatch(ids);
        return R.ok();
    }

    @PostMapping("/copy/{id}")
    public R copyChart(@PathVariable String id) {
        chartService.copyChart(id);
        return R.ok();
    }

    @PutMapping("/build/{id}")
    public R buildChart(@PathVariable String id, @RequestBody ChartDto chart) {
        chartService.buildChart(chart);
        return R.ok();
    }

    @PostMapping("/data/parser")
    public R dataParser(@RequestBody @Validated ChartConfig chartConfig) {
        Map<String, Object> map = chartService.dataParser(chartConfig);
        return R.ok().setData(map);
    }
}
