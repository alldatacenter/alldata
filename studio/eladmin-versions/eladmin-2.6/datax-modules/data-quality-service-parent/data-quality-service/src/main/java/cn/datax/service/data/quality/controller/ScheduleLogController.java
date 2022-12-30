package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.quality.api.entity.ScheduleLogEntity;
import cn.datax.service.data.quality.api.vo.ScheduleLogVo;
import cn.datax.service.data.quality.api.query.ScheduleLogQuery;
import cn.datax.service.data.quality.mapstruct.ScheduleLogMapper;
import cn.datax.service.data.quality.service.ScheduleLogService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据质量监控任务日志信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@Api(tags = {"数据质量监控任务日志信息表"})
@RestController
@RequestMapping("/scheduleLogs")
public class ScheduleLogController extends BaseController {

    @Autowired
    private ScheduleLogService scheduleLogService;

    @Autowired
    private ScheduleLogMapper scheduleLogMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getScheduleLogById(@PathVariable String id) {
        ScheduleLogEntity scheduleLogEntity = scheduleLogService.getScheduleLogById(id);
        return R.ok().setData(scheduleLogMapper.toVO(scheduleLogEntity));
    }

    /**
     * 分页查询信息
     *
     * @param scheduleLogQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "scheduleLogQuery", value = "查询实体scheduleLogQuery", required = true, dataTypeClass = ScheduleLogQuery.class)
    })
    @GetMapping("/page")
    public R getScheduleLogPage(ScheduleLogQuery scheduleLogQuery) {
        QueryWrapper<ScheduleLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(scheduleLogQuery.getExecuteJobId()), "l.execute_job_id", scheduleLogQuery.getExecuteJobId());
        queryWrapper.eq(StrUtil.isNotBlank(scheduleLogQuery.getRuleTypeId()), "t.id", scheduleLogQuery.getRuleTypeId());
        IPage<ScheduleLogEntity> page = scheduleLogService.page(new Page<>(scheduleLogQuery.getPageNum(), scheduleLogQuery.getPageSize()), queryWrapper.orderByDesc("l.id"));
        List<ScheduleLogVo> collect = page.getRecords().stream().map(scheduleLogMapper::toVO).collect(Collectors.toList());
        JsonPage<ScheduleLogVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteScheduleLogById(@PathVariable String id) {
        scheduleLogService.deleteScheduleLogById(id);
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
    public R deleteScheduleLogBatch(@PathVariable List<String> ids) {
        scheduleLogService.deleteScheduleLogBatch(ids);
        return R.ok();
    }
}
