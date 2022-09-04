package com.elasticsearch.cloud.monitor.metric.common.client;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Kafka配置信息
 *
 * @author: fangzong.lyj
 * @date: 2021/08/31 19:27
 */
@Data
@AllArgsConstructor
public class MinioConfig {
    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String file;

    @Override
    public String toString() {
        return "MinioConfig{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", bucket='" + bucket + '\'' +
                ", file='" + file + '\'' +
                '}';
    }
}
