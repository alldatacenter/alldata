package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import lombok.NoArgsConstructor;
import org.apache.sshd.client.session.ClientSession;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.dao.StackServiceRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.dromara.cloudeon.entity.StackServiceEntity;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.SshUtils;

import java.io.IOException;
import java.util.List;

@NoArgsConstructor
public class InitDSTablesTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ServiceRoleInstanceRepository roleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        TaskParam taskParam = getTaskParam();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(serviceInstanceEntity.getStackServiceId()).get();
        String serviceName = serviceInstanceEntity.getServiceName();
        String cmd = String.format("sudo docker  run --net=host -v /opt/edp/%s/conf:/opt/edp/%s/conf -v /opt/edp/%s/log:/opt/edp/%s/log   -v /opt/edp/%s/data:/opt/edp/%s/data  %s sh -c \"  /opt/edp/%s/conf/init-dolphinscheduler-db.sh \"   ",
                serviceName,serviceName, serviceName,serviceName,serviceName,serviceName,stackServiceEntity.getDockerImage(),serviceName);

        // 选择apiserver所在节点执行
        List<ServiceRoleInstanceEntity> roleInstanceEntities = roleInstanceRepository.findByServiceInstanceIdAndServiceRoleName(serviceInstanceId,"DS_API_SERVER");
        ServiceRoleInstanceEntity firstNamenode = roleInstanceEntities.get(0);
        Integer nodeId = firstNamenode.getNodeId();
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findById(nodeId).get();
        String ip = nodeEntity.getIp();
        log.info("在节点"+ip+"上执行命令:" + cmd);
        ClientSession clientSession = sshPoolService.openSession(ip, nodeEntity.getSshPort(), nodeEntity.getSshUser(), nodeEntity.getSshPassword());
        try {
            String result = SshUtils.execCmdWithResult(clientSession, cmd);
            log.info(result);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        sshPoolService.returnSession(clientSession,ip);


    }
}
