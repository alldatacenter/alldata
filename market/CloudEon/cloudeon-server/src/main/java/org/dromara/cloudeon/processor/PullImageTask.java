package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.StackServiceRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.StackServiceEntity;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.SshUtils;
import lombok.NoArgsConstructor;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;

@NoArgsConstructor
public class PullImageTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        // 查询安装服务的镜像
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(taskParam.getStackServiceId()).get();
        String dockerImage = stackServiceEntity.getDockerImage();
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findByHostname(taskParam.getHostName());
        log.info("节点：" + taskParam.getHostName() + " 上拉取镜像：" + dockerImage);
        String command = "";
        String runtimeContainer = nodeEntity.getRuntimeContainer();
        // 兼容containerd
        if (runtimeContainer.startsWith("docker")) {
            command=  "docker pull " + dockerImage;
        } else if (runtimeContainer.startsWith("containerd")) {
            command=  "ctr image pull " + dockerImage;
        }
        // ssh执行拉镜像
        ClientSession clientSession = sshPoolService.openSession(nodeEntity.getIp(), nodeEntity.getSshPort(), nodeEntity.getSshUser(), nodeEntity.getSshPassword());
        try {
            log.info("节点：" + taskParam.getHostName() + " 上执行命令：" + command);
            String result = SshUtils.execCmdWithResult(clientSession, command);
            log.info(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("成功在节点：" + taskParam.getHostName() + " 上拉取镜像：" + dockerImage);
        sshPoolService.returnSession(clientSession,(nodeEntity.getIp()));




    }
}
