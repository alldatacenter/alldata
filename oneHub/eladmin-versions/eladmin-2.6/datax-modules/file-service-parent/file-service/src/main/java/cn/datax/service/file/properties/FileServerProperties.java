package cn.datax.service.file.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 参考zuihou-admin-cloud文件上传
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "data.file-server")
public class FileServerProperties {
    /**
     * 为以下4个值，指定不同的自动化配置
     * qiniu：七牛云oss
     * aliyun：阿里云oss
     * local：本地盘符部署
     */
    private String type;

    /**
     * oss配置
     */
    OssProperties oss = new OssProperties();

    /**
     * local配置
     */
    LocalProperties local = new LocalProperties();
}
