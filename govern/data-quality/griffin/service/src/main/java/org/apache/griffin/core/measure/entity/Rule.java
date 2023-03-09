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
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.util.JsonUtil;


@Entity
public class Rule extends AbstractAuditableEntity {
    private static final long serialVersionUID = -143019093509759648L;

    /**
     * three type:1.griffin-dsl 2.df-opr 3.spark-sql
     */
    @NotNull
    private String dslType;

    @Enumerated(EnumType.STRING)
    private DqType dqType;

    @Column(length = 8 * 1024)
    @NotNull
    private String rule;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String inDataFrameName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String outDataFrameName;

    @JsonIgnore
    @Column(length = 1024)
    private String details;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> detailsMap;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Map<String, Object>> outList;

    @JsonIgnore
    @Column(name = "\"out\"")
    private String out;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean cache;

    @JsonProperty("dsl.type")
    public String getDslType() {
        return dslType;
    }

    public void setDslType(String dslType) {
        this.dslType = dslType;
    }

    @JsonProperty("dq.type")
    public DqType getDqType() {
        return dqType;
    }

    public void setDqType(DqType dqType) {
        this.dqType = dqType;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
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

    @JsonProperty("details")
    public Map<String, Object> getDetailsMap() {
        return detailsMap;
    }

    public void setDetailsMap(Map<String, Object> detailsMap) {
        this.detailsMap = detailsMap;
    }

    private String getDetails() {
        return details;
    }

    private void setDetails(String details) {
        this.details = details;
    }

    @JsonProperty("out")
    public List<Map<String, Object>> getOutList() {
        return outList;
    }

    public void setOutList(List<Map<String, Object>> outList) {
        this.outList = outList;
    }

    private String getOut() {
        return out;
    }

    private void setOut(String out) {
        this.out = out;
    }

    public Boolean getCache() {
        return cache;
    }

    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    @PrePersist
    @PreUpdate
    public void save() throws JsonProcessingException {
        if (detailsMap != null) {
            this.details = JsonUtil.toJson(detailsMap);
        }
        if (outList != null) {
            this.out = JsonUtil.toJson(outList);
        }
    }

    @PostLoad
    public void load() throws IOException {
        if (!StringUtils.isEmpty(details)) {
            this.detailsMap = JsonUtil.toEntity(
                details, new TypeReference<Map<String, Object>>() {
                });
        }
        if (!StringUtils.isEmpty(out)) {
            this.outList = JsonUtil.toEntity(
                out, new TypeReference<List<Map<String, Object>>>() {
                });
        }
    }

    public Rule() {
    }

    public Rule(String dslType,
                DqType dqType,
                String rule,
                Map<String, Object> detailsMap)
        throws JsonProcessingException {
        this.dslType = dslType;
        this.dqType = dqType;
        this.rule = rule;
        this.detailsMap = detailsMap;
        this.details = JsonUtil.toJson(detailsMap);
    }

    public Rule(String dslType, DqType dqType, String rule,
                String inDataFrameName, String outDataFrameName,
                Map<String, Object> detailsMap,
                List<Map<String, Object>> outList)
        throws JsonProcessingException {
        this(dslType, dqType, rule, detailsMap);
        this.inDataFrameName = inDataFrameName;
        this.outDataFrameName = outDataFrameName;
        this.outList = outList;
    }
}
