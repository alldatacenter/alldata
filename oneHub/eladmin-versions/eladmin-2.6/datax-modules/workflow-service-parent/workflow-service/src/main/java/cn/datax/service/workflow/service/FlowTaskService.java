package cn.datax.service.workflow.service;

import cn.datax.service.workflow.api.dto.TaskRequest;
import cn.datax.service.workflow.api.query.FlowTaskQuery;
import cn.datax.service.workflow.api.vo.FlowHistTaskVo;
import cn.datax.service.workflow.api.vo.FlowTaskVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FlowTaskService {

    /**
     * 分页查询待办任务
     * @param flowTaskQuery
     * @return
     */
    Page<FlowTaskVo> pageTodo(FlowTaskQuery flowTaskQuery);

    /**
     * 分页查询已办任务
     * @param flowTaskQuery
     * @return
     */
    Page<FlowHistTaskVo> pageDone(FlowTaskQuery flowTaskQuery);

    /**
     * 执行任务
     * 执行任务类型：claim签收 unclaim反签收 complete完成 delegate任务委派 resolve任务归还 assignee任务转办
     * @param request
     */
    void execute(TaskRequest request);
}
