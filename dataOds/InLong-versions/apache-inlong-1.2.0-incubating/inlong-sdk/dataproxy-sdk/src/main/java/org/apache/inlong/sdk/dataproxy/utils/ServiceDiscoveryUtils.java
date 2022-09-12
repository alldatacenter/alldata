/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;

import static org.apache.inlong.sdk.dataproxy.ConfigConstants.REQUEST_HEADER_AUTHORIZATION;

/**
 * Utils for service discovery
 */
public class ServiceDiscoveryUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryUtils.class);

    private static final String GET_MANAGER_IP_LIST_API = "/api/inlong/manager/openapi/agent/getManagerIpList";
    private static String latestManagerIPList = "";
    private static String arraySed = ",";

    /**
     * Get Inlong-Manager IP list from the given proxy client config
     */
    public static String getManagerIpList(ProxyClientConfig clientConfig) {
        String managerAddress = clientConfig.getManagerIP() + ":" + clientConfig.getManagerPort();
        if (StringUtils.isBlank(managerAddress)) {
            log.error("ServiceDiscovery get managerIpList but managerAddress is blank, just return");
            return null;
        }

        String managerIpList = getManagerIpListByHttp(managerAddress, clientConfig);
        if (StringUtils.isNotBlank(managerIpList)) {
            latestManagerIPList = managerIpList;
            return managerIpList;
        }

        log.error("ServiceDiscovery get managerIpList from {} occur error, try to get from latestManagerIPList",
                managerAddress);

        String[] managerIps = latestManagerIPList.split(arraySed);
        if (managerIps.length > 0) {
            for (String managerIp : managerIps) {
                if (StringUtils.isBlank(managerIp)) {
                    log.error("ServiceDiscovery managerIp is null, latestManagerIPList is {}", latestManagerIPList);
                    continue;
                }

                String currentAddress = managerIp + ":" + clientConfig.getManagerPort();
                managerIpList = getManagerIpListByHttp(currentAddress, clientConfig);
                if (StringUtils.isBlank(managerIpList)) {
                    log.error("ServiceDiscovery get latestManagerIPList from {} but got nothing, will try next ip",
                            managerIp);
                    continue;
                }
                latestManagerIPList = managerIpList;
                return managerIpList;
            }
        } else {
            log.error("ServiceDiscovery latestManagerIpList {} format error, or not contain ip", latestManagerIPList);
        }

        String existedIpList = getLocalManagerIpList(clientConfig.getManagerIpLocalPath());
        if (StringUtils.isNotBlank(existedIpList)) {
            String[] existedIps = existedIpList.split(arraySed);
            if (existedIps.length > 0) {
                for (String existedIp : existedIps) {
                    if (StringUtils.isBlank(existedIp)) {
                        log.error("ServiceDiscovery get illegal format ipList from local file, "
                                        + "exist ip is empty, managerIpList is {}, local file is {}",
                                existedIpList, clientConfig.getManagerIpLocalPath());
                        continue;
                    }

                    String currentAddress = existedIp + ":" + clientConfig.getManagerPort();
                    managerIpList = getManagerIpListByHttp(currentAddress, clientConfig);
                    if (StringUtils.isBlank(managerIpList)) {
                        log.error("ServiceDiscovery get {} from local file {} but got nothing, will try next ip",
                                existedIp, clientConfig.getManagerIpLocalPath());
                        continue;
                    }
                    latestManagerIPList = managerIpList;
                    return managerIpList;
                }
            } else {
                log.error("ServiceDiscovery get illegal format ipList from local file, "
                                + "exist ip is empty, managerIpList is {}, local file is {}",
                        existedIpList, clientConfig.getManagerIpLocalPath());
            }
        } else {
            log.error("ServiceDiscovery get empty ipList from local file {}", clientConfig.getManagerIpLocalPath());
        }

        return managerIpList;
    }

    /**
     * Get Inlong-Manager IP list from the given managerIp and proxy client config
     */
    public static String getManagerIpListByHttp(String managerIp, ProxyClientConfig proxyClientConfig) {
        String url =
                (proxyClientConfig.isLocalVisit() ? "http://" : "https://") + managerIp + GET_MANAGER_IP_LIST_API;
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("operation", "query"));
        params.add(new BasicNameValuePair("username", proxyClientConfig.getUserName()));

        log.info("Begin to get configure from manager {}, param is {}", url, params);
        CloseableHttpClient httpClient;
        HttpParams myParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(myParams, proxyClientConfig.getManagerConnectionTimeout());
        HttpConnectionParams.setSoTimeout(myParams, proxyClientConfig.getManagerSocketTimeout());
        if (proxyClientConfig.isLocalVisit()) {
            httpClient = new DefaultHttpClient(myParams);
        } else {
            try {
                ArrayList<Header> headers = new ArrayList<>();
                for (BasicNameValuePair paramItem : params) {
                    headers.add(new BasicHeader(paramItem.getName(), paramItem.getValue()));
                }
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(10000).setSocketTimeout(30000).build();
                SSLContext sslContext = SSLContexts.custom().build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                        new String[]{"TLSv1"}, null,
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
                httpClient = HttpClients.custom().setDefaultHeaders(headers)
                        .setDefaultRequestConfig(requestConfig).setSSLSocketFactory(sslsf).build();
            } catch (Throwable t) {
                log.error("Create Https client failed: ", t);
                return null;
            }
        }

        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            if (proxyClientConfig.isNeedAuthentication()) {
                long timestamp = System.currentTimeMillis();
                int nonce = new SecureRandom(String.valueOf(timestamp).getBytes()).nextInt(Integer.MAX_VALUE);
                httpPost.setHeader(REQUEST_HEADER_AUTHORIZATION,
                        Utils.getAuthorizenInfo(proxyClientConfig.getUserName(),
                                proxyClientConfig.getSecretKey(), timestamp, nonce));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = httpClient.execute(httpPost);
            String returnStr = EntityUtils.toString(response.getEntity());
            if (Utils.isNotBlank(returnStr) && response.getStatusLine().getStatusCode() == 200) {
                log.info("Get configure from manager is " + returnStr);
                JsonParser jsonParser = new JsonParser();
                JsonObject jb = jsonParser.parse(returnStr).getAsJsonObject();
                if (jb == null) {
                    log.warn("ServiceDiscovery updated manager ip failed, returnStr = {} jb is "
                                    + "null ", returnStr, jb);
                    return null;
                }
                JsonObject rd = jb.get("resultData").getAsJsonObject();
                String ip = rd.get("ip").getAsString();
                log.info("ServiceDiscovery updated manager ip success, ip : " + ip + ", retStr : " + returnStr);
                return ip;
            }
            return null;
        } catch (Throwable t) {
            log.error("Connect Manager error: ", t);
            return null;
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

    /**
     * Get Inlong-Manager IP list from local path
     */
    public static String getLocalManagerIpList(String localPath) {
        log.info("ServiceDiscovery start loading config from file {} ...", localPath);
        String newestIp = null;
        try {
            File managerIpListFile = new File(localPath);
            if (!managerIpListFile.exists()) {
                log.info("ServiceDiscovery not found local groupIdInfo file from {}", localPath);
                return null;
            }
            byte[] serialized = FileUtils.readFileToByteArray(managerIpListFile);
            if (serialized == null) {
                return null;
            }
            newestIp = new String(serialized, StandardCharsets.UTF_8);
            log.info("ServiceDiscovery get manager ip list from local success, result is: {}", newestIp);
        } catch (IOException e) {
            log.error("ServiceDiscovery load manager config error: ", e);
        }

        return newestIp;
    }

    /**
     * Update Inlong-Manager info to local file
     */
    public static void updateManagerInfo2Local(String storeString, String path) {
        if (StringUtils.isBlank(storeString)) {
            log.warn("ServiceDiscovery updateTdmInfo2Local error, configMap is empty or managerIpList is blank");
            return;
        }
        File localPath = new File(path);
        if (!localPath.getParentFile().exists()) {
            localPath.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(localPath), StandardCharsets.UTF_8))) {
            writer.write(storeString);
            writer.flush();
        } catch (IOException e) {
            log.error("ServiceDiscovery save manager config error: ", e);
        }
    }

}
