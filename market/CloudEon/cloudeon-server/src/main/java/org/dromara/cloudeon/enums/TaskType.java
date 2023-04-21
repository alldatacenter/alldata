package org.dromara.cloudeon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {

    TAG_HOST(1, "添加k8s节点标签", "org.dromara.cloudeon.processor.TagHostLabelTask", true),
    START_K8S_SERVICE(2, "启动角色k8s服务", "org.dromara.cloudeon.processor.StartRoleK8sDeploymentTask", false),
    PULL_IMAGE_FROM_REGISTRY_TO_HOST(3, "拉取容器镜像到节点", "org.dromara.cloudeon.processor.PullImageTask", true),
    INSTALL_ROLE_TO_HOST(4, "安装服务角色到节点", "org.dromara.cloudeon.processor.InstallTask", true),
    CONFIG_ROLE_TO_HOST(5, "配置服务角色到节点", "org.dromara.cloudeon.processor.ConfigTask", true),
    INIT_HDFS_NAMENODE(6, "初始化NameNode", "org.dromara.cloudeon.processor.HdfsZkfcFormatTask", false),
    YARN_HDFS_MKDIR(7, "HDFS上创建目录", "org.dromara.cloudeon.processor.InitYARNDirOnHDFSTask", false),
    CANCEL_TAG_HOST(8, "移除节点上的标签", "org.dromara.cloudeon.processor.CancelHostTagTask", true),
    STOP_K8S_SERVICE(9, "停止K8s服务", "org.dromara.cloudeon.processor.StopRoleK8sDeploymentTask", false),
    REGISTER_BE(10, "注册be节点", "org.dromara.cloudeon.processor.RegisterDorisBeTask", false),
    REGISTER_PROMETHEUS(11, "注册prometheus采集配置", "org.dromara.cloudeon.processor.RegisterPrometheusScrapyTask", false),
    DELETE_DATA_DIR(12, "删除服务数据目录", "org.dromara.cloudeon.processor.DeleteServiceDataDirTask", true),
    DELETE_SERVICE_DB_DATA(13, "删除服务相关db数据库", "org.dromara.cloudeon.processor.DeleteServiceDBDataTask", false),
    STOP_ROLE_POD(14, "停止角色Pod", "org.dromara.cloudeon.processor.StopRolePodTask", true),
    SCALE_DOWN_K8S_SERVICE(15, "按规模减少k8s服务", "org.dromara.cloudeon.processor.ScaleDownK8sServiceTask", false),
    SCALE_UP_K8S_SERVICE(16, "按规模增加k8s服务", "org.dromara.cloudeon.processor.ScaleUpK8sServiceTask", false),
    UPDATE_SERVICE_STATE(17, "更新服务实例状态", "org.dromara.cloudeon.processor.UpdateServiceStateTask", false),
    INIT_HIVE_WAREHOUSE(17, "初始化hive仓库文件目录", "org.dromara.cloudeon.processor.InitHiveWarehouseTask", false),
    INIT_HIVE_METASTORE(18, "初始化hive Metastore", "org.dromara.cloudeon.processor.InitHiveMetastoreTask", false),
    SPARK_HDFS_MKDIR(19, "HDFS上创建Spark History目录", "org.dromara.cloudeon.processor.InitSparkHistoryDirOnHDFSTask", false),
    INIT_PROMETHEUS_ALERT(20, "导入PROMETHEUS告警规则", "org.dromara.cloudeon.processor.ImportAlertRuleTask", false),
    INIT_DS_DB(21, "初始化DS数据库", "org.dromara.cloudeon.processor.InitDSTablesTask", false),
    REGISTER_FE(22, "注册fe Follwer节点", "org.dromara.cloudeon.processor.RegisterDorisFeTask", false),



    ;

    private final int code;

    private final String name;

    /**
     * 处理器类
     */
    private final String processorClass;

    private final boolean isHostLoop;

    public static TaskType of(int code) {
        for (TaskType nodeType : values()) {
            if (nodeType.code == code) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("unknown TaskGroupType of " + code);
    }

}
