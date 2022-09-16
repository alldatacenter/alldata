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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * HdfsIdConfig
 */
public class HdfsIdConfig {

    public static final String PATTERN_DAY = "{yyyyMMdd}";
    public static final String PATTERN_HOUR = "{yyyyMMddHH}";
    public static final String PATTERN_MINUTE = "{yyyyMMddHHmm}";
    public static final String REGEX_DAY = "\\{yyyyMMdd\\}";
    public static final String REGEX_HOUR = "\\{yyyyMMddHH\\}";
    public static final String REGEX_MINUTE = "\\{yyyyMMddHHmm\\}";
    public static final long HOUR_MS = 60L * 60 * 1000;
    public static final int SEPARATOR_LENGTH = 1;
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
    // format repository
    private static ThreadLocal<Map<String, SimpleDateFormat>> FORMAT_REPOSITORY;

    static {
        FORMAT_REPOSITORY = new ThreadLocal<Map<String, SimpleDateFormat>>() {

            protected Map<String, SimpleDateFormat> initialValue() {
                return new ConcurrentHashMap<String, SimpleDateFormat>();
            }
        };
    }

    private String inlongGroupId;
    private String inlongStreamId;
    private String separator = "|";
    private long partitionIntervalMs = 60 * 60 * 1000;
    // path
    private String idRootPath;
    private String partitionSubPath;
    // hive partition
    private String hiveTableName;
    private String partitionFieldName = "dt";
    private String partitionFieldPattern;
    private String msgTimeFieldPattern;
    // close partition
    private long maxPartitionOpenDelayHour = 8;

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
     * get partitionIntervalMs
     * 
     * @return the partitionIntervalMs
     */
    public long getPartitionIntervalMs() {
        return partitionIntervalMs;
    }

    /**
     * set partitionIntervalMs
     * 
     * @param partitionIntervalMs the partitionIntervalMs to set
     */
    public void setPartitionIntervalMs(long partitionIntervalMs) {
        this.partitionIntervalMs = partitionIntervalMs;
    }

    /**
     * get idRootPath
     * 
     * @return the idRootPath
     */
    public String getIdRootPath() {
        return idRootPath;
    }

    /**
     * set idRootPath
     * 
     * @param idRootPath the idRootPath to set
     */
    public void setIdRootPath(String idRootPath) {
        this.idRootPath = idRootPath;
    }

    /**
     * get partitionSubPath
     * 
     * @return the partitionSubPath
     */
    public String getPartitionSubPath() {
        return partitionSubPath;
    }

    /**
     * set partitionSubPath
     * 
     * @param partitionSubPath the partitionSubPath to set
     */
    public void setPartitionSubPath(String partitionSubPath) {
        this.partitionSubPath = partitionSubPath;
    }

    /**
     * get partitionFieldName
     * 
     * @return the partitionFieldName
     */
    public String getPartitionFieldName() {
        return partitionFieldName;
    }

    /**
     * set partitionFieldName
     * 
     * @param partitionFieldName the partitionFieldName to set
     */
    public void setPartitionFieldName(String partitionFieldName) {
        this.partitionFieldName = partitionFieldName;
    }

    /**
     * get partitionFieldPattern
     * 
     * @return the partitionFieldPattern
     */
    public String getPartitionFieldPattern() {
        partitionFieldPattern = (partitionFieldPattern == null) ? "yyyyMMddHH" : partitionFieldPattern;
        return partitionFieldPattern;
    }

    /**
     * set partitionFieldPattern
     * 
     * @param partitionFieldPattern the partitionFieldPattern to set
     */
    public void setPartitionFieldPattern(String partitionFieldPattern) {
        this.partitionFieldPattern = partitionFieldPattern;
    }

    /**
     * get msgTimeFieldPattern
     * 
     * @return the msgTimeFieldPattern
     */
    public String getMsgTimeFieldPattern() {
        msgTimeFieldPattern = (msgTimeFieldPattern == null) ? "yyyy-MM-dd HH:mm:ss" : msgTimeFieldPattern;
        return msgTimeFieldPattern;
    }

    /**
     * set msgTimeFieldPattern
     * 
     * @param msgTimeFieldPattern the msgTimeFieldPattern to set
     */
    public void setMsgTimeFieldPattern(String msgTimeFieldPattern) {
        this.msgTimeFieldPattern = msgTimeFieldPattern;
    }

    /**
     * get maxPartitionOpenDelayHour
     * 
     * @return the maxPartitionOpenDelayHour
     */
    public long getMaxPartitionOpenDelayHour() {
        return maxPartitionOpenDelayHour;
    }

    /**
     * set maxPartitionOpenDelayHour
     * 
     * @param maxPartitionOpenDelayHour the maxPartitionOpenDelayHour to set
     */
    public void setMaxPartitionOpenDelayHour(long maxPartitionOpenDelayHour) {
        this.maxPartitionOpenDelayHour = maxPartitionOpenDelayHour;
    }

    /**
     * get hiveTableName
     * 
     * @return the hiveTableName
     */
    public String getHiveTableName() {
        return hiveTableName;
    }

    /**
     * set hiveTableName
     * 
     * @param hiveTableName the hiveTableName to set
     */
    public void setHiveTableName(String hiveTableName) {
        this.hiveTableName = hiveTableName;
    }

    /**
     * parsePartitionPath
     * 
     * @param  msgTime
     * @return
     */
    public String parsePartitionPath(long msgTime) {
        Date dtDate = new Date(msgTime - msgTime % partitionIntervalMs);
        String result = partitionSubPath;
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
        return idRootPath + result;
    }

    /**
     * parsePartitionField
     * 
     * @param  msgTime
     * @return
     */
    public String parsePartitionField(long msgTime) {
        SimpleDateFormat format = FORMAT_REPOSITORY.get().get(partitionFieldPattern);
        if (format == null) {
            format = new SimpleDateFormat(this.partitionFieldPattern);
            FORMAT_REPOSITORY.get().put(partitionFieldPattern, format);
        }
        long formatTime = msgTime - msgTime % partitionIntervalMs;
        return format.format(new Date(formatTime));
    }

    /**
     * parseMsgTimeField
     * 
     * @param  msgTime
     * @return
     */
    public String parseMsgTimeField(long msgTime) {
        SimpleDateFormat format = FORMAT_REPOSITORY.get().get(msgTimeFieldPattern);
        if (format == null) {
            format = new SimpleDateFormat(this.msgTimeFieldPattern);
            FORMAT_REPOSITORY.get().put(msgTimeFieldPattern, format);
        }
        return format.format(new Date(msgTime));

    }
}
