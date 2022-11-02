package com.elasticsearch.cloud.monitor.metric.common.rule.util;

import com.elasticsearch.cloud.monitor.metric.common.client.MinioConfig;
import com.elasticsearch.cloud.monitor.metric.common.constant.Constants;
import com.elasticsearch.cloud.monitor.metric.common.rule.*;
import com.elasticsearch.cloud.monitor.metric.common.rule.constant.StorageType;
import com.elasticsearch.cloud.monitor.metric.common.rule.exception.InvalidParameterException;
import com.elasticsearch.cloud.monitor.metric.common.utils.PropertiesUtil;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.functions.FunctionContext;

/**
 * @author xiaoping
 * @date 2019/11/26
 */
public class RuleUtil {

    private static String defaultMinioEndpoint = PropertiesUtil.getProperty("default.minio.url");
    private static String defaultMinioAccessKey = PropertiesUtil.getProperty("default.minio.access_key");
    private static String defaultMinioSecretKey = PropertiesUtil.getProperty("default.minio.secret_key");
    private static String defaultMinioBucket = PropertiesUtil.getProperty("default.minio.rules_bucket");
    private static String defaultMinioFile = PropertiesUtil.getProperty("default.minio.rules_file");

    /**
     * emon平台规则管理
     * @param context
     * @return
     * @throws InvalidParameterException
     */
    public static SreworksRulesManagerFactory<OssRulesManager> createRuleManagerFactory(FunctionContext context)
            throws InvalidParameterException {
        String ossRulePath = context.getJobParameter(Constants.RULE_PATH, "");
        if (StringUtils.isEmpty(ossRulePath)) {
            throw new InvalidParameterException(String.format("config %s cannot be empty", Constants.RULE_PATH));
        }
        long refreshPeriod = Long.valueOf(
                context.getJobParameter(Constants.RULE_REFRESH_PERIOD, Constants.RULE_REFRESH_PERIOD_DEF + ""));
        long shufflePeriod = Long.valueOf(
                context.getJobParameter(Constants.RULE_REFRESH_SHUFFLE, Constants.RULE_REFRESH_SHUFFLE_DEF + ""));
        boolean forEmon=Boolean.valueOf(context.getJobParameter(Constants.RULE_FOR_EMON, "false"));
        return new OssRulesManagerFactory(ossRulePath, refreshPeriod, shufflePeriod);
    }

    /**
     * flink平台规则管理
     * @param context
     * @return
     * @throws InvalidParameterException
     */
    public static SreworksRulesManagerFactory<MinioRulesManager> createRuleManagerFactoryForFlink(FunctionContext context)
            throws InvalidParameterException {
        StorageType storageType = StorageType.valueOf(context.getJobParameter(Constants.RULE_STORAGE_TYPE,
                "MINIO").toUpperCase());

        long refreshPeriod = Long.parseLong(
                context.getJobParameter(Constants.RULE_REFRESH_PERIOD, Constants.RULE_REFRESH_PERIOD_DEF + ""));
        long shufflePeriod = Long.parseLong(
                context.getJobParameter(Constants.RULE_REFRESH_SHUFFLE, Constants.RULE_REFRESH_SHUFFLE_DEF + ""));

        if (storageType == StorageType.MINIO) {
            String minioEndpoint = context.getJobParameter(Constants.RULE_MINIO_ENDPOINT, defaultMinioEndpoint);
            String minioAccessKey = context.getJobParameter(Constants.RULE_MINIO_ACCESS_KEY, defaultMinioAccessKey);
            String minioSecretKey = context.getJobParameter(Constants.RULE_MINIO_SECRET_KEY, defaultMinioSecretKey);
            String minioBucket = context.getJobParameter(Constants.RULE_MINIO_BUCKET, defaultMinioBucket);
            String minioFile = context.getJobParameter(Constants.RULE_MINIO_FILE, defaultMinioFile);

            Preconditions.checkArgument(StringUtils.isNotEmpty(minioEndpoint) &&
                    StringUtils.isNotEmpty(minioAccessKey) && StringUtils.isNotEmpty(minioSecretKey) &&
                    StringUtils.isNotEmpty(minioBucket) && StringUtils.isNotEmpty(minioFile),
                    "minio config must not be empty");

            Preconditions.checkArgument(minioFile.endsWith(".json") || minioFile.endsWith(".zip"),
                    String.format("only json and zip file are supported, real file:%s", minioFile));

            MinioConfig minioConfig = new MinioConfig(minioEndpoint, minioAccessKey, minioSecretKey, minioBucket, minioFile);
            return new MinioRulesManagerFactory(minioConfig, refreshPeriod, shufflePeriod);

        } else if (storageType == StorageType.OSS) {
            // TODO
            throw new InvalidParameterException(String.format("invalid rule storage type %s", storageType));
        } else {
            throw new InvalidParameterException(String.format("invalid rule storage type %s", storageType));
        }
    }
}
