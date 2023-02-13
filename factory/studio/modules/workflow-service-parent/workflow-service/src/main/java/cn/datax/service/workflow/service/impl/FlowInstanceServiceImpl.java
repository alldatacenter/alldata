package cn.datax.service.workflow.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.enums.VariablesEnum;
import cn.datax.service.workflow.api.query.FlowInstanceQuery;
import cn.datax.service.workflow.api.vo.FlowHistInstanceVo;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import cn.datax.service.workflow.service.FlowInstanceService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Comment;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FlowInstanceServiceImpl implements FlowInstanceService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String IMAGE_TYPE = "png";
    private static final String FONT_NAME = "宋体";

    @Override
    public Page<FlowInstanceVo> pageRunning(FlowInstanceQuery flowInstanceQuery) {
        ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
        if(StrUtil.isNotBlank(flowInstanceQuery.getName())){
            processInstanceQuery.processInstanceNameLike(flowInstanceQuery.getName());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessKey())){
            processInstanceQuery.processInstanceBusinessKey(flowInstanceQuery.getBusinessKey());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessCode())){
            processInstanceQuery.variableValueEquals(VariablesEnum.businessCode.toString(), flowInstanceQuery.getBusinessCode());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessName())){
            processInstanceQuery.variableValueLike(VariablesEnum.businessName.toString(), flowInstanceQuery.getBusinessName());
        }
        List<ProcessInstance> processInstanceList = processInstanceQuery.includeProcessVariables()
                .orderByStartTime().desc()
                .listPage((flowInstanceQuery.getPageNum() - 1) * flowInstanceQuery.getPageSize(), flowInstanceQuery.getPageSize());
        List<FlowInstanceVo> list = new ArrayList<>();
        processInstanceList.stream().forEach(p -> {
            FlowInstanceVo flowInstanceVo = new FlowInstanceVo();
            BeanUtil.copyProperties(p, flowInstanceVo, "variables");
            //放入流程参数
            flowInstanceVo.setVariables(p.getProcessVariables());
            list.add(flowInstanceVo);
        });
        long count = processInstanceQuery.count();
        Page<FlowInstanceVo> page = new Page<>(flowInstanceQuery.getPageNum(), flowInstanceQuery.getPageSize());
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    @Override
    public void activateProcessInstanceById(String processInstanceId) {
        log.info("成功激活流程实例ID:{}", processInstanceId);
        runtimeService.activateProcessInstanceById(processInstanceId);
    }

    @Override
    public void suspendProcessInstanceById(String processInstanceId) {
        log.info("成功挂起流程实例ID:{}", processInstanceId);
        runtimeService.suspendProcessInstanceById(processInstanceId);
    }

    @Override
    public void deleteProcessInstance(String processInstanceId) {
        // 发送消息队列
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables().processInstanceId(processInstanceId).singleResult();
        Map<String, Object> variables = processInstance.getProcessVariables();
        String businessKey = (String) variables.get(VariablesEnum.businessKey.toString());
        String businessCode = (String) variables.get(VariablesEnum.businessCode.toString());
        log.info("业务撤销：{},{}", businessKey, businessCode);
        log.info("成功删除流程实例ID:{}", processInstanceId);
        log.info("业务撤销状态：{}", DataConstant.AuditState.CANCEL.getKey());
        runtimeService.deleteProcessInstance(processInstanceId, "用户撤销");
        Map<String, Object> map = new HashMap<>(4);
        map.put(VariablesEnum.businessKey.toString(), businessKey);
        map.put(VariablesEnum.businessCode.toString(), businessCode);
        map.put("flowStatus", DataConstant.AuditState.CANCEL.getKey());
        rabbitTemplate.convertAndSend(RabbitMqConstant.TOPIC_EXCHANGE_WORKFLOW, RabbitMqConstant.TOPIC_WORKFLOW_KEY + businessCode, map);
    }

    @Override
    public FlowInstanceVo startProcessInstanceById(ProcessInstanceCreateRequest request) {
        Assert.notNull(request.getSubmitter(), "请输入提交人");
        Assert.notNull(request.getBusinessKey(), "请输入业务id");
        Assert.notNull(request.getBusinessCode(), "请输入业务类型");
        Assert.notNull(request.getProcessDefinitionId(), "请输入流程定义ID");
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder();
        if(StrUtil.isNotBlank(request.getProcessDefinitionId())){
            processInstanceBuilder.processDefinitionId(request.getProcessDefinitionId());
        }
        if(StrUtil.isNotBlank(request.getBusinessKey())){
            processInstanceBuilder.businessKey(request.getBusinessKey());
        }
        if(StrUtil.isNotBlank(request.getBusinessName())){
            processInstanceBuilder.name(request.getBusinessName());
        }
        processInstanceBuilder.variables(request.getVariables());
        Authentication.setAuthenticatedUserId(request.getSubmitter());
        ProcessInstance processInstance = processInstanceBuilder.start();
        Authentication.setAuthenticatedUserId(null);
        log.info("发起流程成功，流程ID:{}", processInstance.getId());
        this.completeFirstTask(request.getSubmitter(), processInstance.getProcessInstanceId());
        FlowInstanceVo flowInstanceVo = new FlowInstanceVo();
        BeanUtil.copyProperties(processInstance, flowInstanceVo);
        return flowInstanceVo;
    }

    private void completeFirstTask(String submitter, String processInstanceId){
        Task task = taskService.createTaskQuery().taskAssignee(submitter).processInstanceId(processInstanceId).active().singleResult();
        if (task != null) {
            log.info("自动完成提交任务，任务ID:{}", task.getId());
            Comment comment = taskService.addComment(task.getId(), processInstanceId, "任务提交");
            comment.setUserId(submitter);
            taskService.saveComment(comment);
            taskService.complete(task.getId());
        }
    }

    @Override
    public InputStream createImage(String processInstanceId) {
        //1.获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = null;
        List<String> activeActivityIds = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();
        //2.获取所有的历史轨迹线对象
        List<HistoricActivityInstance> historicSquenceFlows = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).activityType(BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW).list();
        historicSquenceFlows.forEach(historicActivityInstance -> highLightedFlows.add(historicActivityInstance.getActivityId()));
        //3.获取流程定义id和高亮的节点id
        if (processInstance != null) {
            //3.1.正在运行的流程实例
            processDefinitionId = processInstance.getProcessDefinitionId();
            activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        } else {
            //3.2.已经结束的流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            //3.3.获取结束节点列表
            List<HistoricActivityInstance> historicEnds = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId).activityType(BpmnXMLConstants.ELEMENT_EVENT_END).list();
            List<String> finalActiveActivityIds = activeActivityIds;
            historicEnds.forEach(historicActivityInstance -> finalActiveActivityIds.add(historicActivityInstance.getActivityId()));
        }
        //4.获取bpmnModel对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //5.生成图片流
        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
        InputStream inputStream = diagramGenerator.generateDiagram(bpmnModel, IMAGE_TYPE, activeActivityIds,
                highLightedFlows, FONT_NAME, FONT_NAME, FONT_NAME,
                null, 1.0, true);
        return inputStream;
    }

    @Override
    public void stopProcessInstanceById(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance != null) {
            //1、添加终止意见
            //2、执行终止
        }
    }

    @Override
    public Page<FlowHistInstanceVo> pageMyStartedProcessInstance(FlowInstanceQuery flowInstanceQuery) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
        historicProcessInstanceQuery.startedBy(SecurityUtil.getUserId());
        if(StrUtil.isNotBlank(flowInstanceQuery.getName())){
            historicProcessInstanceQuery.processInstanceNameLike(flowInstanceQuery.getName());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessKey())){
            historicProcessInstanceQuery.processInstanceBusinessKey(flowInstanceQuery.getBusinessKey());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessCode())){
            historicProcessInstanceQuery.variableValueEquals(VariablesEnum.businessCode.toString(), flowInstanceQuery.getBusinessCode());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessName())){
            historicProcessInstanceQuery.variableValueLike(VariablesEnum.businessName.toString(), flowInstanceQuery.getBusinessName());
        }
        List<HistoricProcessInstance> historicProcessInstanceList = historicProcessInstanceQuery.includeProcessVariables()
                .orderByProcessInstanceStartTime().desc()
                .listPage((flowInstanceQuery.getPageNum() - 1) * flowInstanceQuery.getPageSize(), flowInstanceQuery.getPageSize());
        List<FlowHistInstanceVo> list = new ArrayList<>();
        historicProcessInstanceList.stream().forEach(p -> {
            FlowHistInstanceVo flowHistInstanceVo = new FlowHistInstanceVo();
            BeanUtil.copyProperties(p, flowHistInstanceVo, "variables");
            //放入流程参数
            flowHistInstanceVo.setVariables(p.getProcessVariables());
            list.add(flowHistInstanceVo);
        });
        long count = historicProcessInstanceQuery.count();
        Page<FlowHistInstanceVo> page = new Page<>(flowInstanceQuery.getPageNum(), flowInstanceQuery.getPageSize());
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    @Override
    public Page<FlowHistInstanceVo> pageMyInvolvedProcessInstance(FlowInstanceQuery flowInstanceQuery) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
        historicProcessInstanceQuery.involvedUser(SecurityUtil.getUserId());
        if(StrUtil.isNotBlank(flowInstanceQuery.getName())){
            historicProcessInstanceQuery.processInstanceNameLike(flowInstanceQuery.getName());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessKey())){
            historicProcessInstanceQuery.processInstanceBusinessKey(flowInstanceQuery.getBusinessKey());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessCode())){
            historicProcessInstanceQuery.variableValueEquals(VariablesEnum.businessCode.toString(), flowInstanceQuery.getBusinessCode());
        }
        if(StrUtil.isNotBlank(flowInstanceQuery.getBusinessName())){
            historicProcessInstanceQuery.variableValueLike(VariablesEnum.businessName.toString(), flowInstanceQuery.getBusinessName());
        }
        List<HistoricProcessInstance> historicProcessInstanceList = historicProcessInstanceQuery.includeProcessVariables()
                .orderByProcessInstanceStartTime().desc()
                .listPage((flowInstanceQuery.getPageNum() - 1) * flowInstanceQuery.getPageSize(), flowInstanceQuery.getPageSize());
        List<FlowHistInstanceVo> list = new ArrayList<>();
        historicProcessInstanceList.stream().forEach(p -> {
            FlowHistInstanceVo flowHistInstanceVo = new FlowHistInstanceVo();
            BeanUtil.copyProperties(p, flowHistInstanceVo, "variables");
            //放入流程参数
            flowHistInstanceVo.setVariables(p.getProcessVariables());
            list.add(flowHistInstanceVo);
        });
        long count = historicProcessInstanceQuery.count();
        Page<FlowHistInstanceVo> page = new Page<>(flowInstanceQuery.getPageNum(), flowInstanceQuery.getPageSize());
        page.setRecords(list);
        page.setTotal(count);
        return page;
    }

    @Override
    public void getProcessInstanceComments(String processInstanceId) {
        List<Comment> processInstanceComments = taskService.getProcessInstanceComments(processInstanceId);
    }
}
