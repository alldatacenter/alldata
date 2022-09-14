package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DagTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentEventEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.addon.task.event.AddonInstanceTaskEvent;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;
import com.alibaba.tesla.dag.api.DagInstApiService;
import com.alibaba.tesla.dag.model.domain.TcDagInstNode;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.event.DagInstStatusEvent;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DAG 流程事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class DagInstListener implements ApplicationListener<DagInstStatusEvent> {

    private static final String LOG_PRE = "[event.dag-inst] ";

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DeployAppService deployAppService;

    @Autowired
    private DeployComponentService deployComponentService;

    @Autowired
    private DagInstApiService dagInstApiService;

    @Autowired
    private DagInstNewService dagInstNewService;

    @Autowired
    private AddonInstanceTaskRepository addonInstanceTaskRepository;

    @Override
    public void onApplicationEvent(DagInstStatusEvent event) {
        long dagInstId;
        try {
            dagInstId = dagInstApiService.getDagInstTopFatherDagInst(event.dagInstId).getId();
        } catch (Exception e) {
            log.error(LOG_PRE + "cannot find top father of dagInstId {}|exception={}",
                    event.dagInstId, ExceptionUtils.getStackTrace(e));
            return;
        }
        DagInstDO dagInst = dagInstNewService.getDagInstById(dagInstId);
        JSONObject globalVariable = JSONObject.parseObject(dagInst.getGlobalVariable());
        String dagType = globalVariable.getString(DefaultConstant.DAG_TYPE);
        DagTypeEnum dagTypeEnum = Enums.getIfPresent(DagTypeEnum.class, dagType).or(DagTypeEnum.UNKNOWN);
        String logSuffix = String.format("dagType=%s|dagName=%s|dagInstId=%d", dagTypeEnum,
                dagInst.fetchDagDO().getName(), dagInstId);

        log.info(LOG_PRE + "status transitioned from {} to {}|{}", event.from, event.to, logSuffix);
        // 终态时进行数据打印及事件发送
        if (event.to.equals(DagInstStatus.EXCEPTION)) {
            List<String> errorMessages = new ArrayList<>();
            try {
                showExceptionNode(dagInstId, errorMessages);
            } catch (Exception e) {
                log.error(LOG_PRE + "show dag exception detail failed|{}|exception={}",
                        logSuffix, ExceptionUtils.getStackTrace(e));
            }
        }

        // 根据 DAG 类型进行事件处理
        switch (dagTypeEnum) {
            case DEPLOY_APP: {
                Pagination<DeployAppBO> deployAppBOList = deployAppService.list(
                        DeployAppQueryCondition.builder().deployProcessId(dagInstId).build(), true
                );
                if (CollectionUtils.isEmpty(deployAppBOList.getItems()) || deployAppBOList.getItems().size() != 1) {
                    log.info(LOG_PRE + "cannot find deploy app record in db, skip|{}", logSuffix);
                    return;
                }
                DeployAppDO order = deployAppBOList.getItems().get(0).getOrder();
                publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.TRIGGER_UPDATE, order.getId()));
                break;
            }
            case DEPLOY_COMPONENT: {
                List<DeployComponentBO> subOrderList = deployComponentService.list(
                        DeployComponentQueryCondition.builder().deployProcessId(dagInstId).build(), false
                );
                if (CollectionUtils.isEmpty(subOrderList) || subOrderList.size() != 1) {
                    log.error(LOG_PRE + "cannot find deploy component record in db, the event will be lost|{}",
                            logSuffix);
                    return;
                }
                DeployComponentDO subOrder = subOrderList.get(0).getSubOrder();
                publisher.publishEvent(new DeployComponentEvent(this,
                        DeployComponentEventEnum.TRIGGER_UPDATE, subOrder.getId()));
                break;
            }
            case APPLY_ADDON_INSTANCE_TASK: {
                AddonInstanceTaskQueryCondition condition = AddonInstanceTaskQueryCondition.builder()
                        .taskProcessId(dagInstId)
                        .build();
                List<AddonInstanceTaskDO> orders = addonInstanceTaskRepository.selectByCondition(condition);
                if (CollectionUtils.isEmpty(orders) || orders.size() != 1) {
                    log.error(LOG_PRE + "cannot find addon instance task record in db, the event will be lost|{}",
                            logSuffix);
                    return;
                }
                AddonInstanceTaskDO order = orders.get(0);
                publisher.publishEvent(
                        new AddonInstanceTaskEvent(this, order.getId(), AddonInstanceTaskEventEnum.TRIGGER_UPDATE)
                );
                break;
            }
            case PACK_APP_PACKAGE:
            case UNPACK_APP_PACKAGE:
                break;
            default:
                log.error(LOG_PRE + "invalid dagTypeEnum {}|{}", dagTypeEnum, logSuffix);
                break;
        }
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
            if (node.getStatus().equals("EXCEPTION")) {
                switch (node.type()) {
                    case DAG:
                        showExceptionNode(node.getSubDagInstId(), errorMessages);
                        break;
                    case NODE:
                        String nodeId = node.getNodeId();
                        String details = node.getStatusDetail();
                        errorMessages.add(String.format("nodeId: %s, details: %s", nodeId, details));
                        log.warn(LOG_PRE + "dag exception detail|dagNodeId={}|detail={}", nodeId, details);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
