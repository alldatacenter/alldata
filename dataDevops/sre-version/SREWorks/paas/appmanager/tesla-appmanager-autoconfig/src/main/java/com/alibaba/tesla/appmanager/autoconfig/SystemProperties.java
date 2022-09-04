package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 系统相关设置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "appmanager.system")
public class SystemProperties {

    /**
     * 系统环境
     */
    private String envId = "prod";

    /**
     * 是否启用产品任务执行器
     */
    private boolean enableProductTaskExecutor = true;

    /**
     * 流程历史保留时长（秒）
     */
    private Integer flowHistoryKeepSeconds = 86400;

    /**
     * 部署单最大运行时长（秒）
     */
    private Integer deploymentMaxRunningSeconds = 5400;

    /**
     * 构建单最大运行时长（秒）
     */
    private Integer buildMaxRunningSeconds = 3600;

    /**
     * 实例历史保留日期 (app/component/trait)
     */
    private Integer instanceHistoryKeepDays = 1;

    /**
     * 是否开启 kaniko
     */
    private boolean enableKaniko = false;

    /**
     * 是否开启 worker 进行包创建
     */
    private boolean enableWorker = true;

    /**
     * 限制部署行为到指定 namespace 中
     */
    private String restrictNamespace = System.getenv("RESTRICT_NAMESPACE");

    /**
     * System Docker Registry
     */
    private String dockerRegistry = System.getenv("DOCKER_REGISTRY");

    /**
     * System Docker Namespace
     */
    private String dockerNamespace = System.getenv("DOCKER_NAMESPACE");

    /**
     * 远程 Docker 后端
     */
    private String remoteDockerDaemon = System.getenv("REMOTE_DOCKER_DAEMON");

    /**
     * 当前部署的namspace
     */
    private String k8sNamespace = System.getenv("K8S_NAMESPACE");

    /**
     * k8s docker secret name
     */
    private String k8sDockerSecret = System.getenv("K8S_DOCKER_SECRET");
}
