package com.elasticsearch.cloud.monitor.metric.common.rule;

import com.elasticsearch.cloud.monitor.metric.common.client.MinioConfig;
import com.elasticsearch.cloud.monitor.metric.common.rule.loader.MinioRulesLoader;
import com.elasticsearch.cloud.monitor.metric.common.rule.loader.RulesLoader;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则管理类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/01 10:45
 */
@Slf4j
public class MinioRulesManager extends ClientRulesManager {

    private MinioConfig minioConfig;

    public MinioRulesManager(MinioConfig minioConfig) {
        this(minioConfig,null);
    }

    public MinioRulesManager(MinioConfig minioConfig, RulesLoader loader) {
        super(minioConfig, loader);
    }

    @Override
    protected RulesLoader initRuleLoader(Object clientConfig) {
        minioConfig = (MinioConfig) clientConfig;
        log.info("begin init minio rule loader");
        return initRuleLoaderMinio(minioConfig);
    }

    private RulesLoader initRuleLoaderMinio(MinioConfig minioConfig) {
        RulesLoader rulesLoader = null;

        MinioClient minioClient = MinioClient.builder().endpoint(minioConfig.getEndpoint()).credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey()).build();

        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (bucketExists) {
                try {
                    minioClient.statObject(StatObjectArgs.builder().bucket(minioConfig.getBucket()).object(minioConfig.getFile()).build());
                    rulesLoader = new MinioRulesLoader(minioConfig);
                } catch (Exception ex) {
                    log.error(String.format("minio bucket: %s object: %s, check fail", minioConfig.getBucket(), minioConfig.getFile()), ex);
                }
            }
        } catch (Exception ex) {
            log.error(String.format("minio bucket: %s check fail", minioConfig.getBucket()), ex);
        }

        return rulesLoader;
    }

}
