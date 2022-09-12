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
import org.apache.inlong.sdk.dataproxy.pb.channel.ProfileTransaction;

/**
 * 
 * ProfileEvent
 */
public class ProfileEvent implements Event {

    private final CallbackProfile profile;
    private final ProfileTransaction transaction;

    /**
     * Constructor
     * 
     * @param profile
     * @param transaction
     */
    public ProfileEvent(CallbackProfile profile, ProfileTransaction transaction) {
        this.profile = profile;
        this.transaction = transaction;
    }

    /**
     * getHeaders
     * 
     * @return
     */
    @Override
    public Map<String, String> getHeaders() {
        return this.profile.getEvent().getHeaders();
    }

    /**
     * setHeaders
     * 
     * @param headers
     */
    @Override
    public void setHeaders(Map<String, String> headers) {
        this.profile.getEvent().setHeaders(headers);
    }

    /**
     * getBody
     * 
     * @return
     */
    @Override
    public byte[] getBody() {
        return this.profile.getEvent().getBody();
    }

    /**
     * setBody
     * 
     * @param body
     */
    @Override
    public void setBody(byte[] body) {
        this.profile.getEvent().setBody(body);
    }

    /**
     * get profile
     * 
     * @return the profile
     */
    public CallbackProfile getProfile() {
        return profile;
    }

    /**
     * commit
     */
    public void commit() {
        this.transaction.commitTakeEvent();
    }

}
