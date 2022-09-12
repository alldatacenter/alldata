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

package org.apache.inlong.dataproxy.config.pojo;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * IdTopicConfig
 */
public class IdTopicConfig {

    private String uid;
    private String inlongGroupId;
    private String inlongStreamid;
    private String topicName;
    private DataType dataType = DataType.TEXT;
    private String fieldDelimiter = "|";
    private String fileDelimiter = "\n";

    /**
     * get uid
     * 
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * generateUid
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @return
     */
    public static String generateUid(String inlongGroupId, String inlongStreamId) {
        if (StringUtils.isBlank(inlongGroupId)) {
            if (StringUtils.isBlank(inlongStreamId)) {
                return "";
            } else {
                return inlongStreamId;
            }
        } else {
            if (StringUtils.isBlank(inlongStreamId)) {
                return inlongGroupId;
            } else {
                return inlongGroupId + "." + inlongStreamId;
            }
        }
    }

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
        this.uid = generateUid(this.inlongGroupId, this.inlongStreamid);
    }

    /**
     * get inlongStreamid
     * 
     * @return the inlongStreamid
     */
    public String getInlongStreamid() {
        return inlongStreamid;
    }

    /**
     * set inlongStreamid
     * 
     * @param inlongStreamid the inlongStreamid to set
     */
    public void setInlongStreamid(String inlongStreamid) {
        this.inlongStreamid = inlongStreamid;
        this.uid = generateUid(this.inlongGroupId, this.inlongStreamid);
    }

    /**
     * get topicName
     * 
     * @return the topicName
     */
    public String getTopicName() {
        return topicName;
    }

    /**
     * set topicName
     * 
     * @param topicName the topicName to set
     */
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    /**
     * get dataType
     * 
     * @return the dataType
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * set dataType
     * 
     * @param dataType the dataType to set
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * get fieldDelimiter
     * 
     * @return the fieldDelimiter
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * set fieldDelimiter
     * 
     * @param fieldDelimiter the fieldDelimiter to set
     */
    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    /**
     * get fileDelimiter
     * 
     * @return the fileDelimiter
     */
    public String getFileDelimiter() {
        return fileDelimiter;
    }

    /**
     * set fileDelimiter
     * 
     * @param fileDelimiter the fileDelimiter to set
     */
    public void setFileDelimiter(String fileDelimiter) {
        this.fileDelimiter = fileDelimiter;
    }

    /**
     * formatTopicName<br>
     * change full topic name "pulsar-9xn9wp35pbxb/test/atta_topic_1" to base topic name "atta_topic_1"<br>
     */
    public void formatTopicName() {
        if (this.topicName != null) {
            int index = this.topicName.lastIndexOf('/');
            if (index >= 0) {
                this.topicName = this.topicName.substring(index + 1);
            }
        }
    }
}
