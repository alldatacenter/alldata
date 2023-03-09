/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.measure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import org.apache.griffin.core.util.JsonUtil;
import org.springframework.util.StringUtils;

@Entity
public class StreamingPreProcess extends AbstractAuditableEntity {
    private static final long serialVersionUID = -7471448761795495384L;

    private String dslType;

    private String inDataFrameName;

    private String outDataFrameName;

    private String rule;

    @JsonIgnore
    @Column(length = 1024)
    private String details;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> detailsMap;

    @JsonProperty(("dsl.type"))
    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    @JsonProperty("in.dataframe.name")
    public String getInDataFrameName() {
        return inDataFrameName;
    }

    public void setInDataFrameName(String inDataFrameName) {
        this.inDataFrameName = inDataFrameName;
    }

    @JsonProperty("out.dataframe.name")
    public String getOutDataFrameName() {
        return outDataFrameName;
    }

    public void setOutDataFrameName(String outDataFrameName) {
        this.outDataFrameName = outDataFrameName;
    }


    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    private String getDetails() {
        return details;
    }

    private void setDetails(String details) {
        this.details = details;
    }

    @JsonProperty("details")
    public Map<String, Object> getDetailsMap() {
        return detailsMap;
    }

    public void setDetailsMap(Map<String, Object> details) {
        this.detailsMap = details;
    }

    @PrePersist
    @PreUpdate
    public void save() throws JsonProcessingException {
        if (detailsMap != null) {
            this.details = JsonUtil.toJson(detailsMap);
        }
    }

    @PostLoad
    public void load() throws IOException {
        if (!StringUtils.isEmpty(details)) {
            this.detailsMap = JsonUtil.toEntity(details,
                new TypeReference<Map<String, Object>>() {
                });
        }
    }

}
