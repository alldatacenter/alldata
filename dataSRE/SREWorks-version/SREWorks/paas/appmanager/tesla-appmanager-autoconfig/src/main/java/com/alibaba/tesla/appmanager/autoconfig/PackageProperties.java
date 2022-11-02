package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 包相关配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "appmanager.package")
public class PackageProperties {

    private String driver = "minio";

    private String endpoint;

    private String endpointProtocol;

    private String accessKey;

    private String secretKey;

    private String bucketName = "appmanager";

    private String pythonBin = "python";

    private String abmcliBin = "abmcli";

    private String flowName = "app-manager-flow.jar";

    private Boolean inUnitTest = false;

    private String kanikoImage = System.getenv("KANIKO_IMAGE");

    /**
     * 针对 AppPackage，对于每个应用，默认的保留最新的包的个数
     */
    private Integer defaultKeepNumbers = 5;

    public String getEndpointProtocol(){
        return StringUtils.isEmpty(endpointProtocol)?"http://":endpointProtocol;
    }
}
