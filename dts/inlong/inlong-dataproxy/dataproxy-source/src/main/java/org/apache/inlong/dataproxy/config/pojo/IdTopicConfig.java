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

package org.apache.inlong.dataproxy.config.pojo;

import org.apache.inlong.sdk.commons.protocol.InlongId;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * IdTopicConfig
 */
public class IdTopicConfig {

    private static final String DEFAULT_PULSAR_TENANT = "public";
    private String uid;
    private String inlongGroupId;
    private String inlongStreamid;
    private String topicName;
    private String tenant;
    private String nameSpace;
    private DataType dataType = DataType.TEXT;
    private String fieldDelimiter = "|";
    private String fileDelimiter = "\n";

    private Map<String, String> params = new HashMap<>();

    public IdTopicConfig() {

    }

    /**
     * get uid
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * get inlongGroupId
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * set inlongGroupId
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
        this.uid = InlongId.generateUid(this.inlongGroupId, this.inlongStreamid);
    }

    /**
     * set inlongGroupId
     * @param inlongGroupId the inlongGroupId to set
     * @param inlongStreamid the inlongStreamid to set
     */
    public void setInlongGroupIdAndStreamId(String inlongGroupId, String inlongStreamid) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamid = inlongStreamid;
        this.uid = InlongId.generateUid(this.inlongGroupId, this.inlongStreamid);
    }

    public String getPulsarTopicName(String clusterTenant, String clusterNameSpace) {
        StringBuilder builder = new StringBuilder(256);
        // build tenant
        if (StringUtils.isBlank(tenant)) {
            if (StringUtils.isBlank(clusterTenant)) {
                builder.append(DEFAULT_PULSAR_TENANT).append("/");
            } else {
                builder.append(clusterTenant).append("/");
            }
        } else {
            builder.append(tenant).append("/");
        }
        // build name space
        if (StringUtils.isBlank(this.nameSpace)) {
            builder.append(clusterNameSpace).append("/");
        } else {
            builder.append(this.nameSpace).append("/");
        }
        // build topic name
        return builder.append(topicName).toString();
    }

    /**
     * get inlongStreamid
     * @return the inlongStreamid
     */
    public String getInlongStreamid() {
        return inlongStreamid;
    }

    /**
     * get topicName
     * @return the topicName
     */
    public String getTopicName() {
        return topicName;
    }

    /**
     * set topicName
     * @param topicName the topicName to set
     */
    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    /**
     * get dataType
     * @return the dataType
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * set dataType
     * @param dataType the dataType to set
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * get fieldDelimiter
     * @return the fieldDelimiter
     */
    public String getFieldDelimiter() {
        return fieldDelimiter;
    }

    /**
     * set fieldDelimiter
     * @param fieldDelimiter the fieldDelimiter to set
     */
    public void setFieldDelimiter(String fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
    }

    /**
     * get fileDelimiter
     * @return the fileDelimiter
     */
    public String getFileDelimiter() {
        return fileDelimiter;
    }

    /**
     * set fileDelimiter
     * @param fileDelimiter the fileDelimiter to set
     */
    public void setFileDelimiter(String fileDelimiter) {
        this.fileDelimiter = fileDelimiter;
    }

    /**
     * set tenant and nameSpace
     *
     * @param tenant  the tenant to set
     * @param nameSpace the nameSpace to set
     */
    public void setTenantAndNameSpace(String tenant, String nameSpace) {
        this.tenant = tenant;
        this.nameSpace = nameSpace;
    }

    /**
     * get params
     * @return the params
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * set params
     * @param params the params to set
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("uid", uid)
                .append("inlongGroupId", inlongGroupId)
                .append("inlongStreamid", inlongStreamid)
                .append("topicName", topicName)
                .append("nameSpace", nameSpace)
                .append("dataType", dataType)
                .append("fieldDelimiter", fieldDelimiter)
                .append("fileDelimiter", fileDelimiter)
                .append("params", params)
                .toString();
    }
}
