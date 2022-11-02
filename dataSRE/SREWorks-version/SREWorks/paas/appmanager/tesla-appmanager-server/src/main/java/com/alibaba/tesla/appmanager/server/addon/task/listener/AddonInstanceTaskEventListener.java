package com.alibaba.tesla.appmanager.server.addon.task.listener;

import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskStatusEnum;
import com.alibaba.tesla.appmanager.server.addon.task.event.AddonInstanceTaskEvent;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.dag.api.DagInstApiService;
import com.alibaba.tesla.dag.model.domain.TcDagInstNode;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * App 部署单开始事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class AddonInstanceTaskEventListener implements ApplicationListener<AddonInstanceTaskEvent> {

    @Autowired
    private AddonInstanceTaskRepository addonInstanceTaskRepository;

    @Autowired
    private DagInstNewService dagInstNewService;

    @Autowired
    private DagInstApiService dagInstApiService;

    /**
     * 处理 Addon Instance 任务处理事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(AddonInstanceTaskEvent event) {
        AddonInstanceTaskDO task = addonInstanceTaskRepository.selectByPrimaryKey(event.getTaskId());
        assert task != null;
        AddonInstanceTaskStatusEnum fromStatus = Enums.getIfPresent(AddonInstanceTaskStatusEnum.class,
                task.getTaskStatus()).orNull();
        assert fromStatus != null;
        if (fromStatus.isEnd()) {
            return;
        }

        AddonInstanceTaskEventEnum currentEvent = event.getEventType();
        String logPre = String.format("action=event.addonInstanceTask.%s|message=", currentEvent.toString());
        DagInstDO dagInst = dagInstNewService.getDagInstById(task.getTaskProcessId());
        DagInstStatus dagStatus = Enums.getIfPresent(DagInstStatus.class, dagInst.getStatus()).orNull();
        assert dagStatus != null;
        if (!dagStatus.isEnd()) {
            log.debug(logPre + "not final state event, ignore|taskId={}|dagInstId={}|status={}",
                    event.getTaskId(), task.getTaskProcessId(), dagStatus.toString());
            return;
        }

        // 根据状态更新当前数据
        switch (dagStatus) {
            case SUCCESS: {
                task.setTaskStatus(AddonInstanceTaskStatusEnum.SUCCESS.toString());
                break;
            }
            case EXCEPTION:
            case STOPPED: {
                List<String> errorMessages = new ArrayList<>();
                try {
                    showExceptionNode(task.getTaskProcessId(), errorMessages);
                } catch (Exception e) {
                    log.error("show dag exception detail failed|exception={}", ExceptionUtils.getStackTrace(e));
                }
                task.setTaskErrorMessage(String.join("\n", errorMessages));
                task.setTaskStatus(AddonInstanceTaskStatusEnum.WAIT_FOR_OP.toString());
                break;
            }
            default: {
                String errorMessage = String.format("cannot recognize dag inst status %s|taskId=%d|dagInstId=%d",
                        dagStatus.toString(), event.getTaskId(), task.getTaskProcessId());
                task.setTaskErrorMessage(errorMessage);
                task.setTaskStatus(AddonInstanceTaskStatusEnum.EXCEPTION.toString());
                log.error(logPre + errorMessage);
                break;
            }
        }
        addonInstanceTaskRepository.updateByPrimaryKey(task);
        log.info(logPre + "status has changed|taskId={}|fromStatus={}|toStatus={}",
                event.getTaskId(), fromStatus.toString(), task.getTaskStatus());
    }

    /**
     * 打印 DAG Inst 中的失败日志
     *
     * @param dagInstId     DAG 实例 ID
     * @param errorMessages 需要填充的错误信息列表
     */
    private void showExceptionNode(Long dagInstId, List<String> errorMessages) throws Exception {
        List<TcDagInstNode> nodeDetails = dagInstApiService.nodes(dagInstId);
        for (TcDagInstNode node : nodeDetails) {
            if (DagInstStatus.EXCEPTION.toString().equals(node.getStatus())) {
                switch (node.type()) {
                    case DAG:
                        showExceptionNode(node.getSubDagInstId(), errorMessages);
                        break;
                    case NODE:
                        String nodeId = node.getNodeId();
                        String details = node.getStatusDetail();
                        errorMessages.add(String.format("nodeId: %s, details: %s", nodeId, details));
                        log.warn("dag exception detail|dagNodeId={}|detail={}", nodeId, details);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
