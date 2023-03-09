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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.griffin.core.util.JsonUtil;
import org.springframework.util.StringUtils;

/**
 * Measures processed on Griffin
 */
@Entity
public class GriffinMeasure extends Measure {
    public enum ProcessType {
        /**
         * Currently we just support BATCH and STREAMING type
         */
        BATCH,
        STREAMING
    }

    @Enumerated(EnumType.STRING)
    private ProcessType processType;
    private static final long serialVersionUID = -475176898459647661L;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long timestamp;

    @JsonIgnore
    @Column(length = 1024)
    private String ruleDescription;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> ruleDescriptionMap;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
        CascadeType.REMOVE, CascadeType.MERGE})
    @JoinColumn(name = "measure_id")
    private List<DataSource> dataSources = new ArrayList<>();

    @NotNull
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
        CascadeType.REMOVE, CascadeType.MERGE})
    @JoinColumn(name = "evaluate_rule_id")
    private EvaluateRule evaluateRule;

    @JsonProperty("process.type")
    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    @JsonProperty("data.sources")
    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSource> dataSources) {
        if (CollectionUtils.isEmpty(dataSources)) {
            throw new NullPointerException("Data source can not be empty.");
        }
        this.dataSources = dataSources;
    }

    @JsonProperty("evaluate.rule")
    public EvaluateRule getEvaluateRule() {
        return evaluateRule;
    }

    public void setEvaluateRule(EvaluateRule evaluateRule) {
        if (evaluateRule == null || CollectionUtils.isEmpty(evaluateRule
            .getRules())) {
            throw new NullPointerException("Evaluate rule can not be empty.");
        }
        this.evaluateRule = evaluateRule;
    }

    @JsonProperty("rule.description")
    public Map<String, Object> getRuleDescriptionMap() {
        return ruleDescriptionMap;
    }

    public void setRuleDescriptionMap(Map<String, Object> ruleDescriptionMap) {
        this.ruleDescriptionMap = ruleDescriptionMap;
    }


    private String getRuleDescription() {
        return ruleDescription;
    }

    private void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return "griffin";
    }

    public GriffinMeasure() {
        super();
    }

    public GriffinMeasure(String name, String owner,
                          List<DataSource> dataSources,
                          EvaluateRule evaluateRule,
                          List<String> sinksList) {
        this.name = name;
        this.owner = owner;
        this.dataSources = dataSources;
        this.evaluateRule = evaluateRule;
        setSinksList(sinksList);
    }

    public GriffinMeasure(Long measureId, String name, String owner,
                          List<DataSource> dataSources,
                          EvaluateRule evaluateRule) {
        this.setId(measureId);
        this.name = name;
        this.owner = owner;
        this.dataSources = dataSources;
        this.evaluateRule = evaluateRule;
    }

    @PrePersist
    @PreUpdate
    public void save() throws JsonProcessingException {
        super.save();
        if (ruleDescriptionMap != null) {
            this.ruleDescription = JsonUtil.toJson(ruleDescriptionMap);
        }
    }

    @PostLoad
    public void load() throws IOException {
        super.load();
        if (!StringUtils.isEmpty(ruleDescription)) {
            this.ruleDescriptionMap = JsonUtil.toEntity(ruleDescription,
                new TypeReference<Map<String, Object>>() {
                });
        }
    }
}
