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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.UnescapeHelper;

/**
 * 
 * DefaultEvent2IndexRequestHandler
 */
public class DefaultEvent2IndexRequestHandler implements IEvent2IndexRequestHandler {

    public static final String KEY_EXTINFO = "extinfo";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private AtomicLong esIndexIndex = new AtomicLong(System.currentTimeMillis());

    /**
     * parse
     * 
     * @param  context
     * @param  event
     * @return
     */
    @Override
    public EsIndexRequest parse(EsSinkContext context, ProfileEvent event) {
        String uid = event.getUid();
        EsIdConfig idConfig = context.getIdConfig(uid);
        if (idConfig == null) {
            context.addSendResultMetric(event, context.getTaskName(), false, System.currentTimeMillis());
            return null;
        }
        // parse fields
        String delimeter = idConfig.getSeparator();
        char cDelimeter = delimeter.charAt(0);
        String strContext = null;
        // for tab separator
        byte[] bodyBytes = event.getBody();
        int msgLength = event.getBody().length;
        int contentOffset = idConfig.getContentOffset();
        if (contentOffset > 0 && msgLength >= 1) {
            strContext = new String(bodyBytes, contentOffset, msgLength - contentOffset, Charset.defaultCharset());
        } else {
            strContext = new String(bodyBytes, Charset.defaultCharset());
        }
        // unescape
        List<String> columnVlues = UnescapeHelper.toFiledList(strContext, cDelimeter);
        int valueLength = columnVlues.size();
        List<String> fieldList = idConfig.getFieldList();
        int columnLength = fieldList.size();
        // field offset
        int fieldOffset = idConfig.getFieldOffset();
        // get field value
        Map<String, String> fieldMap = new HashMap<>();
        for (int i = fieldOffset; i < columnLength; ++i) {
            String fieldName = fieldList.get(i);
            int columnIndex = i - fieldOffset;
            String fieldValue = columnIndex < valueLength ? columnVlues.get(columnIndex) : "";
            byte[] fieldBytes = fieldValue.getBytes(Charset.defaultCharset());
            if (fieldBytes.length > context.getKeywordMaxLength()) {
                fieldValue = new String(fieldBytes, 0, context.getKeywordMaxLength());
            }
            fieldMap.put(fieldName, fieldValue);
        }

        // ftime
        String ftime = dateFormat.format(new Date(event.getRawLogTime()));
        fieldMap.put("ftime", ftime);
        // extinfo
        String extinfo = getExtInfo(event);
        fieldMap.put("extinfo", extinfo);
        String indexName = idConfig.parseIndexName(event.getRawLogTime());
        // build
        EsIndexRequest indexRequest = new EsIndexRequest(indexName, event);
        if (context.isUseIndexId()) {
            String esIndexId = uid + delimeter + event.getRawLogTime() + delimeter + esIndexIndex.incrementAndGet();
            indexRequest.id(esIndexId);
        }
        indexRequest.source(fieldMap);
        return indexRequest;
    }

    /**
     * getExtInfo
     * 
     * @param  event
     * @return
     */
    public static String getExtInfo(ProfileEvent event) {
        String extinfoValue = event.getHeaders().get(KEY_EXTINFO);
        if (extinfoValue != null) {
            return KEY_EXTINFO + "=" + extinfoValue;
        }
        extinfoValue = KEY_EXTINFO + "=" + event.getHeaders().get(EventConstants.HEADER_KEY_SOURCE_IP);
        return extinfoValue;
    }
}
