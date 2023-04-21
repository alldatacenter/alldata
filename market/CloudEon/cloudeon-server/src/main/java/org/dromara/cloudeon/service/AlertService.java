package org.dromara.cloudeon.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.dromara.cloudeon.config.CloudeonConfigProp;
import org.dromara.cloudeon.dao.ClusterAlertRuleRepository;
import org.dromara.cloudeon.dao.ClusterNodeRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.entity.ClusterAlertRuleEntity;
import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.dromara.cloudeon.utils.Constant;
import org.dromara.cloudeon.utils.SshUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.dromara.cloudeon.utils.Constant.*;

@Service
public class AlertService {
    @Resource
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Resource
    private CloudeonConfigProp cloudeonConfigProp;

    @Resource
    private ServiceInstanceRepository serviceInstanceRepository;

    @Resource
    private ServiceRoleInstanceRepository roleInstanceRepository;

    @Resource
    private ClusterAlertRuleRepository clusterAlertRuleRepository;

    @Resource
    private ClusterNodeRepository clusterNodeRepository;

    @Resource
    private SshPoolService sshPoolService;

    public void upgradeMonitorAlertRule(Integer clusterId, Logger log) {
        String workHome = cloudeonConfigProp.getWorkHome();
        // 创建本地告警规则资源工作目录  ${workHome}/alert-rule/1/
        String alertRuleOutputPath = workHome + File.separator + Constant.ALERT_RULE_RESOURCE_DIR + File.separator + clusterId;
        log.info("开始集群告警规则资源文件生成：" + alertRuleOutputPath);

        if (!FileUtil.exist(alertRuleOutputPath)) {
            log.info("目录{}不存在，创建目录...", alertRuleOutputPath);
            FileUtil.mkdir(alertRuleOutputPath);
        }
        List<ClusterAlertRuleEntity> alertRuleEntityList = clusterAlertRuleRepository.findByClusterId(clusterId);
        Map<String, List<ClusterAlertRuleEntity>> map = alertRuleEntityList.stream().collect(Collectors.groupingBy(ClusterAlertRuleEntity::getStackServiceName));
        // 根据服务生成对应的告警规则文件
        map.entrySet().stream().forEach(new Consumer<Map.Entry<String, List<ClusterAlertRuleEntity>>>() {
            @Override
            public void accept(Map.Entry<String, List<ClusterAlertRuleEntity>> stringListEntry) {
                String serviceName = stringListEntry.getKey();
                try {
                    String outputFileName = serviceName + ".yml";
                    String outPutFile = alertRuleOutputPath + File.separator + outputFileName;
                    Template template = freeMarkerConfigurer.getConfiguration().getTemplate("alert-rule-template.ftl");
                    // 处理模板
                    Map<String, Object> data = new HashMap<>();
                    data.put("serviceName", serviceName);
                    data.put("ruleList", stringListEntry.getValue());
                    FileWriter out = new FileWriter(outPutFile);
                    template.process(data, out);
                    log.info("完成服务告警规则资源文件生成：" + outPutFile);
                    out.close();
                } catch (IOException | TemplateException e) {
                    e.printStackTrace();
                }
            }
        });

        // 查找promethus服务所在节点，将本地目录里该集群的告警规则文件全部上传
        ServiceInstanceEntity monitorServiceInstance = serviceInstanceRepository.findEntityByClusterIdAndStackServiceName(clusterId, MONITOR_SERVICE_NAME);
        ServiceRoleInstanceEntity prometheus = roleInstanceRepository.findByServiceInstanceIdAndServiceRoleName(monitorServiceInstance.getId(), MONITOR_ROLE_PROMETHEUS).get(0);
        ClusterNodeEntity prometheusNode = clusterNodeRepository.findById(prometheus.getNodeId()).get();
        // 建立ssh连接
        ClientSession clientSession = sshPoolService.openSession(prometheusNode.getIp(), prometheusNode.getSshPort(), prometheusNode.getSshUser(), prometheusNode.getSshPassword());
        SftpFileSystem sftp;
        sftp = sshPoolService.openSftpFileSystem(prometheusNode.getIp());
        String remoteConfDirPath = "/opt/edp/" + monitorServiceInstance.getServiceName() + "/conf/rule/";
        log.info("拷贝本地配置目录：" + alertRuleOutputPath + " 到节点" + prometheusNode.getIp() + "的：" + remoteConfDirPath);
        try {
            SshUtils.uploadLocalDirToRemote(remoteConfDirPath, alertRuleOutputPath, sftp);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        sshPoolService.returnSession(clientSession,prometheusNode.getIp());
        sshPoolService.returnSftp(sftp,prometheusNode.getIp());

    }
}
