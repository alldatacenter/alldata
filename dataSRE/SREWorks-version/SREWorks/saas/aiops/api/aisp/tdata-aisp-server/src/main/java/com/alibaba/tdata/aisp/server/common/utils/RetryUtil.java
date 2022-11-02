package com.alibaba.tdata.aisp.server.common.utils;

import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @ClassName: RetryUtil
 * @Author: dyj
 * @DATE: 2021-12-29
 * @Description:
 **/
public class RetryUtil {
    private static RetryTemplate retryTemplate;

    public static RetryTemplate getRetryClient(){
        if (retryTemplate==null){
            RetryTemplate retryTemplate = new RetryTemplate();
            SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
            //最大重试次数
            simpleRetryPolicy.setMaxAttempts(3);

            ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
            backOffPolicy.setInitialInterval(1000);
            backOffPolicy.setMaxInterval(6000);
            retryTemplate.setBackOffPolicy(backOffPolicy);
            retryTemplate.setRetryPolicy(simpleRetryPolicy);
            return retryTemplate;
        } else {
            return retryTemplate;
        }
    }
}
