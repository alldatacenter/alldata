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

package org.apache.inlong.sdk.dataproxy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.inlong.sdk.dataproxy.network.ProxysdkException;

public interface MessageSender {

    /**
     * This method provides a synchronized function which you want to send data
     * with extra attributes except  groupId,streamId,dt,etc
     * This method is deprecated,we suggest you don't use it.
     *
     * @param body       The data will be sent
     * @param attributes The attributes you want to add
     */
    @Deprecated
    public SendResult sendMessage(byte[] body, String attributes, String msgUUID,
                                  long timeout, TimeUnit timeUnit);

    /**
     * This method provides a synchronized  function which you want to send data  without packing
     *
     * @param body The data will be sent
     *             
     */
    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit);

    /**
     * This method provides a synchronized  function which you want to send data without packing
     * with extra attributes except  groupId,streamId,dt,etc
     *
     * @param body         The data will be sent
     *                     
     * @param extraAttrMap The attributes you want to add,
     *                     and each element of extraAttrMap contains a pair like attrKey,attrValue
     */
    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap);

    /**
     * This method provides a synchronized  function which you want to send data  with packing
     * 
     *
     * @param bodyList The data will be sent,which is a collection consisting of byte arrays
     */
    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit);

    /**
     * This method provides a synchronized  function which you want to send data with packing
     * with extra attributes except  groupId,streamId,dt,etc
     * 
     *
     * @param bodyList     The data will be sent,which is a collection consisting of byte arrays
     * @param extraAttrMap The attributes you want to add,
     *                     and each element of extraAttrMap contains a pair like attrKey,attrValue
     */
    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap);

    /**
     * This method provides an asynchronized  function which you want to send data
     * with extra attributes except  groupId,streamId,dt,etc
     * This method is deprecated,we suggest you don't use it.
     * 
     *
     * @param body       The data will be sent
     * @param attributes The attributes you want to add
     */
    @Deprecated
    public void asyncSendMessage(SendMessageCallback callback,
                                 byte[] body, String attributes, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException;

    /**
     * This method provides a synchronized  function which you want to send data without packing
     * with extra attributes except  groupId,streamId,dt,etc
     * 
     *
     * @param body         The data will be sent
     * @param extraAttrMap The attributes you want to add,
     *                     and each element of extraAttrMap contains a pair like attrKey,attrValue
     */
    public void asyncSendMessage(SendMessageCallback callback,
                                 byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit,
                                 Map<String, String> extraAttrMap) throws ProxysdkException;

    /**
     * This method provides an asynchronized  function which you want to send data  without packing
     * 
     *
     * @param callback The implementation of callback function
     * @param body     The data will be sent
     */
    public void asyncSendMessage(SendMessageCallback callback,
                                 byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException;

    /**
     * This method provides an asynchronized  function which you want to send data  with packing
     * 
     *
     * @param bodyList The data will be sent,which is a collection consisting of byte arrays
     */
    public void asyncSendMessage(SendMessageCallback callback,
                                 List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException;

    /**
     * This method provides an asynchronized  function which you want to send data with packing
     * with extra attributes except  groupId,streamId,dt,etc
     * 
     *
     * @param bodyList     The data will be sent,which is a collection consisting of byte arrays
     * @param extraAttrMap The attributes you want to add, and each
     *                     element of extraAttrMap contains a pair like attrKey,attrValue
     */
    public void asyncSendMessage(SendMessageCallback callback,
                                 List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit,
                                 Map<String, String> extraAttrMap) throws ProxysdkException;

    /**
     * This method provides an asynchronized function which you want to send data.<br>
     * Sending timeout is configured by MessageSenderBuilder.<br>
     * Message time will be current time.<br>
     * @param inlongGroupId
     * @param inlongStreamId
     * @param body
     * @param callback callback can be null
     * @throws ProxysdkException
     */
    void asyncSendMessage(String inlongGroupId, String inlongStreamId, byte[] body, SendMessageCallback callback) 
            throws ProxysdkException;
    
    /**
     * This method provides an asynchronized function which you want to send datas.<br>
     * Sending timeout is configured by MessageSenderBuilder.<br>
     * Message time will be current time.<br>
     * @param inlongGroupId
     * @param inlongStreamId
     * @param bodyList
     * @param callback callback can be null
     * @throws ProxysdkException
     */
    void asyncSendMessage(String inlongGroupId, String inlongStreamId, List<byte[]> bodyList, 
            SendMessageCallback callback) throws ProxysdkException;

    public void close();
}
