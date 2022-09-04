package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.common.enums.DeployAppAttrTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.lib.helper.DagHelper;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * App 部署工单 State 处理 Action - WAITING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("WaitingDeployAppStateAction")
public class WaitingDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    public static Integer runTimeout = 3600;

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.WAITING;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DagHelper dagHelper;

    @Autowired
    private DagInstNewService dagInstNewService;

    @Autowired
    private DeployAppService deployAppService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new DeployAppStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        Long deployAppId = order.getId();
        Long appPackageId = order.getAppPackageId();
        Long dagInstId = order.getDeployProcessId();

        // 检查 DAG 部署状态
        DagInstDO dagInst = dagInstNewService.getDagInstById(dagInstId);
        DagInstStatus dagStatus = DagInstStatus.valueOf(dagInst.getStatus());
        String logDetail = String.format("|deployAppId=%d|appPackageId=%d|dagInstId=%d|status=%s|" +
                "creator=%s", deployAppId, appPackageId, dagInstId, dagStatus.toString(), order.getDeployCreator());
        log.info("check deploy app dag status" + logDetail);
        if (!dagStatus.isEnd()) {
            return;
        }

        // 当 DAG 运行已经到达终态时，更新 global params
        String globalParamStr = dagInst.fetchGlobalParamsJson().toJSONString();
        deployAppService.updateAttr(order.getId(), DeployAppAttrTypeEnum.GLOBAL_PARAMS.toString(), globalParamStr);
        log.info("final global parameters has put into deploy app attr|deployAppId={}|appPackageId={}|dagInstId={}|" +
                "globalParams={}", deployAppId, appPackageId, dagInstId, globalParamStr);

        // 根据 DAG 状态发送不同的事件
        switch (dagStatus) {
            case SUCCESS: {
                deployAppService.update(order);
                publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.ALL_SUCCEED, order.getId()));
                break;
            }
            case STOPPED:
            case EXCEPTION: {
                JSONArray errorMessages = new JSONArray();
                try {
                    dagHelper.collectExceptionMessages(dagInstId, errorMessages);
                } catch (Exception e) {
                    throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                            String.format("cannot collect exception messages with specified deploy app dag inst|" +
                                    "deployAppId=%d|dagInstId=%d", deployAppId, dagInstId), e);
                }
                order.setDeployErrorMessage(errorMessages.toJSONString());
                deployAppService.update(order);
                publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.PARTIAL_FAILED, order.getId()));
                break;
            }
            default:
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        "invalid dag status found when check deploy app dag status" + logDetail);
        }
    }
}
