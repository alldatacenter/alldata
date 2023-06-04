package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.SshUtils;
import lombok.NoArgsConstructor;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;

@NoArgsConstructor
public class DeleteServiceDataDirTask extends BaseCloudeonTask {

    @Override
    public void internalExecute() {
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        String serviceInstanceName = taskParam.getServiceInstanceName();
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findByHostname(taskParam.getHostName());
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        try  {
            ClientSession clientSession = sshPoolService.openSession(nodeEntity.getIp(), nodeEntity.getSshPort(), nodeEntity.getSshUser(), nodeEntity.getSshPassword());
            String remoteDataDirPath = "/opt/edp/" + serviceInstanceName;
            String command = "rm -rf " + remoteDataDirPath;
            log.info("执行远程命令：" + command);
            SshUtils.execCmdWithResult(clientSession, command);
            sshPoolService.returnSession(clientSession,nodeEntity.getIp());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("打开sftp失败：" + e);
        }

    }
}
