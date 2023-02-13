package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.quality.api.entity.ScheduleJobEntity;
import cn.datax.service.data.quality.api.vo.ScheduleJobVo;
import cn.datax.service.data.quality.api.query.ScheduleJobQuery;
import cn.datax.service.data.quality.mapstruct.ScheduleJobMapper;
import cn.datax.service.data.quality.service.ScheduleJobService;
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
 * 数据质量监控任务信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Api(tags = {"数据质量监控任务信息表"})
@RestController
@RequestMapping("/scheduleJobs")
public class ScheduleJobController extends BaseController {

    @Autowired
    private ScheduleJobService scheduleJobService;

    @Autowired
    private ScheduleJobMapper scheduleJobMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getScheduleJobById(@PathVariable String id) {
        ScheduleJobEntity scheduleJobEntity = scheduleJobService.getScheduleJobById(id);
        return R.ok().setData(scheduleJobMapper.toVO(scheduleJobEntity));
    }

    /**
     * 分页查询信息
     *
     * @param scheduleJobQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "scheduleJobQuery", value = "查询实体scheduleJobQuery", required = true, dataTypeClass = ScheduleJobQuery.class)
    })
    @GetMapping("/page")
    public R getScheduleJobPage(ScheduleJobQuery scheduleJobQuery) {
        QueryWrapper<ScheduleJobEntity> queryWrapper = new QueryWrapper<>();
        IPage<ScheduleJobEntity> page = scheduleJobService.page(new Page<>(scheduleJobQuery.getPageNum(), scheduleJobQuery.getPageSize()), queryWrapper.orderByDesc("id"));
        List<ScheduleJobVo> collect = page.getRecords().stream().map(scheduleJobMapper::toVO).collect(Collectors.toList());
        JsonPage<ScheduleJobVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 暂停任务
     * @param id
     * @return
     */
    @ApiOperation(value = "暂停任务", notes = "根据url的id来暂停指定任务")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/pause/{id}")
    public R pauseScheduleJobById(@PathVariable("id") String id) {
        scheduleJobService.pauseScheduleJobById(id);
        return R.ok();
    }

    /**
     * 恢复任务
     * @param id
     * @return
     */
    @ApiOperation(value = "恢复任务", notes = "根据url的id来恢复指定任务")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/resume/{id}")
    public R resumeScheduleJobById(@PathVariable("id") String id) {
        scheduleJobService.resumeScheduleJobById(id);
        return R.ok();
    }

	/**
	 * 立即执行任务
	 * @param id
	 * @return
	 */
	@ApiOperation(value = "立即执行任务", notes = "根据url的id来执行指定任务")
	@ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
	@PostMapping("/run/{id}")
	public R runScheduleJobById(@PathVariable("id") String id) {
		scheduleJobService.runScheduleJobById(id);
		return R.ok();
	}
}
