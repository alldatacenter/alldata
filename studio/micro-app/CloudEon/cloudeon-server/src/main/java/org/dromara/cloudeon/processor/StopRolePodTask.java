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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class StopRolePodTask extends BaseCloudeonTask {
    @Override
    public void internalExecute() {
        StackServiceRoleRepository stackServiceRoleRepository = SpringUtil.getBean(StackServiceRoleRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        ServiceRoleInstanceRepository serviceRoleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        KubeService kubeService = SpringUtil.getBean(KubeService.class);

        String serviceInstanceName = taskParam.getServiceInstanceName();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();
        String hostName = taskParam.getHostName();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(taskParam.getServiceInstanceId()).get();

        // 查询框架服务角色名获取模板名
        String roleName = taskParam.getRoleName();
        StackServiceRoleEntity stackServiceRoleEntity = stackServiceRoleRepository.findByServiceIdAndName(taskParam.getStackServiceId(), roleName);
        String roleFullName = stackServiceRoleEntity.getRoleFullName();
        String podLabel = String.format("app=%s-%s", roleFullName, serviceInstanceName);
        try (KubernetesClient client = kubeService.getKubeClient(serviceInstanceEntity.getClusterId());) {

            List<Pod> pods = client.pods().inNamespace("default").withLabel(podLabel).list().getItems();
            for (Pod pod : pods) {
                String nodeName = pod.getSpec().getNodeName();
                if (nodeName != null && nodeName.equals(hostName)) {
                    // do something with the pod
                    String podName = pod.getMetadata().getName();
                    log.info("删除节点 {} 上的pod: {}", hostName, podName);
                    client.pods().withName(podName).delete();
                }
            }
        }
        // 根据hostname查询节点
        Integer nodeId = clusterNodeRepository.findByHostname(hostName).getId();

        // 根据节点id更新角色状态
        ServiceRoleInstanceEntity roleInstanceEntity = serviceRoleInstanceRepository.findByServiceInstanceIdAndNodeIdAndServiceRoleName(serviceInstanceId, nodeId,roleName);
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.ROLE_STOPPED);
        serviceRoleInstanceRepository.save(roleInstanceEntity);
    }
}
