package cn.datax.service.workflow.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.query.FlowInstanceQuery;
import cn.datax.service.workflow.api.vo.FlowHistInstanceVo;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import cn.datax.service.workflow.service.FlowInstanceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Api(tags = {"流程实例"})
@RestController
@RequestMapping("/instances")
public class FlowInstanceController extends BaseController {

    @Autowired
    private FlowInstanceService flowInstanceService;

    @PostMapping("/startById")
    @ApiOperation(value = "启动通过流程定义ID流程实例")
    @ApiImplicitParam(name = "request", value = "启动流程实例实体request", required = true, dataType = "ProcessInstanceCreateRequest")
    public FlowInstanceVo startById(@RequestBody ProcessInstanceCreateRequest request) {
        FlowInstanceVo flowInstanceVo = flowInstanceService.startProcessInstanceById(request);
        return flowInstanceVo;
    }

    @ApiOperation(value = "分页查询运行中的流程实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "flowInstanceQuery", value = "查询实体flowInstanceQuery", required = true, dataTypeClass = FlowInstanceQuery.class)
    })
    @GetMapping("/pageRunning")
    public R pageRunning(FlowInstanceQuery flowInstanceQuery) {
        Page<FlowInstanceVo> page = flowInstanceService.pageRunning(flowInstanceQuery);
        JsonPage<FlowInstanceVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "分页查询本人发起的流程实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "flowInstanceQuery", value = "查询实体flowInstanceQuery", required = true, dataTypeClass = FlowInstanceQuery.class)
    })
    @GetMapping("/pageMyStarted")
    public R pageMyStarted(FlowInstanceQuery flowInstanceQuery) {
        Page<FlowHistInstanceVo> page = flowInstanceService.pageMyStartedProcessInstance(flowInstanceQuery);
        JsonPage<FlowHistInstanceVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "分页查询本人参与的流程实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "flowInstanceQuery", value = "查询实体flowInstanceQuery", required = true, dataTypeClass = FlowInstanceQuery.class)
    })
    @GetMapping("/pageMyInvolved")
    public R pageMyInvolved(FlowInstanceQuery flowInstanceQuery) {
        Page<FlowHistInstanceVo> page = flowInstanceService.pageMyInvolvedProcessInstance(flowInstanceQuery);
        JsonPage<FlowHistInstanceVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "激活流程实例", notes = "根据url的id来指定激活流程实例")
    @ApiImplicitParam(name = "processInstanceId", value = "流程实例ID", required = true, dataType = "String", paramType = "path")
    @PutMapping("/activate/{processInstanceId}")
    public R activate(@PathVariable String processInstanceId) {
        flowInstanceService.activateProcessInstanceById(processInstanceId);
        return R.ok();
    }

    @ApiOperation(value = "挂起流程实例", notes = "根据url的id来指定挂起流程实例")
    @ApiImplicitParam(name = "processInstanceId", value = "流程实例ID", required = true, dataType = "String", paramType = "path")
    @PutMapping("/suspend/{processInstanceId}")
    public R suspend(@PathVariable String processInstanceId) {
        flowInstanceService.suspendProcessInstanceById(processInstanceId);
        return R.ok();
    }

    @ApiOperation(value = "删除流程实例", notes = "根据url的id来指定删除流程实例")
    @ApiImplicitParam(name = "processInstanceId", value = "流程实例ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/delete/{processInstanceId}")
    public R delete(@PathVariable String processInstanceId) {
        flowInstanceService.deleteProcessInstance(processInstanceId);
        return R.ok();
    }

    @ApiOperation(value = "流程追踪")
    @ApiImplicitParam(name = "processInstanceId", value = "流程实例ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/track")
    public void track(String processInstanceId, HttpServletResponse response) throws Exception {
        InputStream resourceAsStream = flowInstanceService.createImage(processInstanceId);
        byte[] b = new byte[1024];
        int len = -1;
        while ((len = resourceAsStream.read(b, 0, 1024)) != -1) {
            response.getOutputStream().write(b, 0, len);
        }
    }

    @ApiOperation(value = "获取审批意见")
    @ApiImplicitParam(name = "processInstanceId", value = "流程实例ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/comments")
    public R getComments(String processInstanceId) {
        flowInstanceService.getProcessInstanceComments(processInstanceId);
        return R.ok();
    }
}
