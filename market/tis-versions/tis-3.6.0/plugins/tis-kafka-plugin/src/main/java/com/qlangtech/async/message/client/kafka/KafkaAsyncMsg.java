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

package com.qlangtech.async.message.client.kafka;

import com.google.common.collect.Maps;
import com.pingcap.ticdc.cdc.TicdcEventData;
import com.pingcap.ticdc.cdc.key.TicdcEventKey;
import com.pingcap.ticdc.cdc.value.TicdcEventColumn;
import com.pingcap.ticdc.cdc.value.TicdcEventRowChange;
import com.qlangtech.tis.async.message.client.consumer.AsyncMsg;
import com.qlangtech.tis.realtime.transfer.DTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: baisui 百岁
 * @create: 2020-12-09 14:33
 **/
public class KafkaAsyncMsg implements AsyncMsg {
    // private final TicdcEventData data;
    // 为了让DefaultTable的validateTable方法通过，这里需要添加一个占位符，其实没有什么用的
    // private static final Map<String, String> beforeValues = Collections.singletonMap("tis_placeholder", "1");


    private final String tableName;
    private final String topicName;
    private final TicdcEventKey ticdcEventKey;
    final boolean update;
    private TicdcEventRowChange value;

    public KafkaAsyncMsg(String topicName, TicdcEventData data) {
        this.topicName = topicName;
        this.ticdcEventKey = data.getTicdcEventKey();
        ticdcEventKey.getTimestamp();
        this.tableName = ticdcEventKey.getTbl();
        value = (TicdcEventRowChange) data.getTicdcEventValue();
        this.update = "u".equals(value.getUpdateOrDelete());
    }

    @Override
    public Object getSource() throws IOException {
        return null;
    }



    @Override
    public Set<String> getFocusTabs() {
        return null;
    }

    @Override
    public String getTopic() {
        return this.topicName;
    }

    @Override
    public String getTag() {
        return this.tableName;
    }

    // @Override
    public DTO getContent() throws IOException {
        DTO dto = new DTO();
        dto.setDbName(this.ticdcEventKey.getScm());
        dto.setEventType(this.update ? DTO.EventType.UPDATE_AFTER : DTO.EventType.ADD);
        dto.setTableName(this.tableName);
        Map<String, Object> after = Maps.newHashMap();
        Map<String, Object> before = Maps.newHashMap();
        for (TicdcEventColumn col : this.value.getColumns()) {
            after.put(col.getName(), String.valueOf(col.getV()));
        }
        dto.setAfter(after);
        List<TicdcEventColumn> oldCols = value.getOldColumns();
        if (oldCols != null) {
            for (TicdcEventColumn col : oldCols) {
                before.put(col.getName(), String.valueOf(col.getV()));
            }
        }

        dto.setBefore(before);
        return dto;
    }

    @Override
    public String getMsgID() {
        return null;
    }

}
