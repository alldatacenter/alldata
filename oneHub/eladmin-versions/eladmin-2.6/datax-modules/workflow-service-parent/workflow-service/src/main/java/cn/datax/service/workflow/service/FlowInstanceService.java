package cn.datax.service.workflow.service;

import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.query.FlowInstanceQuery;
import cn.datax.service.workflow.api.vo.FlowHistInstanceVo;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.InputStream;

public interface FlowInstanceService {

    /**
     * 分页查询运行中的流程实例
     * @param flowInstanceQuery
     * @return
     */
    Page<FlowInstanceVo> pageRunning(FlowInstanceQuery flowInstanceQuery);

    /**
     * 激活流程实例
     * @param processInstanceId
     */
    void activateProcessInstanceById(String processInstanceId);

    /**
     * 挂起流程实例
     * @param processInstanceId
     */
    void suspendProcessInstanceById(String processInstanceId);

    /**
     * 删除流程实例
     * @param processInstanceId
     */
    void deleteProcessInstance(String processInstanceId);

    /**
     * 启动流程实例
     * @param request
     * @return
     */
    FlowInstanceVo startProcessInstanceById(ProcessInstanceCreateRequest request);

    /**
     * 获取流程图图片
     * @param processInstanceId
     */
    InputStream createImage(String processInstanceId);

    /**
     * 终止流程
     * @param processInstanceId
     */
    void stopProcessInstanceById(String processInstanceId);

    /**
     * 本人发起的流程实例
     * @param flowInstanceQuery
     * @return
     */
    Page<FlowHistInstanceVo> pageMyStartedProcessInstance(FlowInstanceQuery flowInstanceQuery);

    /**
     * 本人参与的流程实例
     * @param flowInstanceQuery
     * @return
     */
    Page<FlowHistInstanceVo> pageMyInvolvedProcessInstance(FlowInstanceQuery flowInstanceQuery);

    /**
     * 获取审批意见
     * @param processInstanceId
     */
    void getProcessInstanceComments(String processInstanceId);
}
