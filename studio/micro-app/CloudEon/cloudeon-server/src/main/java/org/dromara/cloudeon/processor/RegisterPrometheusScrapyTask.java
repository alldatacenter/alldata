package org.dromara.cloudeon.processor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.dromara.cloudeon.config.CloudeonConfigProp;
import org.dromara.cloudeon.dao.*;
import org.dromara.cloudeon.dto.RoleNodeInfo;
import org.dromara.cloudeon.entity.*;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.SshUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dromara.cloudeon.utils.Constant.MONITOR_SERVICE_NAME;

@NoArgsConstructor
public class RegisterPrometheusScrapyTask extends BaseCloudeonTask {
    private static final String PROMETHEUS_DIR = "prometheus";
    private static final String PROMETHEUS_OUT_DIR = "prometheus-resource";

    @Override
    public void internalExecute() {
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ServiceInstanceConfigRepository configRepository = SpringUtil.getBean(ServiceInstanceConfigRepository.class);
        ServiceRoleInstanceRepository roleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        CloudeonConfigProp cloudeonConfigProp = SpringUtil.getBean(CloudeonConfigProp.class);
        TaskParam taskParam = getTaskParam();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();

        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(serviceInstanceEntity.getStackServiceId()).get();
        String stackCode = stackServiceEntity.getStackCode();
        String stackServiceName = stackServiceEntity.getName().toLowerCase();

        String renderDir = cloudeonConfigProp.getStackLoadPath() + File.separator + stackCode + File.separator + stackServiceName + File.separator + PROMETHEUS_DIR;
        String workHome = cloudeonConfigProp.getWorkHome();
        File renderDirFile = new File(renderDir);

        // 查出当前集群已安装的Monitor服务
        ServiceInstanceEntity monitorServiceInstance = serviceInstanceRepository.findEntityByClusterIdAndStackServiceName(serviceInstanceEntity.getClusterId(), MONITOR_SERVICE_NAME);

        // 如果不存在prometheus目录则跳过
        // todo monitor服务没安装则跳过
        if (!renderDirFile.exists() || monitorServiceInstance == null) {
            log.info("扫描安装包内不存在prometheus目录，退出当前任务....");
            return;
        }

        String outputConfPath = workHome + File.separator + PROMETHEUS_OUT_DIR + File.separator + serviceInstanceEntity.getServiceName();
        if (!FileUtil.exist(outputConfPath)) {
            log.info("目录{}不存在，创建目录...", outputConfPath);
            FileUtil.mkdir(outputConfPath);
        }

        log.info("开始生成prometheus采集的配置文件....");
        // 用freemarker在本地生成服务实例的所有配置文件
        // 构建数据模型
        Map<String, Object> dataModel = new HashMap<>();
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
        // 查询服务实例所有配置项
        List<ServiceInstanceConfigEntity> allConfigEntityList = configRepository.findByServiceInstanceId(serviceInstanceId);
        // 查出所有角色
        List<ServiceRoleInstanceEntity> roleInstanceEntities = roleInstanceRepository.findByServiceInstanceId(serviceInstanceId);
        Map<String, List<RoleNodeInfo>> serviceRoles = getServiceRoles(roleInstanceEntities, clusterNodeRepository);
        dataModel.put("conf", allConfigEntityList.stream().collect(Collectors.toMap(ServiceInstanceConfigEntity::getName, ServiceInstanceConfigEntity::getValue)));
        dataModel.put("serviceRoles", serviceRoles);

        Arrays.stream(renderDirFile.listFiles()).forEach(file -> {
            // 设置加载的目录
            try {
                log.info("加载prometheus模板目录：" + renderDir);
                config.setDirectoryForTemplateLoading(renderDirFile);
                String fileName = file.getName();
                String outPutFile = outputConfPath + File.separator + StringUtils.substringBeforeLast(fileName, ".ftl");

                Template template = config.getTemplate(fileName);
                FileWriter out = new FileWriter(outPutFile);
                template.process(dataModel, out);
                log.info("完成prometheus配置文件生成：" + outPutFile);

            } catch (IOException | TemplateException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        log.info("上传prometheus采集的配置文件到prometheus配置目录....");
        // 找出promethus安装节点
        ServiceRoleInstanceEntity monitorPrometheus = roleInstanceRepository.findByServiceInstanceIdAndServiceRoleName(monitorServiceInstance.getId(), "PROMETHEUS").get(0);
        Integer monitorPrometheusNodeId = monitorPrometheus.getNodeId();
        ClusterNodeEntity prometheusNodeEntity = clusterNodeRepository.findById(monitorPrometheusNodeId).get();
        ClientSession clientSession =sshPoolService.openSession(prometheusNodeEntity.getIp(), prometheusNodeEntity.getSshPort(), prometheusNodeEntity.getSshUser(), prometheusNodeEntity.getSshPassword());
        SftpFileSystem sftp;
        sftp = sshPoolService.openSftpFileSystem(prometheusNodeEntity.getIp());
        String remoteConfDirPath = "/opt/edp/" + monitorServiceInstance.getServiceName() +"/conf/discovery_configs/";
        log.info("拷贝本地配置目录：" + outputConfPath + " 到节点" + prometheusNodeEntity.getHostname() + "的：" + remoteConfDirPath);
        try {
            SshUtils.uploadDirectory(sftp,new File(outputConfPath),remoteConfDirPath );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("拷贝文件上远程服务器失败：" + e);
        }
        log.info("成功拷贝本地配置目录：" + outputConfPath + " 到节点" + prometheusNodeEntity.getHostname() + "的：" + remoteConfDirPath);
        sshPoolService.returnSession(clientSession,prometheusNodeEntity.getIp());
        sshPoolService.returnSftp(sftp,prometheusNodeEntity.getIp());
    }

    private Map<String, List<RoleNodeInfo>> getServiceRoles(List<ServiceRoleInstanceEntity> roleInstanceEntities, ClusterNodeRepository clusterNodeRepository) {
        Map<String, List<RoleNodeInfo>> serviceRoles = roleInstanceEntities.stream().map(new Function<ServiceRoleInstanceEntity, RoleNodeInfo>() {
            @Override
            public RoleNodeInfo apply(ServiceRoleInstanceEntity serviceRoleInstanceEntity) {
                ClusterNodeEntity nodeEntity = clusterNodeRepository.findById(serviceRoleInstanceEntity.getNodeId()).get();
                return new RoleNodeInfo(serviceRoleInstanceEntity.getId(), nodeEntity.getHostname(), serviceRoleInstanceEntity.getServiceRoleName());
            }
        }).collect(Collectors.groupingBy(RoleNodeInfo::getRoleName));
        return serviceRoles;
    }
}
