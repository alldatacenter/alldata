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

package org.apache.inlong.sort.standalone.sink.cls;

import com.tencentcloudapi.cls.producer.common.LogItem;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.apache.inlong.sort.standalone.utils.UnescapeHelper;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default event to logItem handler.
 */
public class DefaultEvent2LogItemHandler implements IEvent2LogItemHandler {

    private static final Logger LOG = InlongLoggerFactory.getLogger(DefaultEvent2LogItemHandler.class);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Parse event to {@literal List<LogItem>} format.
     *
     * @param context Context of CLS sink.
     * @param event Event to be pares to {@literal List<LogItem>}
     * @return Prepared data structure to send.
     */
    @Override
    public List<LogItem> parse(ClsSinkContext context, ProfileEvent event) {
        String uid = event.getUid();
        ClsIdConfig idConfig = context.getIdConfig(uid);
        if (idConfig == null) {
            LOG.error("There is no cls id config for uid {}, discard it", uid);
            context.addSendResultMetric(event, context.getTaskName(), false, System.currentTimeMillis());
            return null;
        }

        // prepare values
        String stringValues = this.getStringValues(event, idConfig);
        char delimiter = idConfig.getSeparator().charAt(0);
        List<String> listValues = UnescapeHelper.toFiledList(stringValues, delimiter);
        listValues.forEach(value -> this.truncateSingleValue(value, context.getKeywordMaxLength()));
        // prepare keys
        List<String> listKeys = idConfig.getFieldList();
        // prepare offset
        int fieldOffset = idConfig.getFieldOffset();
        // convert to LogItem format
        LogItem item = this.parseToLogItem(listKeys, listValues, event.getRawLogTime(), fieldOffset);
        // add ftime
        String ftime = dateFormat.format(new Date(event.getRawLogTime()));
        item.PushBack("ftime", ftime);
        // add extinfo
        String extinfo = this.getExtInfo(event);
        item.PushBack("extinfo", extinfo);

        List<LogItem> itemList = new ArrayList<>();
        itemList.add(item);
        return itemList;
    }

    private String getStringValues(ProfileEvent event, ClsIdConfig idConfig) {
        byte[] bodyBytes = event.getBody();
        int msgLength = event.getBody().length;
        int contentOffset = idConfig.getContentOffset();
        if (contentOffset > 0 && msgLength >= 1) {
            return new String(bodyBytes, contentOffset, msgLength - contentOffset, Charset.defaultCharset());
        } else {
            return new String(bodyBytes, Charset.defaultCharset());
        }
    }

    private LogItem parseToLogItem(List<String> listKeys, List<String> listValues, long time, int fieldOffset) {
        LogItem logItem = new LogItem(time);
        for (int i = fieldOffset; i < listKeys.size(); ++i) {
            String key = listKeys.get(i);
            int columnIndex = i - fieldOffset;
            String value = columnIndex < listValues.size() ? listValues.get(columnIndex) : "";
            logItem.PushBack(key, value);
        }
        return logItem;
    }

    private String truncateSingleValue(String value, int limit) {
        byte[] inBytes = value.getBytes(Charset.defaultCharset());
        if (inBytes.length > limit) {
            value = new String(inBytes, 0, limit);
        }
        return value;
    }

    private String getExtInfo(ProfileEvent event) {
        if (event.getHeaders().size() > 0) {
            StringBuilder sBuilder = new StringBuilder();
            for (Map.Entry<String, String> extInfo : event.getHeaders().entrySet()) {
                String key = extInfo.getKey();
                String value = extInfo.getValue();
                sBuilder.append(key).append('=').append(value).append('&');
            }
            return sBuilder.substring(0, sBuilder.length() - 1);
        }
        return "";
    }

}
