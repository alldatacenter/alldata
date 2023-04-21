package org.dromara.cloudeon.enums;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum TaskGroupType {

    PULL_IMAGE_FROM_REGISTRY(1, "从dockerRegistry拉取镜像", Lists.newArrayList(TaskType.PULL_IMAGE_FROM_REGISTRY_TO_HOST), true),
    INSTALL_SERVICE(2, "安装服务", Lists.newArrayList(TaskType.INSTALL_ROLE_TO_HOST), true),
    CONFIG_SERVICE(3, "配置服务", Lists.newArrayList(TaskType.CONFIG_ROLE_TO_HOST), true),
    TAG_AND_START_K8S_SERVICE(4, "添加标签并启动k8s服务", Lists.newArrayList(TaskType.TAG_HOST, TaskType.START_K8S_SERVICE), true),
    INIT_HDFS(5, "初始化HDFS", Lists.newArrayList(TaskType.INIT_HDFS_NAMENODE), false),
    INIT_YARN(6, "初始化YARN", Lists.newArrayList(TaskType.YARN_HDFS_MKDIR), false),
    CANCEL_TAG_AND_STOP_K8S_SERVICE(7, "移除标签并停止k8s服务", Lists.newArrayList(TaskType.CANCEL_TAG_HOST, TaskType.STOP_K8S_SERVICE), true),
    INIT_DORIS(8, "初始化DORIS", Lists.newArrayList(TaskType.REGISTER_FE,TaskType.REGISTER_BE), false),
    REGISTER_MONITOR(9, "注册监控到Monitor服务", Lists.newArrayList(TaskType.REGISTER_PROMETHEUS), false),
    DELETE_SERVICE(10, "删除服务", Lists.newArrayList(TaskType.DELETE_DATA_DIR), true),
    DELETE_DB_DATA(11, "删除db中服务相关数据", Lists.newArrayList(TaskType.DELETE_SERVICE_DB_DATA), false),
    STOP_ROLE(12, "停止服务的角色实例", Lists.newArrayList(TaskType.CANCEL_TAG_HOST,TaskType.STOP_ROLE_POD,TaskType.SCALE_DOWN_K8S_SERVICE), true),
    START_ROLE(13, "启动服务的角色实例", Lists.newArrayList(TaskType.TAG_HOST,TaskType.SCALE_UP_K8S_SERVICE), true),
    UPDATE_SERVICE_STATE(14, "更新服务实例状态", Lists.newArrayList(TaskType.UPDATE_SERVICE_STATE), false),
    INIT_HIVE(15, "初始化HIVE", Lists.newArrayList(TaskType.INIT_HIVE_METASTORE,TaskType.INIT_HIVE_WAREHOUSE), false),
    INIT_SPARK(16, "初始化SPARK", Lists.newArrayList(TaskType.SPARK_HDFS_MKDIR), false),
    INIT_MONITOR(17, "初始化MONITOR", Lists.newArrayList(TaskType.INIT_PROMETHEUS_ALERT), false),
    INIT_DS(18, "初始化DS", Lists.newArrayList(TaskType.INIT_DS_DB), false),

    ;

    private final int code;

    private final String name;
    private final List<TaskType> taskTypes;
    private final boolean isRoleLoop;


    public static TaskGroupType of(int code) {
        for (TaskGroupType nodeType : values()) {
            if (nodeType.code == code) {
                return nodeType;
            }
        }
        throw new IllegalArgumentException("unknown TaskGroupType of " + code);
    }

}
