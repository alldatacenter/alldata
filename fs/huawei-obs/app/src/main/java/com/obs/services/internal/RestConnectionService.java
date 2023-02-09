/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services.internal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.security.ProviderCredentials;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.HttpMethodEnum;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RestConnectionService {
    private static final ILogger log = LoggerBuilder.getLogger(RestConnectionService.class);

    protected OkHttpClient httpClient;

    protected ObsProperties obsProperties;

    protected KeyManagerFactory keyManagerFactory;

    protected TrustManagerFactory trustManagerFactory;

    protected Semaphore semaphore;

    protected AtomicBoolean shuttingDown = new AtomicBoolean(false);

    protected volatile ProviderCredentials credentials;

    protected void initHttpClient(Dispatcher httpDispatcher) {

        OkHttpClient.Builder builder = RestUtils.initHttpClientBuilder(obsProperties, keyManagerFactory,
                trustManagerFactory, httpDispatcher);

        if (this.obsProperties.getBoolProperty(ObsConstraint.PROXY_ISABLE, true)) {
            String proxyHostAddress = this.obsProperties.getStringProperty(ObsConstraint.PROXY_HOST, null);
            int proxyPort = this.obsProperties.getIntProperty(ObsConstraint.PROXY_PORT, -1);
            String proxyUser = this.obsProperties.getStringProperty(ObsConstraint.PROXY_UNAME, null);
            String proxyPassword = this.obsProperties.getStringProperty(ObsConstraint.PROXY_PAWD, null);
            RestUtils.initHttpProxy(builder, proxyHostAddress, proxyPort, proxyUser, proxyPassword);
        }

        this.httpClient = builder.build();
        // Fix okhttp bug
        int maxConnections = this.obsProperties.getIntProperty(ObsConstraint.HTTP_MAX_CONNECT,
                ObsConstraint.HTTP_MAX_CONNECT_VALUE);
        this.semaphore = new Semaphore(maxConnections);
    }

    protected void shutdown() {
        this.shutdownImpl();
    }

    protected void shutdownImpl() {
        if (shuttingDown.compareAndSet(false, true)) {
            this.credentials = null;
            this.obsProperties = null;
            if (this.httpClient != null) {
                invokeShutdown();
                if (httpClient.connectionPool() != null) {
                    httpClient.connectionPool().evictAll();
                }
                httpClient = null;
            }
        }
    }

    private void invokeShutdown() {
        try {
            Method dispatcherMethod = httpClient.getClass().getMethod("dispatcher");
            if (dispatcherMethod != null) {
                Method m = dispatcherMethod.invoke(httpClient).getClass().getDeclaredMethod("executorService");
                // fix findbugs: DP_DO_INSIDE_DO_PRIVILEGED
                // m.setAccessible(true);
                Object exeService = m.invoke(httpClient.dispatcher());
                if (exeService instanceof ExecutorService) {
                    ExecutorService executorService = (ExecutorService) exeService;
                    executorService.shutdown();
                }
            }
        } catch (Exception e) {
            // ignore
            if (log.isWarnEnabled()) {
                log.warn("shows some exceptions at the time of shutdown httpClient. ", e);
            }
        }
    }

    protected Request.Builder setupConnection(NewTransResult result, boolean isOEF, boolean isListBuckets)
            throws ServiceException {

        boolean pathStyle = this.isPathStyle();
        String endPoint = this.getEndpoint();
        boolean isCname = this.isCname();
        String hostname = (isCname || isListBuckets) ? endPoint : ServiceUtils.generateHostnameForBucket(RestUtils
                .encodeUrlString(result.getBucketName()), pathStyle, endPoint);
        String resourceString = "/";
        if (hostname.equals(endPoint) && !isCname && result.getBucketName() != null && !result.getBucketName().isEmpty()) {
            resourceString += RestUtils.encodeUrlString(result.getBucketName());
        }
        if (result.getObjectKey() != null) {
            if (result.isEncodeUrl()) {
                resourceString += ((pathStyle && !isCname) ? "/" : "") + RestUtils.encodeUrlString(result.getObjectKey());
            } else {
                resourceString += result.getObjectKey();
            }
        }

        String url = addProtocol(hostname, resourceString);
        url = addRequestParametersToUrlPath(url, result.getParams(), isOEF);

        Request.Builder builder = createRequestBuilder(result.getHttpMethod(), result.getBody(), url);

        return builder;
    }

    private Request.Builder createRequestBuilder(HttpMethodEnum method, RequestBody body, String url) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (body == null) {
            body = RequestBody.create("", null);
        }
        switch (method) {
            case PUT:
                builder.put(body);
                break;
            case POST:
                builder.post(body);
                break;
            case HEAD:
                builder.head();
                break;
            case GET:
                builder.get();
                break;
            case DELETE:
                builder.delete(body);
                break;
            case OPTIONS:
                builder.method("OPTIONS", null);
                break;
            default:
                throw new IllegalArgumentException("Unrecognised HTTP method name: " + method);
        }

        if (!this.isKeepAlive()) {
            builder.addHeader("Connection", "Close");
        }
        return builder;
    }

    private String addProtocol(String hostname, String resourceString) {
        String url = null;
        if (getHttpsOnly()) {
            int securePort = this.getHttpsPort();
            String securePortStr = securePort == 443 ? "" : ":" + securePort;
            url = "https://" + hostname + securePortStr + resourceString;
        } else {
            int insecurePort = this.getHttpPort();
            String insecurePortStr = insecurePort == 80 ? "" : ":" + insecurePort;
            url = "http://" + hostname + insecurePortStr + resourceString;
        }
        if (log.isDebugEnabled()) {
            log.debug("OBS URL: " + url);
        }
        return url;
    }

    protected String addRequestParametersToUrlPath(String urlPath, Map<String, String> requestParameters, boolean isOEF)
            throws ServiceException {
        StringBuilder urlPathBuilder = new StringBuilder(urlPath);
        if (requestParameters != null) {
            for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isOEF) {
                    if (isPathStyle()) {
                        urlPathBuilder.append("/").append(key);
                    } else {
                        urlPathBuilder.append(key);
                    }
                } else {
                    urlPathBuilder.append((urlPathBuilder.indexOf("?") < 0 ? "?" : "&"))
                            .append(RestUtils.encodeUrlString(key));
                }
                if (ServiceUtils.isValid(value)) {
                    urlPathBuilder.append("=").append(RestUtils.encodeUrlString(value));
                    if (log.isDebugEnabled()) {
                        log.debug("Added request parameter: " + key + "=" + value);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Added request parameter without value: " + key);
                    }
                }
            }
        }
        return urlPathBuilder.toString();
    }

    protected String getEndpoint() {
        return this.obsProperties.getStringProperty(ObsConstraint.END_POINT, "");
    }

    protected int getHttpPort() {
        return this.obsProperties.getIntProperty(ObsConstraint.HTTP_PORT, ObsConstraint.HTTP_PORT_VALUE);
    }

    protected int getHttpsPort() {
        return this.obsProperties.getIntProperty(ObsConstraint.HTTPS_PORT, ObsConstraint.HTTPS_PORT_VALUE);
    }

    protected boolean isKeepAlive() {
        return this.obsProperties.getBoolProperty(ObsConstraint.KEEP_ALIVE, true);
    }

    protected boolean isPathStyle() {
        return this.obsProperties.getBoolProperty(ObsConstraint.DISABLE_DNS_BUCKET, false);
    }

    protected boolean isCname() {
        return this.obsProperties.getBoolProperty(ObsConstraint.IS_CNAME, false);
    }

    protected boolean getHttpsOnly() {
        return this.obsProperties.getBoolProperty(ObsConstraint.HTTPS_ONLY, true);
    }
}
