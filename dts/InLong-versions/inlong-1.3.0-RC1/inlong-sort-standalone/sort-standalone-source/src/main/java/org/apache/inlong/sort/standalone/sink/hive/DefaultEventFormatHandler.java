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

package org.apache.inlong.sort.standalone.sink.hive;

import static org.apache.inlong.sort.standalone.sink.hive.HdfsIdConfig.SEPARATOR_LENGTH;

import org.apache.inlong.sort.standalone.channel.ProfileEvent;

/**
 * 
 * DefaultEventFormatHandler
 */
public class DefaultEventFormatHandler implements IEventFormatHandler {

    /**
     * format data, the protocol is : partitionField|msgTime|rawData<br>
     * 1. add partition field and separator; partitionField support Java Date Format;<br>
     * 2. add msgTime field and separator; msgTime support Java Date Format;<br>
     * 3. keep rawData;<br>
     * 
     * @param  event
     * @param  idConfig
     * @return
     */
    @Override
    public byte[] format(ProfileEvent event, HdfsIdConfig idConfig) {
        long msgTime = event.getRawLogTime();
        String partitionFieldValue = idConfig.parsePartitionField(msgTime);
        String msgTimeFieldValue = idConfig.parseMsgTimeField(msgTime);
        byte[] partitionFieldBytes = partitionFieldValue.getBytes();
        byte[] msgTimeFieldBytes = msgTimeFieldValue.getBytes();
        byte[] formatBytes = new byte[partitionFieldBytes.length + SEPARATOR_LENGTH + msgTimeFieldBytes.length
                + SEPARATOR_LENGTH + event.getBody().length];
        int index = 0;
        System.arraycopy(partitionFieldBytes, 0, formatBytes, index, partitionFieldBytes.length);
        index += partitionFieldBytes.length;
        formatBytes[index] = (byte) idConfig.getSeparator().charAt(0);
        index++;
        System.arraycopy(msgTimeFieldBytes, 0, formatBytes, index, msgTimeFieldBytes.length);
        index += msgTimeFieldBytes.length;
        formatBytes[index] = (byte) idConfig.getSeparator().charAt(0);
        index++;
        System.arraycopy(event.getBody(), 0, formatBytes, index, event.getBody().length);
        return formatBytes;
    }

}
