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

/*
 * Created with IntelliJ IDEA.
 * User:jiandan
 * Date:2015/6/19.
 * Time:13:23.
 * INFO:用于向 Mqtt 客户端发送消息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IMqttSendManager {

    /**
     * 发消息，此时发送key取md5(msgTopic+msgTag+内容)，默认采用 FastJson 序列化
     *
     * @param msg  消息内容
     * @param otherParams
     * @return 序列id，出错时返回null
     */
    Boolean sendMsg(String msg, String... otherParams);

    /**
     * 发送P2P 消息
     * @param msg
     * @param deviceIds
     */
    void sendP2pMsg(String msg, String... deviceIds);

    /**
     * 重新初始化MQTT连接池
     */
    void reConnection();
}
