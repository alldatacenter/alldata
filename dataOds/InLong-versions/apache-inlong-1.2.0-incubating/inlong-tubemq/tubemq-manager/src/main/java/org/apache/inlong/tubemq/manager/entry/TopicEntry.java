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

import java.sql.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "topic")
@Data
@EntityListeners(AuditingEntityListener.class) // support CreationTimestamp annotation
public class TopicEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long businessId;

    @Size(max = 30)
    @NotNull
    private String businessName;

    @Size(max = 64)
    private String messageType;

    @Size(max = 256)
    private String businessCnName;

    @Size(max = 256)
    private String description;

    private String bg;

    @Size(max = 240)
    @NotNull
    private String schemaName;

    @Size(max = 32)
    @NotNull
    private String username;

    @Size(max = 64)
    @NotNull
    private String passwd;

    @Size(max = 64)
    @NotNull
    private String topic;

    @Size(max = 10)
    private String fieldSplitter;

    @Size(max = 256)
    private String predefinedFields;

    private int isHybridDataSource = 0;

    @Size(max = 64)
    @NotNull
    private String encodingType;

    private int isSubSort = 0;

    private String topologyName;

    private String targetServer;

    private String targetServerPort;

    private String netTarget;

    private int status;

    private String category;

    private int clusterId;

    private String inCharge;

    private String sourceServer;

    private String baseDir;

    private Date createTime;

    private String importType;

    private String exampleData;

    @Column(name = "SN")
    private int sn;

    @Size(max = 32)
    private String issueMethod;

    public TopicEntry(String businessName, String schemaName,
                      String username, String passwd, String topic, String encodingType) {
        this.businessName = businessName;
        this.schemaName = schemaName;
        this.username = username;
        this.passwd = passwd;
        this.topic = topic;
        this.encodingType = encodingType;
    }

    public TopicEntry() {

    }
}
