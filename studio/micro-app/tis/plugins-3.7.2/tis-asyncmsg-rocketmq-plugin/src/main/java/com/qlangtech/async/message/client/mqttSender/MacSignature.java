/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.async.message.client.mqttSender;

import org.apache.commons.codec.binary.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Created by heytong on 15/6/9.
 * INFO:MQTT 消息签名
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MacSignature {

    /**
     * 发送⽅方签名⽅方法
     *
     * @param text  Mqtt ClientId
     * @param secretKey 阿⾥里云ONS secretKey
     * @return 加密后的字符串
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     */
    public static String macSignature(String text, String secretKey) throws InvalidKeyException, NoSuchAlgorithmException {
        Charset charset = Charset.forName("UTF-8");
        String algorithm = "HmacSHA1";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(secretKey.getBytes(charset), algorithm));
        byte[] bytes = mac.doFinal(text.getBytes(charset));
        return new String(Base64.encodeBase64(bytes), charset);
    }

    public static String publishSignature(String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        return macSignature(clientId, secretKey);
    }

    /**
     * 订阅⽅方签名⽅方法
     *
     * @param topics 要订阅的Topic集合
     * @param clientId  Mqtt ClientId
     * @param secretKey 阿⾥里云ONS secretKey
     * @return 加密后的字符串
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     */
    public static String subSignature(List<String> topics, String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // 以字典顺序排序
        Collections.sort(topics);
        String topicText = "";
        for (String topic : topics) {
            topicText += topic + "\n";
        }
        String text = topicText + clientId;
        return macSignature(text, secretKey);
    }

    /**
     * 订阅⽅方签名⽅方法
     *
     * @param topic 要订阅的Topic
     * @param clientId  Mqtt ClientId
     * @param secretKey 阿⾥里云ONS secretKey
     * @return 加密后的字符串
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.InvalidKeyException
     */
    public static String subSignature(String topic, String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        List<String> topics = new ArrayList<String>();
        topics.add(topic);
        return subSignature(topics, clientId, secretKey);
    }
}
