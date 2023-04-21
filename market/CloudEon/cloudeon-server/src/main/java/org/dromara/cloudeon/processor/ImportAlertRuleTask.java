package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import lombok.NoArgsConstructor;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.service.AlertService;

@NoArgsConstructor
public class ImportAlertRuleTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        Integer serviceInstanceId = taskParam.getServiceInstanceId();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
        AlertService alertService = SpringUtil.getBean(AlertService.class);
        alertService.upgradeMonitorAlertRule(serviceInstanceEntity.getClusterId(), log);

    }
}
