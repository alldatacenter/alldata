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

package org.apache.inlong.sort.standalone.sink.pulsar;

import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * DefaultEvent2PulsarRecordHandler
 */
public class DefaultEvent2PulsarRecordHandler implements IEvent2PulsarRecordHandler {

    public static final Logger LOG = InlongLoggerFactory.getLogger(DefaultEvent2PulsarRecordHandler.class);

    public static final String KEY_EXTINFO = "extinfo";
    protected final ByteArrayOutputStream outMsg = new ByteArrayOutputStream();
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    protected final Date currentDate = new Date();

    /**
     * parse
     * 
     * @param  context
     * @param  event
     * @return             byte array
     * @throws IOException
     */
    @Override
    public byte[] parse(PulsarFederationSinkContext context, ProfileEvent event)
            throws IOException {
        String uid = event.getUid();
        PulsarIdConfig idConfig = context.getIdConfig(uid);
        if (idConfig == null) {
            context.addSendResultMetric(event, context.getTaskName(), false, System.currentTimeMillis());
            LOG.error("Can not find the id config:{}", uid);
            return null;
        }
        String delimiter = idConfig.getSeparator();
        byte separator = (byte) delimiter.charAt(0);
        outMsg.reset();
        switch (idConfig.getDataType()) {
            case TEXT:
                currentDate.setTime(event.getRawLogTime());
                String ftime = dateFormat.format(currentDate);
                outMsg.write(ftime.getBytes());
                outMsg.write(separator);
                String extinfo = getExtInfo(event);
                outMsg.write(extinfo.getBytes());
                outMsg.write(separator);
                break;
            case PB:
            case JCE:
            case UNKNOWN:
                break;
            default:
                break;
        }
        outMsg.write(event.getBody());
        byte[] msgContent = outMsg.toByteArray();
        return msgContent;
    }

    /**
     * getExtInfo
     * 
     * @param  event
     * @return
     */
    public String getExtInfo(ProfileEvent event) {
        String extinfoValue = event.getHeaders().get(KEY_EXTINFO);
        if (extinfoValue != null) {
            return KEY_EXTINFO + "=" + extinfoValue;
        }
        extinfoValue = KEY_EXTINFO + "=" + event.getHeaders().get(EventConstants.HEADER_KEY_SOURCE_IP);
        return extinfoValue;
    }
}
