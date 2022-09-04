package com.alibaba.tesla.appmanager.workflow.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.DeleteWorkflowSnapshotReq;
import com.alibaba.tesla.appmanager.domain.req.UpdateWorkflowSnapshotReq;
import com.alibaba.tesla.appmanager.workflow.repository.WorkflowSnapshotRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowSnapshotQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * Workflow 快照服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class WorkflowSnapshotServiceImpl implements WorkflowSnapshotService {

    @Autowired
    private WorkflowSnapshotRepository workflowSnapshotRepository;

    /**
     * 根据 WorkflowInstanceID 获取对应的 WorkflowSnapshot 对象
     *
     * @param workflowSnapshotId Workflow 快照 ID
     * @return WorkflowSnapshot 对象，不存在则返回 null
     */
    @Override
    public WorkflowSnapshotDO get(Long workflowSnapshotId) {
        WorkflowSnapshotQueryCondition condition = WorkflowSnapshotQueryCondition.builder()
                .snapshotId(workflowSnapshotId)
                .build();
        return workflowSnapshotRepository.getByCondition(condition);
    }

    /**
     * 根据条件过滤 Workflow 任务列表
     *
     * @param condition 过滤条件
     * @return List of WorkflowSnapshot
     */
    @Override
    public Pagination<WorkflowSnapshotDO> list(WorkflowSnapshotQueryCondition condition) {
        List<WorkflowSnapshotDO> result = workflowSnapshotRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 更新一个 Workflow 快照
     *
     * @param request 更新 Workflow 快照请求
     * @return 更新后的 WorkflowSnapshot 对象
     */
    @Override
    public WorkflowSnapshotDO update(UpdateWorkflowSnapshotReq request) {
        WorkflowSnapshotQueryCondition condition = WorkflowSnapshotQueryCondition.builder()
                .instanceId(request.getWorkflowInstanceId())
                .taskId(request.getWorkflowTaskId())
                .build();
        Pagination<WorkflowSnapshotDO> snapshots = list(condition);
        if (snapshots.getItems().size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("multiple workflow snapshots found|condition=%s",
                            JSONObject.toJSONString(condition)));
        }
        WorkflowSnapshotDO record;
        if (snapshots.isEmpty()) {
            record = WorkflowSnapshotDO.builder()
                    .workflowInstanceId(request.getWorkflowInstanceId())
                    .workflowTaskId(request.getWorkflowTaskId())
                    .snapshotContext(JSONObject.toJSONString(request.getContext()))
                    .snapshotTask(SchemaUtil.toYamlMapStr(request.getConfiguration()))
                    .snapshotWorkflow(null)
                    .build();
            workflowSnapshotRepository.insert(record);
        } else {
            record = snapshots.getItems().get(0);
            record.setSnapshotContext(JSONObject.toJSONString(request.getContext()));
            record.setSnapshotTask(SchemaUtil.toYamlMapStr(request.getConfiguration()));
            record.setSnapshotWorkflow(null);
            workflowSnapshotRepository.updateByCondition(record, condition);
        }
        return get(record.getId());
    }

    /**
     * 根据条件删除 Workflow 快照
     *
     * @param request 删除 Workflow 快照请求
     * @return 删除数量
     */
    @Override
    public int delete(DeleteWorkflowSnapshotReq request) {
        if (request.getWorkflowInstanceId() == null && request.getWorkflowTaskId() == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty delete workflow snapshot request");
        }
        WorkflowSnapshotQueryCondition condition = WorkflowSnapshotQueryCondition.builder()
                .instanceId(request.getWorkflowInstanceId())
                .taskId(request.getWorkflowTaskId())
                .build();
        return workflowSnapshotRepository.deleteByCondition(condition);
    }
}
