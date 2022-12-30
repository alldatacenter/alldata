package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 镜像构建配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "appmanager.image-builder")
public class ImageBuilderProperties {

    /**
     * 默认 CI 账户
     */
    private String defaultCiAccount = "";

    /**
     * 默认 CI Token
     */
    private String defaultCiToken = "";

    /**
     * Docker Daemon Host
     */
    private String remoteDaemon = "";

    /**
     * ARM Docker Daemon Host
     */
    private String armRemoteDaemon = "";

    /**
     * Docker bin 绝对路径
     */
    private String dockerBin = "/usr/bin/docker";

    /**
     * 是否使用 sudo 进行 Docker 调用
     */
    private boolean useSudo = false;
}
