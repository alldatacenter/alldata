/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class IndexRecords implements Serializable {
    private LinkedHashMap<String, IndexRecord> records;

    public IndexRecords() {
        this.records = new LinkedHashMap<>();
    }

    public Map<String, IndexRecord> getRecords() {
        return records;
    }

    public void setRecords(LinkedHashMap<String, IndexRecord> records) {
        this.records = records;
    }

    @JsonIgnore
    public int size() {
        LinkedHashMap<String, IndexRecord> records = this.records;

        return records != null ? records.size() : 0;
    }

    @JsonIgnore
    public void remove(IndexRecord record) {
        LinkedHashMap<String, IndexRecord> records = this.records;

        if (records != null) {
            records.remove(record.getId());
        }
    }

    @JsonIgnore
    public void add(IndexRecord record) {
        LinkedHashMap<String, IndexRecord> records = this.records;

        if (records == null) {
            records = new LinkedHashMap<>();

            this.records = records;
        }

        records.put(record.getId(), record);
    }

    @JsonIgnore
    public void delete(IndexRecord record) {
        remove(record);
    }
}
