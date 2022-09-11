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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 
 * EsIdConfig
 */
public class EsIdConfig {

    public static final String PATTERN_DAY = "{yyyyMMdd}";
    public static final String PATTERN_HOUR = "{yyyyMMddHH}";
    public static final String PATTERN_MINUTE = "{yyyyMMddHHmm}";
    public static final String REGEX_DAY = "\\{yyyyMMdd\\}";
    public static final String REGEX_HOUR = "\\{yyyyMMddHH\\}";
    public static final String REGEX_MINUTE = "\\{yyyyMMddHHmm\\}";
    private static ThreadLocal<SimpleDateFormat> FORMAT_DAY = new ThreadLocal<SimpleDateFormat>() {

        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd");
        }
    };
    private static ThreadLocal<SimpleDateFormat> FORMAT_HOUR = new ThreadLocal<SimpleDateFormat>() {

        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHH");
        }
    };
    private static ThreadLocal<SimpleDateFormat> FORMAT_MINUTE = new ThreadLocal<SimpleDateFormat>() {

        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmm");
        }
    };

    private String inlongGroupId;
    private String inlongStreamId;
    private String separator = "|";
    private String indexNamePattern;
    private String fieldNames;
    private int fieldOffset = 2; // for ftime,extinfo
    private int contentOffset = 0;// except for boss + tab(1)
    private List<String> fieldList;

    /**
     * get inlongGroupId
     * 
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * set inlongGroupId
     * 
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    /**
     * get inlongStreamId
     * 
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * set inlongStreamId
     * 
     * @param inlongStreamId the inlongStreamId to set
     */
    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }

    /**
     * get separator
     * 
     * @return the separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * set separator
     * 
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * get indexNamePattern
     * 
     * @return the indexNamePattern
     */
    public String getIndexNamePattern() {
        return indexNamePattern;
    }

    /**
     * set indexNamePattern
     * 
     * @param indexNamePattern the indexNamePattern to set
     */
    public void setIndexNamePattern(String indexNamePattern) {
        this.indexNamePattern = indexNamePattern;
    }

    /**
     * get fieldOffset
     * 
     * @return the fieldOffset
     */
    public int getFieldOffset() {
        return fieldOffset;
    }

    /**
     * set fieldOffset
     * 
     * @param fieldOffset the fieldOffset to set
     */
    public void setFieldOffset(int fieldOffset) {
        this.fieldOffset = fieldOffset;
    }

    /**
     * get fieldList
     * 
     * @return the fieldList
     */
    public List<String> getFieldList() {
        if (fieldList == null) {
            this.fieldList = new ArrayList<>();
            if (fieldNames != null) {
                String[] fieldNameArray = fieldNames.split("\\s+");
                this.fieldList.addAll(Arrays.asList(fieldNameArray));
            }
        }
        return fieldList;
    }

    /**
     * set fieldList
     * 
     * @param fieldList the fieldList to set
     */
    public void setFieldList(List<String> fieldList) {
        this.fieldList = fieldList;
    }

    /**
     * get fieldNames
     * 
     * @return the fieldNames
     */
    public String getFieldNames() {
        return fieldNames;
    }

    /**
     * set fieldNames
     * 
     * @param fieldNames the fieldNames to set
     */
    public void setFieldNames(String fieldNames) {
        this.fieldNames = fieldNames;
    }

    /**
     * get contentOffset
     * 
     * @return the contentOffset
     */
    public int getContentOffset() {
        return contentOffset;
    }

    /**
     * set contentOffset
     * 
     * @param contentOffset the contentOffset to set
     */
    public void setContentOffset(int contentOffset) {
        this.contentOffset = contentOffset;
    }

    /**
     * parseIndexName
     * 
     * @param  msgTime
     * @return
     */
    public String parseIndexName(long msgTime) {
        Date dtDate = new Date(msgTime);
        String result = indexNamePattern;
        if (result.indexOf(PATTERN_MINUTE) >= 0) {
            String strHour = FORMAT_MINUTE.get().format(dtDate);
            result = result.replaceAll(REGEX_MINUTE, strHour);
        }
        if (result.indexOf(PATTERN_HOUR) >= 0) {
            String strHour = FORMAT_HOUR.get().format(dtDate);
            result = result.replaceAll(REGEX_HOUR, strHour);
        }
        if (result.indexOf(PATTERN_DAY) >= 0) {
            String strHour = FORMAT_DAY.get().format(dtDate);
            result = result.replaceAll(REGEX_DAY, strHour);
        }
        return result;
    }
}
