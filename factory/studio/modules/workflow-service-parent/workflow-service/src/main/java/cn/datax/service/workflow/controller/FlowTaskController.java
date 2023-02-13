package cn.datax.service.workflow.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.workflow.api.dto.TaskRequest;
import cn.datax.service.workflow.api.query.FlowTaskQuery;
import cn.datax.service.workflow.api.vo.FlowHistTaskVo;
import cn.datax.service.workflow.api.vo.FlowTaskVo;
import cn.datax.service.workflow.service.FlowTaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = {"流程任务"})
@RestController
@RequestMapping("/tasks")
public class FlowTaskController extends BaseController {

    @Autowired
    private FlowTaskService flowTaskService;

    @ApiOperation(value = "执行任务", notes = "执行任务类型：claim签收 unclaim反签收 complete完成 delegate任务委派 resolve任务归还 assignee任务转办")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "request", value = "执行任务实体request", required = true, dataType = "TaskRequest")
    })
    @PostMapping(value = "/execute/{taskId}")
    public R executeTask(@PathVariable String taskId, @RequestBody TaskRequest request) {
        flowTaskService.execute(request);
        return R.ok();
    }

    @ApiOperation(value = "分页查询待办任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "flowTaskQuery", value = "查询实体flowTaskQuery", required = true, dataTypeClass = FlowTaskQuery.class)
    })
    @GetMapping("/pageTodo")
    public R pageTodo(FlowTaskQuery flowTaskQuery) {
        Page<FlowTaskVo> page = flowTaskService.pageTodo(flowTaskQuery);
        JsonPage<FlowTaskVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "分页查询已办任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "flowTaskQuery", value = "查询实体flowTaskQuery", required = true, dataTypeClass = FlowTaskQuery.class)
    })
    @GetMapping("/pageDone")
    public R pageDone(FlowTaskQuery flowTaskQuery) {
        Page<FlowHistTaskVo> page = flowTaskService.pageDone(flowTaskQuery);
        JsonPage<FlowHistTaskVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }
}
