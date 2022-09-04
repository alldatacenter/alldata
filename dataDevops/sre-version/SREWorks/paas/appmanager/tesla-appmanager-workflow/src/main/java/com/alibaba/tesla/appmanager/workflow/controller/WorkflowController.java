package com.alibaba.tesla.appmanager.workflow.controller;

import com.alibaba.tesla.appmanager.api.provider.WorkflowInstanceProvider;
import com.alibaba.tesla.appmanager.api.provider.WorkflowTaskProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowInstanceDTO;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.domain.option.WorkflowInstanceOption;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowInstanceListReq;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowTaskListReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Workflow Instance Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/workflow")
@RestController
public class WorkflowController extends AppManagerBaseController {

    @Autowired
    private WorkflowInstanceProvider workflowInstanceProvider;

    @Autowired
    private WorkflowTaskProvider workflowTaskProvider;

    // 发起部署
    @PostMapping(value = "/launch")
    @ResponseBody
    public TeslaBaseResult launch(
            @RequestParam("appId") String appId,
            @RequestBody String body, OAuth2Authentication auth) {
        WorkflowInstanceOption options = WorkflowInstanceOption.builder()
                .creator(getOperator(auth))
                .build();
        try {
            WorkflowInstanceDTO response = workflowInstanceProvider.launch(appId, body, options);
            return buildSucceedResult(response);
        } catch (Exception e) {
            log.error("cannot launch deployments|exception={}|request={}", ExceptionUtils.getStackTrace(e), body);
            return buildExceptionResult(e);
        }
    }

    // 根据过滤条件查询 Workflow Instance 列表
    @GetMapping
    @ResponseBody
    public TeslaBaseResult list(
            @ModelAttribute WorkflowInstanceListReq request, OAuth2Authentication auth
    ) throws Exception {
        Pagination<WorkflowInstanceDTO> response = workflowInstanceProvider.list(request);
        return buildSucceedResult(response);
    }

    // 查询指定 Workflow Instance 详情
    @GetMapping("{instanceId}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable("instanceId") Long instanceId, OAuth2Authentication auth
    ) throws Exception {
        WorkflowInstanceDTO response = workflowInstanceProvider.get(instanceId, true);
        return buildSucceedResult(response);
    }

    // 根据过滤条件查询 Workflow Task 列表
    @GetMapping("{instanceId}/tasks")
    @ResponseBody
    public TeslaBaseResult listTask(
            @PathVariable("instanceId") Long instanceId,
            @ModelAttribute WorkflowTaskListReq request, OAuth2Authentication auth
    ) throws Exception {
        request.setInstanceId(instanceId);
        Pagination<WorkflowTaskDTO> response = workflowTaskProvider.list(request);
        for (int i = 0; i < response.getItems().size(); i++) {
            WorkflowTaskDTO current = response.getItems().get(i);
            current.setBatchId((long) (i + 1));
            if (StringUtils.isEmpty(current.getDeployAppUnitId())) {
                current.setDeployAppUnitId("internal");
            }
        }
        return buildSucceedResult(response);
    }

    // 查询指定 Workflow Task 详情
    @GetMapping("{instanceId}/tasks/{taskId}")
    @ResponseBody
    public TeslaBaseResult getTask(
            @PathVariable("instanceId") Long instanceId,
            @PathVariable("taskId") Long taskId,
            OAuth2Authentication auth
    ) throws Exception {
        WorkflowTaskDTO response = workflowTaskProvider.get(taskId, true);
        return buildSucceedResult(response);
    }
}