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

package org.apache.inlong.tubemq.manager.entry;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;
import org.apache.inlong.tubemq.manager.enums.TaskStatusEnum;
import org.apache.inlong.tubemq.manager.utils.ValidateUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "createTopicTask", uniqueConstraints =
        {
                @UniqueConstraint(columnNames = {"id"})
        })
@Data
@EntityListeners(AuditingEntityListener.class)
public class TopicTaskEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clusterId;

    @CreatedDate
    private Date createDate;

    private Integer status = TaskStatusEnum.ADDING.getCode();

    private String token;

    private String topicName;

    @LastModifiedDate
    private Date modifyDate;

    private String modifyUser;

    private Integer configRetryTimes = 0;

    private Integer reloadRetryTimes = 0;

    public boolean legal() {
        return !ValidateUtils.isNull(clusterId);
    }

}
