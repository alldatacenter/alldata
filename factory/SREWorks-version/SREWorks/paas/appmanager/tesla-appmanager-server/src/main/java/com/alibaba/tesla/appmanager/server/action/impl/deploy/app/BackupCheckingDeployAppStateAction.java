package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * App 部署工单 State 处理 Action - BACKUP_CHECKING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("BackupCheckingDeployAppStateAction")
public class BackupCheckingDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.BACKUP_CHECKING;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new DeployAppStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 扩展属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.APP_PACKAGE_EXIST, order.getId()));
    }
}
