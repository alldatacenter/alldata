/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.context;

import java.util.Map;

import org.apache.flume.Event;
import org.apache.inlong.sdk.commons.protocol.SdkEvent;
import org.apache.inlong.sdk.dataproxy.SendMessageCallback;

/**
 * 
 * CallbackProfile
 */
public class CallbackProfile implements Event {

    private final SdkEvent event;
    private final long sendSdkTime;
    private final SendMessageCallback callback;

    /**
     * Constructor
     * 
     * @param event
     * @param callback
     */
    public CallbackProfile(SdkEvent event, SendMessageCallback callback) {
        this.event = event;
        this.callback = callback;
        this.sendSdkTime = System.currentTimeMillis();
    }

    /**
     * get event
     * 
     * @return the event
     */
    public SdkEvent getEvent() {
        return event;
    }

    /**
     * get callback
     * 
     * @return the callback
     */
    public SendMessageCallback getCallback() {
        return callback;
    }

    /**
     * get sendSdkTime
     * 
     * @return the sendSdkTime
     */
    public long getSendSdkTime() {
        return sendSdkTime;
    }

    /**
     * getHeaders
     * 
     * @return
     */
    @Override
    public Map<String, String> getHeaders() {
        return event.getHeaders();
    }

    /**
     * setHeaders
     * 
     * @param headers
     */
    @Override
    public void setHeaders(Map<String, String> headers) {
        this.event.setHeaders(headers);
    }

    /**
     * getBody
     * 
     * @return
     */
    @Override
    public byte[] getBody() {
        return this.event.getBody();
    }

    /**
     * setBody
     * 
     * @param body
     */
    @Override
    public void setBody(byte[] body) {
        this.event.setBody(body);
    }

}
