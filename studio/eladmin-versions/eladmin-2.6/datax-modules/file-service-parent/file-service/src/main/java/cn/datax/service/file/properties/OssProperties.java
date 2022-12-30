package cn.datax.service.file.properties;

import lombok.Data;

@Data
public class OssProperties {
    /**
     * 密钥key
     */
    private String accessKey;
    /**
     * 密钥密码
     */
    private String accessKeySecret;
    /**
     * 存储空间名称
     */
    private String bucketName;
    /**
     * 对象存储绑定的访问主机域名
     */
    private String domainName;
}
