package com.alibaba.tesla.appmanager.server.storage;

import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.server.storage.impl.MinioStorage;
import com.alibaba.tesla.appmanager.server.storage.impl.OssStorage;
import com.aliyuncs.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Storage 外部存储配置类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
@Slf4j
public class StorageConfiguration {

    @Autowired
    private PackageProperties properties;

    @Value("${dag.hot.load.jar.minio.bucket.name:}")
    private String systemBucket;

    @Value("${appmanager.package.bucket-name:}")
    private String dagBucket;

    @Bean
    @ConditionalOnProperty(value = "appmanager.package.driver", havingValue = "minio", matchIfMissing = true)
    public Storage getMinioStorage() {
        MinioStorage client = new MinioStorage(
                properties.getEndpoint(), properties.getAccessKey(), properties.getSecretKey());
        log.info("action=init|message=minio client has initialized|endpoint={}", properties.getEndpoint());
        initBucket(client);
        return client;
    }

    @Bean
    @ConditionalOnProperty(value = "appmanager.package.driver", havingValue = "oss")
    public Storage getOssStorage() {
        OssStorage client = new OssStorage(
                properties.getEndpoint(), properties.getAccessKey(), properties.getSecretKey());
        log.info("action=init|message=oss client has initialized|endpoint={}", properties.getEndpoint());
        initBucket(client);
        return client;
    }

    /**
     * 初始化 bucket
     *
     * @param client 客户端
     */
    private void initBucket(Storage client) {
        try {
            if (!StringUtils.isEmpty(systemBucket)) {
                if (!client.bucketExists(systemBucket)) {
                    client.makeBucket(systemBucket, true);
                }
            }
            if (!StringUtils.isEmpty(dagBucket)) {
                if (!client.bucketExists(dagBucket)) {
                    client.makeBucket(dagBucket, true);
                }
            }
            log.info("init system/dag bucket finished");
        } catch (Exception e) {
            log.error("Cannot init system/dag bucket, exception={}", ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }
}
