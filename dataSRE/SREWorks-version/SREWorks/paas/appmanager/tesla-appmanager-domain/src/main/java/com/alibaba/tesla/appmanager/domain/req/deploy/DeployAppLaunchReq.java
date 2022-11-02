package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发起一次 AppPackage 的部署单请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppLaunchReq implements Serializable {

    private static final long serialVersionUID = 3256022989002584168L;

    /**
     * 应用包 ID
     */
    private Long appPackageId = 0L;

    /**
     * 请求 Yaml 配置
     */
    private String configuration;

    /**
     * 是否自动提供全局环境变量 true/false
     */
    private String autoEnvironment;

    /**
     * 是否移除应用 ID 后缀
     */
    private Boolean removeSuffix = false;
}
