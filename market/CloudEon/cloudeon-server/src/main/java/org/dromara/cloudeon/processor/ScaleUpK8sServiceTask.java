package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.dao.StackServiceRoleRepository;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.dromara.cloudeon.entity.StackServiceRoleEntity;
import org.dromara.cloudeon.enums.ServiceRoleState;
import org.dromara.cloudeon.service.KubeService;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ScaleUpK8sServiceTask extends BaseCloudeonTask {
    @Override
    public void internalExecute() {
        StackServiceRoleRepository stackServiceRoleRepository = SpringUtil.getBean(StackServiceRoleRepository.class);
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        ServiceRoleInstanceRepository serviceRoleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        KubeService kubeService = SpringUtil.getBean(KubeService.class);

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(taskParam.getServiceInstanceId()).get();
        String serviceName = serviceInstanceEntity.getServiceName();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();


        // 查询框架服务角色名获取deployment名字
        String roleName = taskParam.getRoleName();
        StackServiceRoleEntity stackServiceRoleEntity = stackServiceRoleRepository.findByServiceIdAndName(taskParam.getStackServiceId(), roleName);
        String roleFullName = stackServiceRoleEntity.getRoleFullName();
        String serviceInstanceName = taskParam.getServiceInstanceName();
        String deploymentName = String.format("%s-%s", roleFullName, serviceInstanceName);
        String tag = roleFullName + "-" + serviceName.toLowerCase();

        try (KubernetesClient client = kubeService.getKubeClient(serviceInstanceEntity.getClusterId());) {
            RollableScalableResource<Deployment> resource = client.apps().deployments().inNamespace("default").withName(deploymentName);
            Integer replicas = resource.get().getStatus().getReplicas();
            if (replicas == null) {
                replicas = 0;
            }
            log.info("当前deployment: {} Replicas: {}", deploymentName, replicas);
            int count = replicas + 1;
            log.info("scale up deployment 为: " + count);
            resource.scale(count);

            // 根据label查找启动了角色实例的hostname
            client.nodes().withLabel(tag).list().getItems().forEach(node->{
                String hostname = node.getStatus().getAddresses().get(1).getAddress();
                // 根据hostname查询节点
                Integer nodeId = clusterNodeRepository.findByHostname(hostname).getId();
                // 根据节点id更新角色状态
                ServiceRoleInstanceEntity roleInstanceEntity = serviceRoleInstanceRepository.findByServiceInstanceIdAndNodeIdAndServiceRoleName(serviceInstanceId, nodeId,roleName);
                roleInstanceEntity.setServiceRoleState(ServiceRoleState.ROLE_STARTED);
                serviceRoleInstanceRepository.save(roleInstanceEntity);
            });


        }


    }
}
