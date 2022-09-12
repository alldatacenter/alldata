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

package org.apache.inlong.sdk.dataproxy.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.config.HostInfo;
import org.apache.inlong.sdk.dataproxy.network.HttpMessage;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.apache.inlong.sdk.dataproxy.utils.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * internal http sender
 */
public class InternalHttpSender {
    private static final Logger logger = LoggerFactory.getLogger(InternalHttpSender.class);

    private final ProxyClientConfig proxyClientConfig;
    private final ConcurrentHashSet<HostInfo> hostList;

    private final LinkedBlockingQueue<HttpMessage> messageCache;
    private final ExecutorService workerServices = Executors
            .newCachedThreadPool();
    private CloseableHttpClient httpClient;
    private final JsonParser jsonParser = new JsonParser();
    private boolean bShutDown = false;

    public InternalHttpSender(ProxyClientConfig proxyClientConfig,
                              ConcurrentHashSet<HostInfo> hostList,
                              LinkedBlockingQueue<HttpMessage> messageCache) {
        this.proxyClientConfig = proxyClientConfig;
        this.hostList = hostList;
        this.messageCache = messageCache;
        submitWorkThread();
    }

    private void submitWorkThread() {
        for (int i = 0; i < proxyClientConfig.getAsyncWorkerNumber(); i++) {
            workerServices.execute(new WorkerRunner());
        }
    }

    /**
     * construct header
     *
     * @param bodies
     * @param groupId
     * @param streamId
     * @param dt
     * @return
     */
    private ArrayList<BasicNameValuePair> getHeaders(List<String> bodies,
                                                     String groupId, String streamId, long dt) {
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("groupId", groupId));
        params.add(new BasicNameValuePair("streamId", streamId));
        params.add(new BasicNameValuePair("dt", String.valueOf(dt)));
        params.add(new BasicNameValuePair("body", StringUtils.join(bodies, "\n")));
        params.add(new BasicNameValuePair("cnt", String.valueOf(bodies.size())));

        return params;
    }

    /**
     * http client
     *
     * @param timeout
     * @param timeUnit
     * @return
     */
    private synchronized CloseableHttpClient constructHttpClient(long timeout, TimeUnit timeUnit) {
        if (httpClient != null) {
            return httpClient;
        }
        long timeoutInMs = timeUnit.toMillis(timeout);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) timeoutInMs)
                .setSocketTimeout((int) timeoutInMs).build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        return httpClientBuilder.build();
    }

    /**
     * check cache runner
     */
    private class WorkerRunner implements Runnable {
        @Override
        public void run() {
            // if not shutdown or queue is not empty
            while (!bShutDown || !messageCache.isEmpty()) {
                try {
                    while (!messageCache.isEmpty()) {
                        HttpMessage httpMessage = messageCache.poll();
                        if (httpMessage != null) {
                            SendResult result = sendMessageWithHostInfo(
                                    httpMessage.getBodies(), httpMessage.getGroupId(),
                                    httpMessage.getStreamId(), httpMessage.getDt(),
                                    httpMessage.getTimeout(), httpMessage.getTimeUnit());
                            httpMessage.getCallback().onMessageAck(result);
                        }
                    }
                    TimeUnit.MILLISECONDS.sleep(proxyClientConfig.getAsyncWorkerInterval());
                } catch (Exception exception) {
                    logger.error("exception caught", exception);
                }
            }
        }
    }

    /**
     * get random ip
     *
     * @return list of host info
     */
    public List<HostInfo> getRandomHostInfo() {
        List<HostInfo> tmpHostList = new ArrayList<>(hostList);
        Collections.shuffle(tmpHostList);
        // respect alive connection
        int maxIndex = Math.min(proxyClientConfig.getAliveConnections(), tmpHostList.size());
        return tmpHostList.subList(0, maxIndex);
    }

    /**
     * send request by http
     *
     * @param bodies
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @param hostInfo
     * @return
     *
     * @throws Exception
     */
    private SendResult sendByHttp(List<String> bodies, String groupId, String streamId, long dt,
                                  long timeout, TimeUnit timeUnit, HostInfo hostInfo) throws Exception {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        try {
            if (httpClient == null) {
                httpClient = constructHttpClient(timeout, timeUnit);
            }

            String url = "http://" + hostInfo.getHostName() + ":" + hostInfo.getPortNumber()
                    + "/dataproxy/message";

            httpPost = new HttpPost(url);
            httpPost.setHeader(HttpHeaders.CONNECTION, "close");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            ArrayList<BasicNameValuePair> contents = getHeaders(bodies, groupId, streamId, dt);
            String s = URLEncodedUtils.format(contents, StandardCharsets.UTF_8);
            logger.info("encode string is {}", s);
            httpPost.setEntity(new StringEntity(s));

            response = httpClient.execute(httpPost);
            String returnStr = EntityUtils.toString(response.getEntity());

            if (Utils.isNotBlank(returnStr) && response.getStatusLine().getStatusCode() == 200) {
                logger.debug("Get configure from manager is " + returnStr);
                JsonObject jsonRes = jsonParser.parse(returnStr).getAsJsonObject();

                if (jsonRes.has("code")) {
                    int code = jsonRes.get("code").getAsInt();
                    if (code == 1) {
                        return SendResult.OK;
                    } else {
                        logger.debug("get error response {}", returnStr);
                        return SendResult.INVALID_DATA;
                    }
                }

            } else {
                throw new Exception("exception to get response from request " + returnStr + " "
                        + response.getStatusLine().getStatusCode());
            }

        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
            if (response != null) {
                response.close();
            }
        }
        return SendResult.UNKOWN_ERROR;
    }

    /**
     * send message with host info
     *
     * @param bodies
     * @param groupId
     * @param streamId
     * @param dt
     * @param timeout
     * @param timeUnit
     * @return
     */
    public SendResult sendMessageWithHostInfo(List<String> bodies, String groupId, String streamId, long dt,
                                              long timeout, TimeUnit timeUnit) {

        List<HostInfo> randomHostList = getRandomHostInfo();
        Exception tmpException = null;
        for (HostInfo hostInfo : randomHostList) {
            try {
                return sendByHttp(bodies, groupId, streamId, dt, timeout, timeUnit, hostInfo);
            } catch (Exception exception) {
                tmpException = exception;
                logger.debug("error while sending data, resending it", exception);
            }
        }
        if (tmpException != null) {
            logger.error("error while sending data", tmpException);
        }
        return SendResult.UNKOWN_ERROR;
    }

    /**
     * close
     *
     * @throws Exception
     */
    public void close() throws Exception {
        bShutDown = true;
        if (proxyClientConfig.isCleanHttpCacheWhenClosing()) {
            messageCache.clear();
        }
        if (httpClient != null) {
            httpClient.close();
        }
        workerServices.shutdown();
    }
}
