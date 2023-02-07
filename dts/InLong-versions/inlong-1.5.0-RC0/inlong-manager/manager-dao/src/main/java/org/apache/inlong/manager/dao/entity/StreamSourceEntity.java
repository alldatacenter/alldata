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

package org.apache.inlong.manager.dao.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.util.MaskDataUtils;

/**
 * Stream source entity, including source type, source name, etc.
 */
@Data
public class StreamSourceEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String inlongGroupId;
    private String inlongStreamId;
    private String sourceType;
    private String sourceName;
    private Integer templateId;
    private String agentIp;
    private String uuid;

    private String dataNodeName;
    private String inlongClusterName;
    private String inlongClusterNodeGroup;
    private String serializationType;
    private String snapshot;
    private Date reportTime;

    // extParams saved filePath, fileRollingType, dbName, tableName, etc.
    private String extParams;

    private Integer version;
    private Integer status;
    private Integer previousStatus;
    private Integer isDeleted;
    private String creator;
    private String modifier;
    private Date createTime;
    private Date modifyTime;

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(extParams)) {
            StringBuilder buffer = new StringBuilder(extParams);
            MaskDataUtils.mask(buffer);
            extParams = buffer.toString();
        }
        return "StreamSourceEntity{"
                + "id=" + id
                + ", inlongGroupId='" + inlongGroupId + '\''
                + ", inlongStreamId='" + inlongStreamId + '\''
                + ", sourceType='" + sourceType + '\''
                + ", sourceName='" + sourceName + '\''
                + ", templateId=" + templateId
                + ", agentIp='" + agentIp + '\''
                + ", uuid='" + uuid + '\''
                + ", dataNodeName='" + dataNodeName + '\''
                + ", inlongClusterName='" + inlongClusterName + '\''
                + ", serializationType='" + serializationType + '\''
                + ", snapshot='" + snapshot + '\''
                + ", reportTime=" + reportTime
                + ", extParams='" + extParams + '\''
                + ", version=" + version
                + ", status=" + status
                + ", previousStatus=" + previousStatus
                + ", isDeleted=" + isDeleted
                + ", creator='" + creator + '\''
                + ", modifier='" + modifier + '\''
                + ", createTime=" + createTime + '\''
                + ", modifyTime=" + modifyTime
                + '}';
    }
}