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
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

/**
 * cluster machine for tube cluster. broker/master/standby
 */
@Entity
@Table(name = "cluster",
        uniqueConstraints = @UniqueConstraint(columnNames = {"clusterName"}))
@Data
public class ClusterEntry {
    @Id
    private long clusterId;

    private String clusterName;

    private Date createTime;

    private Date modifyTime;

    private String createUser;

    private int reloadBrokerSize;
}
