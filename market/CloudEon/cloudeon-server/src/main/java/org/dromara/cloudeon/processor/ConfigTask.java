package org.dromara.cloudeon.processor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.dromara.cloudeon.config.CloudeonConfigProp;
import org.dromara.cloudeon.dao.*;
import org.dromara.cloudeon.dto.RoleNodeInfo;
import org.dromara.cloudeon.entity.*;
import org.dromara.cloudeon.service.SshPoolService;
import org.dromara.cloudeon.utils.Constant;
import org.dromara.cloudeon.utils.SshUtils;
import org.dromara.cloudeon.utils.UnixConverUtil;
import org.springframework.core.env.Environment;

import java.io.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NoArgsConstructor
public class ConfigTask extends BaseCloudeonTask {


    private static final String CONF_DIR = "conf";
    private static final String RENDER_DIR = "render";

    @Override
    public void internalExecute() {
        StackServiceRepository stackServiceRepository = SpringUtil.getBean(StackServiceRepository.class);
        ServiceInstanceRepository serviceInstanceRepository = SpringUtil.getBean(ServiceInstanceRepository.class);
        ClusterNodeRepository clusterNodeRepository = SpringUtil.getBean(ClusterNodeRepository.class);
        ServiceRoleInstanceRepository roleInstanceRepository = SpringUtil.getBean(ServiceRoleInstanceRepository.class);
        ServiceInstanceConfigRepository configRepository = SpringUtil.getBean(ServiceInstanceConfigRepository.class);
        SshPoolService sshPoolService = SpringUtil.getBean(SshPoolService.class);

        CloudeonConfigProp cloudeonConfigProp = SpringUtil.getBean(CloudeonConfigProp.class);
        Environment environment = SpringUtil.getBean(Environment.class);

        TaskParam taskParam = getTaskParam();
        Integer serviceInstanceId = taskParam.getServiceInstanceId();
        ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(serviceInstanceEntity.getStackServiceId()).get();
        // 查询服务实例所有配置项
        List<ServiceInstanceConfigEntity> allConfigEntityList = configRepository.findByServiceInstanceId(serviceInstanceId);
        List<ServiceRoleInstanceEntity> roleInstanceEntities = roleInstanceRepository.findByServiceInstanceId(serviceInstanceId);

        String stackCode = stackServiceEntity.getStackCode();
        String stackServiceName = stackServiceEntity.getName().toLowerCase();

        // todo 加载依赖服务的配置到本地conf目录，例如spark依赖core-site.xml和hdfs-site.xml还有hive-site.xml文件
        // 创建工作目录  ${workHome}/zookeeper1/node001/conf
        String workHome = cloudeonConfigProp.getWorkHome();
        String taskExecuteHostName = taskParam.getHostName();
        String outputConfPath = workHome + File.separator + serviceInstanceEntity.getServiceName() + File.separator + taskExecuteHostName + File.separator + CONF_DIR;
        // 先删除清空
        FileUtil.del(outputConfPath);
        FileUtil.mkdir(outputConfPath);

        // 用freemarker在本地生成服务实例的所有配置文件
        // 创建核心配置对象
        Configuration config = new Configuration(Configuration.getVersion());
        // 设置加载的目录
        try {
            String renderDir = cloudeonConfigProp.getStackLoadPath() + File.separator + stackCode + File.separator + stackServiceName + File.separator + RENDER_DIR;
            log.info("加载配置文件模板目录：" + renderDir);
            File renderDirFile = new File(renderDir);
            config.setDirectoryForTemplateLoading(renderDirFile);
            // 构建数据模型
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("service", serviceInstanceEntity);
            String dependenceServiceInstanceIds = serviceInstanceEntity.getDependenceServiceInstanceIds();
            if (StrUtil.isNotBlank(dependenceServiceInstanceIds)) {
                String[] depServiceInstanceIds = dependenceServiceInstanceIds.split(",");
                buildDependenceServiceInModel(dataModel, depServiceInstanceIds,
                        stackServiceRepository, serviceInstanceRepository, roleInstanceRepository, clusterNodeRepository, configRepository);
                // 拷贝依赖服务指定配置到当前服务实例配置目录中
                copyDependenceServiceConf(stackServiceName, depServiceInstanceIds, outputConfPath, serviceInstanceRepository, stackServiceRepository, workHome);
            }


            dataModel.put("conf", allConfigEntityList.stream().collect(Collectors.toMap(ServiceInstanceConfigEntity::getName, ServiceInstanceConfigEntity::getValue)));

            Map<String, List<RoleNodeInfo>> serviceRoles = getServiceRoles(roleInstanceEntities, clusterNodeRepository);

            dataModel.put("serviceRoles", serviceRoles);
            dataModel.put("localhostname", taskExecuteHostName);
            dataModel.put("localhostip", taskParam.getIp());
            dataModel.put("cloudeonURL", "http://"+ InetAddress.getLocalHost().getHostAddress()+":"+environment.getProperty("server.port"));

            // 获取该服务支持的自定义配置文件名
            String customConfigFiles = stackServiceEntity.getCustomConfigFiles();
            Map<String, Map<String, String>> confFiles = new HashMap<>();

            if (StringUtils.isNoneBlank(customConfigFiles)) {
                for (String confFileName : customConfigFiles.split(",")) {
                    List<ServiceInstanceConfigEntity> groupConfEntities = configRepository.findByServiceInstanceIdAndConfFile(serviceInstanceId, confFileName);
                    HashMap<String, String> map = new HashMap<>();
                    for (ServiceInstanceConfigEntity groupConf : groupConfEntities) {
                        map.put(groupConf.getName(), groupConf.getValue());
                    }
                    confFiles.put(confFileName, map);
                }
            }
            dataModel.put("confFiles", confFiles);

            // 执行渲染
            scanTemplateToRender(renderDirFile, renderDir,config,dataModel,outputConfPath);
            File dir = new File(outputConfPath);
            UnixConverUtil.convertToUnix(dir);

        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // ssh上传所有配置文件到指定目录
        ClusterNodeEntity nodeEntity = clusterNodeRepository.findByHostname(taskParam.getHostName());
        ClientSession clientSession = sshPoolService.openSession(nodeEntity.getIp(), nodeEntity.getSshPort(), nodeEntity.getSshUser(), nodeEntity.getSshPassword());
        SftpFileSystem sftp;
        sftp = sshPoolService.openSftpFileSystem(nodeEntity.getIp());
        String remoteConfDirPath = "/opt/edp/" + serviceInstanceEntity.getServiceName() +"/conf/";
        log.info("拷贝本地配置目录：" + outputConfPath + " 到节点" + taskParam.getHostName() + "的：" + remoteConfDirPath);
        try {
            SshUtils.uploadDirectory(sftp,new File(outputConfPath),remoteConfDirPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("拷贝文件上远程服务器失败："+e);
        }
        log.info("成功拷贝本地配置目录：" + outputConfPath + " 到节点" + taskParam.getHostName() + "的：" + remoteConfDirPath);

        try {
            String premissionCommand = "chmod +x " + remoteConfDirPath + "/*.sh";
            log.info("赋予conf目录下sh脚本执行权限：{}", premissionCommand);
            SshUtils.execCmdWithResult(clientSession, premissionCommand);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        // 特殊处理
        if (stackServiceEntity.getName().equals(Constant.ZOOKEEPER_SERVICE_NAME)) {
            try {
                String remoteDataDirPath = "/opt/edp/" + serviceInstanceEntity.getServiceName()  +"/data";
                String command = "mv " + remoteConfDirPath + File.separator + "myid " + remoteDataDirPath;
                log.info("移动myid文件到data目录 {}", remoteDataDirPath);
                log.info("ssh执行命令： {}", command);
                SshUtils.execCmdWithResult(clientSession, command);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        sshPoolService.returnSession(clientSession,nodeEntity.getIp());
        sshPoolService.returnSftp(sftp,nodeEntity.getIp());
    }

    /**
     * 递归扫描目录里的模板和文件进行渲染
     */
    private void scanTemplateToRender(File renderDirFile, String renderDir, Configuration config,
                                      Map<String, Object> dataModel, String outputConfPath) throws IOException, TemplateException {
        if (renderDirFile.isDirectory()) {
            log.info("Scanning directory: " + renderDirFile.getAbsolutePath());
            File[] files = renderDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanTemplateToRender(file,renderDir, config, dataModel, outputConfPath);
                }
            }
        } else {
            // 相对于render目录的路径：dashboards/grafana/alertmanager.json
            String fileSubPath = FileUtil.subPath(renderDir, renderDirFile);
            log.info("File found: " + fileSubPath);
            if (fileSubPath.endsWith(".ftl")) {
                Template template = config.getTemplate(fileSubPath);
                String outPutFile = outputConfPath + File.separator + StringUtils.substringBeforeLast(fileSubPath, ".ftl");
                // 输出文件前先建好其父级目录
                FileUtil.mkParentDirs(outPutFile);
                FileWriter out = new FileWriter(outPutFile);
                template.process(dataModel, out);
                log.info("完成配置文件生成：" + outPutFile);
                out.close();
            } else {
                InputStream fileReader = new FileInputStream(renderDir + File.separator + fileSubPath);
                String outPutFile = outputConfPath + File.separator + fileSubPath;
                // 输出文件前先建好其父级目录
                FileUtil.mkParentDirs(outPutFile);
                FileOutputStream out = new FileOutputStream(outPutFile);
                IOUtils.copy(fileReader, out);
                log.info("完成配置文件生成：" + outPutFile);
                IOUtils.close(fileReader);
                IOUtils.close(out);
            }
        }
    }

    /**
     * 拷贝依赖服务指定配置到当前服务实例配置目录中
     */
    private void copyDependenceServiceConf(String stackServiceName, String[] depServiceInstanceIds, String outputConfPath,
                                           ServiceInstanceRepository serviceInstanceRepository, StackServiceRepository stackServiceRepository, String workHome) {
        if (stackServiceName.equalsIgnoreCase(Constant.YARN_SERVICE_NAME)
                ||stackServiceName.equalsIgnoreCase(Constant.HBASE_SERVICE_NAME)
                ||stackServiceName.equalsIgnoreCase(Constant.SPARK_SERVICE_NAME)
                ||stackServiceName.equalsIgnoreCase(Constant.HIVE_SERVICE_NAME)) {
            List<Integer> instanceIds = Arrays.stream(depServiceInstanceIds).map(new Function<String, Integer>() {
                @Override
                public Integer apply(String s) {
                    return Integer.valueOf(s);
                }
            }).collect(Collectors.toList());
            // 找出依赖的HDFS服务的实例名
            String hdfsServiceInstanceName = serviceInstanceRepository.findAllById(instanceIds).stream()
                    .filter(new Predicate<ServiceInstanceEntity>() {
                        @Override
                        public boolean test(ServiceInstanceEntity serviceInstanceEntity) {
                            // 过滤出框架服务名为HDFS的服务实例
                            Integer stackServiceId = serviceInstanceEntity.getStackServiceId();
                            StackServiceEntity stackServiceEntity = stackServiceRepository.findById(stackServiceId).get();
                            return stackServiceEntity.getName().equals(Constant.HDFS_SERVICE_NAME);
                        }
                    }).map(new Function<ServiceInstanceEntity, String>() {
                        @Override
                        public String apply(ServiceInstanceEntity serviceInstanceEntity) {
                            return serviceInstanceEntity.getServiceName();
                        }
                    }).findFirst().get();
            // 拷贝core-site和hdfs-site
            String depServiceDir = workHome + File.separator + hdfsServiceInstanceName;
            if (FileUtil.exist(depServiceDir) && FileUtil.isDirectory(depServiceDir)) {
                File file = new File(depServiceDir);
                // todo 写死用第一个节点的配置 /work/hdfs1/fl001/conf
                String firstNodeDir = Arrays.stream(file.list()).findFirst().get();
                String depServiceDirConf = depServiceDir + File.separator + firstNodeDir + File.separator + CONF_DIR;

                String coreSiteFile = "core-site.xml";
                FileUtil.copy(depServiceDirConf + File.separator + coreSiteFile, outputConfPath, true);
                log.info("拷贝依赖服务HDFS的配置文件{}到配置目录",coreSiteFile);

                String hdfsSiteFile = "hdfs-site.xml";
                FileUtil.copy(depServiceDirConf + File.separator + hdfsSiteFile, outputConfPath, true);
                log.info("拷贝依赖服务HDFS的配置文件{}到配置目录",hdfsSiteFile);
            }


        }

        if (stackServiceName.equalsIgnoreCase(Constant.HIVE_SERVICE_NAME) || stackServiceName.equalsIgnoreCase(Constant.SPARK_SERVICE_NAME)) {
            List<Integer> instanceIds = Arrays.stream(depServiceInstanceIds).map(new Function<String, Integer>() {
                @Override
                public Integer apply(String s) {
                    return Integer.valueOf(s);
                }
            }).collect(Collectors.toList());
            // 找出依赖的HDFS服务的实例名
            String hdfsServiceInstanceName = serviceInstanceRepository.findAllById(instanceIds).stream()
                    .filter(serviceInstanceEntity -> {
                        // 过滤出框架服务名为YARN的服务实例
                        Integer stackServiceId = serviceInstanceEntity.getStackServiceId();
                        StackServiceEntity stackServiceEntity = stackServiceRepository.findById(stackServiceId).get();
                        return stackServiceEntity.getName().equals(Constant.YARN_SERVICE_NAME);
                    }).map(ServiceInstanceEntity::getServiceName).findFirst().get();
            // 拷贝mapred-site和yarn-site
            String depServiceDir = workHome + File.separator + hdfsServiceInstanceName;
            if (FileUtil.exist(depServiceDir) && FileUtil.isDirectory(depServiceDir)) {
                File file = new File(depServiceDir);

                String firstNodeDir = Arrays.stream(file.list()).findFirst().get();
                String depServiceDirConf = depServiceDir + File.separator + firstNodeDir + File.separator + CONF_DIR;

                String yarnSiteFile = "yarn-site.xml";
                FileUtil.copy(depServiceDirConf + File.separator + yarnSiteFile, outputConfPath, true);
                log.info("拷贝依赖服务YARN的配置文件{}到配置目录",yarnSiteFile);

                String mapredSiteFile = "mapred-site.xml";
                FileUtil.copy(depServiceDirConf + File.separator + mapredSiteFile, outputConfPath, true);
                log.info("拷贝依赖服务YARN的配置文件{}到配置目录",mapredSiteFile);
            }


        }
    }

    /**
     * 构建依赖服务进入Model中
     */
    private void buildDependenceServiceInModel(Map<String, Object> dataModel, String[] depServiceInstanceIds,
                                               StackServiceRepository stackServiceRepository,
                                               ServiceInstanceRepository serviceInstanceRepository,
                                               ServiceRoleInstanceRepository roleInstanceRepository, ClusterNodeRepository clusterNodeRepository, ServiceInstanceConfigRepository configRepository) {
        Map<String, Object> services = new HashMap<>();
        Arrays.stream(depServiceInstanceIds).forEach(id -> {
            Integer serviceInstanceId = Integer.valueOf(id);
            ServiceInstanceEntity serviceInstanceEntity = serviceInstanceRepository.findById(serviceInstanceId).get();
            Integer stackServiceId = serviceInstanceEntity.getStackServiceId();
            String stackServiceName = stackServiceRepository.findById(stackServiceId).get().getName();

            // 查询服务实例所有配置项
            List<ServiceInstanceConfigEntity> allConfigEntityList = configRepository.findByServiceInstanceId(serviceInstanceId);
            // 查出所有角色
            List<ServiceRoleInstanceEntity> roleInstanceEntities = roleInstanceRepository.findByServiceInstanceId(serviceInstanceId);
            Map<String, List<RoleNodeInfo>> serviceRoles = getServiceRoles(roleInstanceEntities, clusterNodeRepository);
            services.put(stackServiceName, ImmutableMap.of(
                    "conf", allConfigEntityList.stream().collect(Collectors.toMap(ServiceInstanceConfigEntity::getName, ServiceInstanceConfigEntity::getValue)),
                    "serviceRoles", serviceRoles,
                    "service", serviceInstanceEntity));
        });

        dataModel.put("dependencies", services);
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
