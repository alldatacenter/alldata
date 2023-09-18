package org.dromara.cloudeon.processor;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.SshUtils;
import lombok.NoArgsConstructor;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

@NoArgsConstructor
public class InstallTask extends BaseCloudeonTask {


    @Override
    public void internalExecute() {
        // 查询服务实例需要创建的目录
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(taskParam.getServiceInstanceId()).get();
        String persistencePaths = serviceInstanceEntity.getPersistencePaths();
        String[] paths = persistencePaths.split(",");

        ClusterNodeEntity nodeEntity = clusterNodeRepository.findByHostname(taskParam.getHostName());
        ClientSession clientSession = sshPoolService.openSession(nodeEntity.getIp(), nodeEntity.getSshPort(), nodeEntity.getSshUser(), nodeEntity.getSshPassword());
        SftpFileSystem sftp;
        sftp =sshPoolService.openSftpFileSystem(nodeEntity.getIp());
        // 查询节点
        Arrays.stream(paths).forEach(new Consumer<String>() {
            @Override
            public void accept(String path) {
                log.info("节点：" + taskParam.getHostName() + " 上创建目录：" + path);
                // ssh执行创建目录
                SshUtils.createDir(clientSession, path, sftp);
                try {
                    String command = "chmod 777 -R " + path;
                    log.info("执行远程命令：" + command);
                    SshUtils.execCmdWithResult(clientSession, command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("成功在节点：" + taskParam.getHostName() + " 上创建目录：" + path);

            }
        });
        sshPoolService.returnSession(clientSession,nodeEntity.getIp());
        sshPoolService.returnSftp(sftp,nodeEntity.getIp());


        // todo 服务日志采集的相关执行

    }
}
