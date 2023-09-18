package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.dao.StackServiceRoleRepository;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.entity.StackServiceRoleEntity;
import org.dromara.cloudeon.service.KubeService;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ScaleDownK8sServiceTask extends BaseCloudeonTask {
    @Override
    public void internalExecute() {
        StackServiceRoleRepository stackServiceRoleRepository = SpringUtil.getBean(StackServiceRoleRepository.class);
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        KubeService kubeService = SpringUtil.getBean(KubeService.class);
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(taskParam.getServiceInstanceId()).get();

        // 查询框架服务角色名获取deployment名字
        String roleName = taskParam.getRoleName();
        StackServiceRoleEntity stackServiceRoleEntity = stackServiceRoleRepository.findByServiceIdAndName(taskParam.getStackServiceId(), roleName);
        String roleFullName = stackServiceRoleEntity.getRoleFullName();
        String serviceInstanceName = taskParam.getServiceInstanceName();
        String deploymentName = String.format("%s-%s", roleFullName, serviceInstanceName);

        try (KubernetesClient client = kubeService.getKubeClient(serviceInstanceEntity.getClusterId());) {
            RollableScalableResource<Deployment> resource = client.apps().deployments().inNamespace("default").withName(deploymentName);
            Integer replicas = resource.get().getStatus().getReplicas();
            log.info("当前deployment: {} Replicas: {}", deploymentName, replicas);
            int count = replicas - 1;
            log.info("scale down deployment 为: " + count);
            resource.scale(count);
        }


    }
}
