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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Event;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.sdk.commons.protocol.InlongId;

import java.util.Map;

/**
 * SimpleBatchPackProfileV0
 * 
 */
public class SimpleBatchPackProfileV0 extends BatchPackProfile {

    private Event simpleProfile;
    private Map<String, String> properties;

    /**
     * Constructor
     * @param uid
     * @param inlongGroupId
     * @param inlongStreamId
     * @param dispatchTime
     */
    public SimpleBatchPackProfileV0(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        super(uid, inlongGroupId, inlongStreamId, dispatchTime);
    }

    /**
     * create
     * @param event
     * @return
     */
    public static SimpleBatchPackProfileV0 create(Event event) {
        Map<String, String> headers = event.getHeaders();
        String inlongGroupId = headers.get(AttributeConstants.GROUP_ID);
        String inlongStreamId = headers.get(AttributeConstants.STREAM_ID);
        String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        long msgTime = NumberUtils.toLong(headers.get(AttributeConstants.DATA_TIME), System.currentTimeMillis());
        long dispatchTime = msgTime - msgTime % MINUTE_MS;
        SimpleBatchPackProfileV0 profile = new SimpleBatchPackProfileV0(uid, inlongGroupId, inlongStreamId,
                dispatchTime);
        profile.setCount(1);
        profile.setSize(event.getBody().length);
        profile.simpleProfile = event;

        String pkgVersion = event.getHeaders().get(ConfigConstants.MSG_ENCODE_VER);
        if (StringUtils.isNotBlank(pkgVersion)) {
            profile.properties = MessageUtils.getXfsAttrs(headers, pkgVersion);
        }
        return profile;
    }

    /**
     * get simpleProfile
     * @return the simpleProfile
     */
    public Event getSimpleProfile() {
        return simpleProfile;
    }

    /**
     * get properties
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }
}
