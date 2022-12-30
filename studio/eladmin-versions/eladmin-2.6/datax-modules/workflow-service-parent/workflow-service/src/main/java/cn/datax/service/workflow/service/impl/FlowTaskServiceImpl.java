package cn.datax.service.workflow.service.impl;

import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.workflow.api.dto.TaskRequest;
import cn.datax.service.workflow.api.enums.ActionEnum;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import cn.datax.service.workflow.api.query.FlowTaskQuery;
import cn.datax.service.workflow.api.vo.FlowHistTaskVo;
import cn.datax.service.workflow.api.vo.FlowTaskVo;
import cn.datax.service.workflow.service.FlowTaskService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class FlowTaskServiceImpl implements FlowTaskService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Override
    public Page<FlowTaskVo> pageTodo(FlowTaskQuery flowTaskQuery) {
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery.taskCandidateOrAssigned(SecurityUtil.getUserId()).taskCandidateGroupIn(SecurityUtil.getUserRoleIds());
        if(StrUtil.isNotBlank(flowTaskQuery.getBusinessKey())){
            taskQuery.processInstanceBusinessKey(flowTaskQuery.getBusinessKey());
        }
        if(StrUtil.isNotBlank(flowTaskQuery.getBusinessCode())){
            taskQuery.processVariableValueEquals(VariablesEnum.businessCode.toString(), flowTaskQuery.getBusinessCode());
        }
        if(StrUtil.isNotBlank(flowTaskQuery.getBusinessName())){
            taskQuery.processVariableValueLike(VariablesEnum.businessName.toString(), flowTaskQuery.getBusinessName());
        }
        List<Task> taskList = taskQuery.includeProcessVariables()
                .orderByTaskCreateTime().asc()
                .listPage((flowTaskQuery.getPageNum() - 1) * flowTaskQuery.getPageSize(), flowTaskQuery.getPageSize());
        List<FlowTaskVo> list = new ArrayList<>();
        taskList.stream().forEach(task -> {
            FlowTaskVo flowTaskVo = new FlowTaskVo();
            BeanUtil.copyProperties(task, flowTaskVo, "variables");
            //放入流程参数
            flowTaskVo.setVariables(task.getProcessVariables());
            //是否委派任务
            flowTaskVo.setIsDelegation(DelegationState.PENDING.equals(task.getDelegationState()));
            list.add(flowTaskVo);
        });
        long count = taskQuery.count();
        Page<FlowTaskVo> page = new Page<>(flowTaskQuery.getPageNum(), flowTaskQuery.getPageSize());
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    @Override
    public Page<FlowHistTaskVo> pageDone(FlowTaskQuery flowTaskQuery) {
        HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
        historicTaskInstanceQuery.taskAssignee(SecurityUtil.getUserId());
        if (StrUtil.isNotBlank(flowTaskQuery.getBusinessKey())) {
            historicTaskInstanceQuery.processInstanceBusinessKey(flowTaskQuery.getBusinessKey());
        }
        if (StrUtil.isNotBlank(flowTaskQuery.getBusinessCode())) {
            historicTaskInstanceQuery.processVariableValueEquals(VariablesEnum.businessCode.toString(), flowTaskQuery.getBusinessCode());
        }
        if (StrUtil.isNotBlank(flowTaskQuery.getBusinessName())) {
            historicTaskInstanceQuery.processVariableValueLike(VariablesEnum.businessName.toString(), flowTaskQuery.getBusinessName());
        }
        List<HistoricTaskInstance> historicTaskInstanceList = historicTaskInstanceQuery.finished()
                .includeProcessVariables().orderByHistoricTaskInstanceEndTime().desc()
                .listPage((flowTaskQuery.getPageNum() - 1) * flowTaskQuery.getPageSize(), flowTaskQuery.getPageSize());
        List<FlowHistTaskVo> list = new ArrayList<>();
        historicTaskInstanceList.stream().forEach(task -> {
            FlowHistTaskVo flowHistTaskVo = new FlowHistTaskVo();
            BeanUtil.copyProperties(task, flowHistTaskVo, "variables");
            //放入流程参数
            flowHistTaskVo.setVariables(task.getProcessVariables());
            list.add(flowHistTaskVo);
        });
        long count = historicTaskInstanceQuery.count();
        Page<FlowHistTaskVo> page = new Page<>(flowTaskQuery.getPageNum(), flowTaskQuery.getPageSize());
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    @Override
    public void execute(TaskRequest request) {
        String action = request.getAction();
        String processInstanceId = request.getProcessInstanceId();
        String taskId = request.getTaskId();
        String userId = request.getUserId();
        String message = request.getMessage();
        Map<String, Object> variables = request.getVariables();
        log.info("执行任务类型:{},流程实例ID:{},执行任务ID:{},处理人ID:{},参数:{}", action, processInstanceId, taskId, userId, variables);
        Assert.notNull(action, "请输入执行任务类型");
        Assert.notNull(processInstanceId, "请输入流程实例ID");
        Assert.notNull(taskId, "请输入任务ID");
        ActionEnum actionEnum = ActionEnum.actionOf(action);
        switch (actionEnum) {
            case COMPLETE:
                //完成任务
                this.completeTask(taskId, variables, processInstanceId, message);
                break;
            case CLAIM:
                //签收任务
                this.claimTask(taskId, processInstanceId);
                break;
            case UNCLAIM:
                //反签收
                this.unClaimTask(taskId, processInstanceId);
                break;
            case DELEGATE:
                //任务委派
                this.delegateTask(taskId, userId, processInstanceId);
                break;
            case RESOLVE:
                //任务归还
                this.resolveTask(taskId, variables, processInstanceId);
                break;
            case ASSIGNEE:
                //任务转办
                this.assigneeTask(taskId, userId, processInstanceId);
                break;
            default:
                break;
        }
    }

    private void completeTask(String taskId, Map<String, Object> variables, String processInstanceId, String message) {
        log.info("完成任务ID:{}", taskId);
        Boolean approved = (Boolean) Optional.ofNullable(variables).map(s -> s.get(VariablesEnum.approved.toString())).orElse(true);
        this.addComment(ActionEnum.COMPLETE, taskId, processInstanceId, StrUtil.isBlank(message) ? (approved ? "默认同意" : "默认不同意") : message);
        taskService.complete(taskId, variables);
    }

    private void claimTask(String taskId, String processInstanceId) {
        log.info("签收任务ID:{},签收人ID:{}", taskId, SecurityUtil.getUserId());
        this.addComment(ActionEnum.CLAIM, taskId, processInstanceId, null);
        taskService.claim(taskId, SecurityUtil.getUserId());
    }

    private void unClaimTask(String taskId, String processInstanceId) {
        log.info("反签收任务ID:{}", taskId);
        this.addComment(ActionEnum.UNCLAIM, taskId, processInstanceId, null);
        taskService.unclaim(taskId);
    }

    private void delegateTask(String taskId, String userId, String processInstanceId) {
        log.info("委派任务ID:{},委派给用户ID:{}", taskId, userId);
        this.addComment(ActionEnum.DELEGATE, taskId, processInstanceId, null);
        taskService.delegateTask(taskId, userId);
    }

    private void resolveTask(String taskId, Map<String, Object> variables, String processInstanceId) {
        log.info("任务归还ID:{}", taskId);
        this.addComment(ActionEnum.RESOLVE, taskId, processInstanceId, null);
        taskService.resolveTask(taskId);
        taskService.complete(taskId, variables);
    }

    private void assigneeTask(String taskId, String userId, String processInstanceId) {
        log.info("任务转办ID:{},移交给用户ID:{}", taskId, userId);
        this.addComment(ActionEnum.ASSIGNEE, taskId, processInstanceId, null);
        taskService.setAssignee(taskId, userId);
    }

    private void addComment(ActionEnum actionEnum, String taskId, String processInstanceId, String message) {
        log.info("任务或者流程实例添加审批意见:任务ID:{},流程实例ID{}", taskId, processInstanceId);
        Comment comment = taskService.addComment(taskId, processInstanceId, StrUtil.isBlank(message) ? actionEnum.getTitle() : message);
        comment.setUserId(SecurityUtil.getUserId());
        taskService.saveComment(comment);
    }
}
