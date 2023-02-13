/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to process http connection and return result conversion,
 * currently does not support https
 */
public class HttpUtils {

    // log printer
    private static final Logger logger =
            LoggerFactory.getLogger(HttpUtils.class);

    /**
     * Send request to target server.
     *
     * @param url            the target url
     * @param inParamMap     the parameter map
     */
    public static JsonObject requestWebService(String url,
            Map<String, String> inParamMap) throws Exception {
        if (url == null) {
            throw new Exception("Web service url is null!");
        }
        if (url.trim().toLowerCase().startsWith("https://")) {
            throw new Exception("Unsupported https protocol!");
        }
        // process business parameters
        ArrayList<BasicNameValuePair> params = new ArrayList<>();
        if (inParamMap != null && !inParamMap.isEmpty()) {
            for (Map.Entry<String, String> entry : inParamMap.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            if (inParamMap.containsKey(WebFieldDef.CALLERIP.shortName)
                    || inParamMap.containsKey(WebFieldDef.CALLERIP.name)) {
                params.add(new BasicNameValuePair(WebFieldDef.CALLERIP.name,
                        AddressUtils.getIPV4LocalAddress()));
            }
        }
        // build connect configure
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(50000).setSocketTimeout(60000).build();
        // build HttpClient and HttpPost objects
        CloseableHttpClient httpclient = null;
        HttpPost httpPost1 = null;
        HttpPost httpPost2 = null;
        JsonObject jsonRes = null;
        try {
            httpclient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig).build();
            UrlEncodedFormEntity se = new UrlEncodedFormEntity(params);
            // build first post request
            httpPost1 = new HttpPost(url);
            httpPost1.setEntity(se);
            // send http request and process response
            CloseableHttpResponse response = httpclient.execute(httpPost1);
            // Determine and process the redirect request
            if (response.getStatusLine().getStatusCode() == 302) {
                String redirectURL = response.getFirstHeader("Location").getValue();
                // build 2td post request
                httpPost2 = new HttpPost(redirectURL);
                httpPost2.setEntity(se);
                response = httpclient.execute(httpPost2);
            }
            // process result
            String returnStr = EntityUtils.toString(response.getEntity());
            if (TStringUtils.isNotBlank(returnStr)
                    && response.getStatusLine().getStatusCode() == 200) {
                jsonRes = JsonParser.parseString(returnStr).getAsJsonObject();
            }
        } catch (Throwable e) {
            throw new Exception("Connecting " + url + " throw an error!", e);
        } finally {
            if (httpPost1 != null) {
                httpPost1.releaseConnection();
            }
            if (httpPost2 != null) {
                httpPost2.releaseConnection();
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException ie) {
                    logger.error("Close HttpClient error.", ie);
                }
            }
        }
        return jsonRes;
    }

    /**
     *  Test scenario:
     *     simulate where there are multiple Master nodes in the cluster,
     *      and there are nodes that do not take effect
     * Call url:
     *    http://127.0.0.1:8080/webapi.htm?method=admin_query_topic_info
     * Request parameters:
     *    topicName=test_1, brokerId=170399798
     * Master nodes:
     *    127.0.0.1:8082(invalid node),127.0.0.1:8080(valid node)
     *
     * @param args   the call arguments
     */
    public static void main(String[] args) {
        Map<String, String> inParamMap = new HashMap<>();
        inParamMap.put("topicName", "test_1");
        inParamMap.put("brokerId", "170399798");
        String masterAddr = "127.0.0.1:8082,127.0.0.1:8080";
        // build visit object
        MasterInfo masterInfo = new MasterInfo(masterAddr.trim());
        JsonObject jsonRes = null;
        // call master nodes
        for (String address : masterInfo.getNodeHostPortList()) {
            String visitUrl = "http://" + address
                    + "/webapi.htm?method=admin_query_topic_info";
            try {
                jsonRes = HttpUtils.requestWebService(visitUrl, inParamMap);
                if (jsonRes != null) {
                    // if get data, break cycle
                    break;
                }
            } catch (Throwable e) {
                //
            }
        }
        // process result
        if (jsonRes != null) {
            System.out.println("query result is " + jsonRes.toString());
        }
    }
}
