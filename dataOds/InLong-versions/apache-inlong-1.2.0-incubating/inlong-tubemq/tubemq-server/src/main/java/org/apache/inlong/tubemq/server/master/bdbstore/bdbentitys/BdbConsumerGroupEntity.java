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

package org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;

@Entity
public class BdbConsumerGroupEntity implements Serializable {
    private static final long serialVersionUID = 4395735199580415319L;
    @PrimaryKey
    private String recordKey;
    private String topicName;
    private String consumerGroupName;
    private String createUser;
    private Date createDate;
    private String attributes;

    public BdbConsumerGroupEntity() {

    }

    public BdbConsumerGroupEntity(String topicName, String consumerGroupName,
                                  String createUser, Date createDate) {
        this.recordKey = new StringBuilder(512).append(topicName)
                .append(TokenConstants.ATTR_SEP).append(consumerGroupName).toString();
        this.topicName = topicName;
        this.consumerGroupName = consumerGroupName;
        this.createUser = createUser;
        this.createDate = createDate;
    }

    public String getGroupAttributes() {
        return attributes;
    }

    public void setGroupAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public String getGroupTopicName() {
        return topicName;
    }

    public void setGroupTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setGroupAndTopicName(String groupName, String topicName) {
        this.consumerGroupName = groupName;
        this.topicName = topicName;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }

    public String getRecordCreateUser() {
        return createUser;
    }

    public void setRecordCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public Date getRecordCreateDate() {
        return createDate;
    }

    public void setRecordCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("recordKey", recordKey)
                .append("topicName", topicName)
                .append("consumerGroupName", consumerGroupName)
                .append("createUser", createUser)
                .append("createDate", createDate)
                .append("attributes", attributes)
                .toString();
    }

    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"type\":\"BdbConsumerGroupEntity\",")
                .append("\"recordKey\":\"").append(recordKey)
                .append("\",\"topicName\":\"").append(topicName)
                .append("\",\"consumerGroupName\":\"").append(consumerGroupName)
                .append("\",\"attributes\":\"").append(attributes)
                .append("\",\"createUser\":\"").append(createUser)
                .append("\",\"createDate\":\"")
                .append(DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate))
                .append("\"}");
    }
}
