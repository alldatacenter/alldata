package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.service.DeleteClusterService;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DeleteServiceDBDataTask extends BaseCloudeonTask {

    @Override
    public void internalExecute() {
        Integer serviceInstanceId = taskParam.getServiceInstanceId();
        DeleteClusterService deleteClusterService = SpringUtil.getBean(DeleteClusterService.class);
        log.info("开始删除 {} 服务相关的表数据....", taskParam.getServiceInstanceName());
        deleteClusterService.deleteOneService(serviceInstanceId);
    }
}
