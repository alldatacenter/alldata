/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.notification.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Data
@EqualsAndHashCode
@ToString
public class SlaNotificationResult implements Serializable {

    private static final long serialVersionUID = -1L;

    private Boolean status;

    private List<SlaNotificationResultRecord> records;

    public SlaNotificationResult(){
        this.status = false;
    }

    public SlaNotificationResult merge(SlaNotificationResult other){
        this.status = this.status && other.status;
        if (Objects.isNull(records)){
            this.records = other.records;
        }
        if (Objects.nonNull(other) && Objects.nonNull(other.getRecords())){
            this.records.addAll(other.getRecords());
        }
        return this;
    }
}
