/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos;

import com.qcloud.cos.auth.COSSigner;
import com.qcloud.cos.endpoint.DefaultEndpointResolver;
import com.qcloud.cos.endpoint.EndpointBuilder;
import com.qcloud.cos.endpoint.EndpointResolver;
import com.qcloud.cos.endpoint.RegionEndpointBuilder;
import com.qcloud.cos.endpoint.SuffixEndpointBuilder;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.retry.BackoffStrategy;
import com.qcloud.cos.retry.PredefinedBackoffStrategies;
import com.qcloud.cos.retry.PredefinedRetryPolicies;
import com.qcloud.cos.retry.RetryPolicy;
import com.qcloud.cos.utils.VersionInfoUtils;

public class ClientConfig {

    // 默认的获取连接的超时时间, 单位ms
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = -1;
    // 默认连接超时, 单位ms
    private static final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    // 默认的SOCKET读取超时时间, 单位ms
    private static final int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
    // 默认的维护最大HTTP连接数
    private static final int DEFAULT_MAX_CONNECTIONS_COUNT = 1024;
    // 多次签名的默认过期时间,单位秒
    private static final long DEFAULT_SIGN_EXPIRED = 3600;
    // 默认的user_agent标识
    private static final String DEFAULT_USER_AGENT = VersionInfoUtils.getUserAgent();
    // Read Limit
    private static final int DEFAULT_READ_LIMIT = (2 << 17) + 1;
    /**
     * default retry times is 3 when retryable exception occured
     **/
    private static final int DEFAULT_RETRY_TIMES = 3;
    /**
     * The max retry times if retryable exception occured
     **/
    private int maxErrorRetry = DEFAULT_RETRY_TIMES;
    /**
     * The retry policy if exception occured
     **/
    private static final RetryPolicy DEFAULT_RETRY_POLICY = PredefinedRetryPolicies.DEFAULT;
    /**
     * The sleep time interval between exception occured and retry
     **/
    public static final BackoffStrategy DEFAULT_BACKOFF_STRATEGY = PredefinedBackoffStrategies.DEFAULT;
    private Region region;
    private HttpProtocol httpProtocol = HttpProtocol.https;
    private String endPointSuffix = null;
    private EndpointBuilder endpointBuilder = null;
    private EndpointResolver endpointResolver = new DefaultEndpointResolver();
    private RetryPolicy retryPolicy = DEFAULT_RETRY_POLICY;
    private BackoffStrategy backoffStrategy = DEFAULT_BACKOFF_STRATEGY;

    // http proxy代理，如果使用http proxy代理，需要设置IP与端口
    private String httpProxyIp = null;
    private int httpProxyPort = 0;
    private String proxyUsername = null;
    private String proxyPassword = null;
    private boolean useBasicAuth = false;
    private long signExpired = DEFAULT_SIGN_EXPIRED;
    private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int maxConnectionsCount = DEFAULT_MAX_CONNECTIONS_COUNT;
    private String userAgent = DEFAULT_USER_AGENT;
    private int readLimit = DEFAULT_READ_LIMIT;
    private COSSigner cosSigner = new COSSigner();

    // 数据万象特殊请求配置
    private boolean ciSpecialRequest = false;

    // 不传入region 用于后续调用List Buckets(获取所有的bucket信息)
    public ClientConfig() {
        super();
        this.region = null;
        this.endpointBuilder = new RegionEndpointBuilder(this.region);
    }

    // 除了List Buckets, 其他API需要传入region(比如上传，下载，遍历bucket文件)
    public ClientConfig(Region region) {
        super();
        this.region = region;
        this.endpointBuilder = new RegionEndpointBuilder(this.region);
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
        this.endpointBuilder = new RegionEndpointBuilder(this.region);
    }

    public HttpProtocol getHttpProtocol() {
        return httpProtocol;
    }

    public void setHttpProtocol(HttpProtocol httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    public String getHttpProxyIp() {
        return httpProxyIp;
    }

    public void setHttpProxyIp(String httpProxyIp) {
        this.httpProxyIp = httpProxyIp;
    }

    public int getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(int httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    public long getSignExpired() {
        return signExpired;
    }

    public void setSignExpired(long signExpired) {
        this.signExpired = signExpired;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getMaxConnectionsCount() {
        return maxConnectionsCount;
    }

    public void setMaxConnectionsCount(int maxConnectionsCount) {
        this.maxConnectionsCount = maxConnectionsCount;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Deprecated
    public String getEndPointSuffix() {
        return endPointSuffix;
    }

    public void setEndPointSuffix(String endPointSuffix) {
        this.endPointSuffix = endPointSuffix;
        this.endpointBuilder = new SuffixEndpointBuilder(endPointSuffix);
    }

    public int getReadLimit() {
        return readLimit;
    }

    public void setReadLimit(int readLimit) {
        this.readLimit = readLimit;
    }

    public EndpointBuilder getEndpointBuilder() {
        return endpointBuilder;
    }

    public void setEndpointBuilder(EndpointBuilder endpointBuilder) {
        this.endpointBuilder = endpointBuilder;
    }

    public EndpointResolver getEndpointResolver() {
        return endpointResolver;
    }

    public void setEndpointResolver(EndpointResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public void setUseBasicAuth(boolean useBasicAuth) {
        this.useBasicAuth = useBasicAuth;
    }

    public boolean useBasicAuth() {
        return useBasicAuth;
    }

    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    public void setMaxErrorRetry(int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public BackoffStrategy getBackoffStrategy() {
        return backoffStrategy;
    }

    public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
        this.backoffStrategy = backoffStrategy;
    }

    public COSSigner getCosSigner() {
        return cosSigner;
    }

    public void setCosSigner(COSSigner cosSigner) {
        this.cosSigner = cosSigner;
    }

    public boolean getCiSpecialRequest() {
        return ciSpecialRequest;
    }

    public void setCiSpecialRequest(boolean ciSpecialRequest) {
        this.ciSpecialRequest = ciSpecialRequest;
    }
}
