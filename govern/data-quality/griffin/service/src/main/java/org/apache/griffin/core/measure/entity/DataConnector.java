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
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Entity
public class DataConnector extends AbstractAuditableEntity {
    private static final long serialVersionUID = -4748881017029815594L;

    private final static Logger LOGGER = LoggerFactory
        .getLogger(DataConnector.class);

    public enum DataType {
        /**
         * There are three data source type which we support now.
         */
        HIVE,
        KAFKA,
        AVRO,
        CUSTOM
    }

    @NotNull
    private String name;

    @Enumerated(EnumType.STRING)
    private DataType type;

    private String version;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String dataFrameName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String dataUnit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String dataTimeZone;

    @JsonIgnore
    @Transient
    private String defaultDataUnit = "365000d";

    @JsonIgnore
    @Column(length = 20480)
    private String config;

    @Transient
    private Map<String, Object> configMap;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
        CascadeType.REMOVE, CascadeType.MERGE})
    @JoinColumn(name = "data_connector_id")
    private List<SegmentPredicate> predicates = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
        CascadeType.REMOVE, CascadeType.MERGE})
    @JoinColumn(name = "pre_process_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<StreamingPreProcess> preProcess;

    public List<SegmentPredicate> getPredicates() {
        return predicates;
    }

    public void setPredicates(List<SegmentPredicate> predicates) {
        this.predicates = predicates;
    }

    @JsonProperty("pre.proc")
    public List<StreamingPreProcess> getPreProcess() {
        return CollectionUtils.isEmpty(preProcess) ? null : preProcess;
    }

    public void setPreProcess(List<StreamingPreProcess> preProcess) {
        this.preProcess = preProcess;
    }

    @JsonProperty("config")
    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }

    private void setConfig(String config) {
        this.config = config;
    }

    private String getConfig() {
        return config;
    }

    @JsonProperty("dataframe.name")
    public String getDataFrameName() {
        return dataFrameName;
    }

    public void setDataFrameName(String dataFrameName) {
        this.dataFrameName = dataFrameName;
    }

    @JsonProperty("data.unit")
    public String getDataUnit() {
        return dataUnit;
    }

    public void setDataUnit(String dataUnit) {
        this.dataUnit = dataUnit;
    }

    @JsonProperty("data.time.zone")
    public String getDataTimeZone() {
        return dataTimeZone;
    }

    public void setDataTimeZone(String dataTimeZone) {
        this.dataTimeZone = dataTimeZone;
    }

    public String getDefaultDataUnit() {
        return defaultDataUnit;
    }

    public void setDefaultDataUnit(String defaultDataUnit) {
        this.defaultDataUnit = defaultDataUnit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (StringUtils.isEmpty(name)) {
            LOGGER.warn("Connector name cannot be empty.");
            throw new NullPointerException();
        }
        this.name = name;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @PrePersist
    @PreUpdate
    public void save() throws JsonProcessingException {
        if (configMap != null) {
            this.config = JsonUtil.toJson(configMap);
        }
    }

    @PostLoad
    public void load() throws IOException {
        if (!StringUtils.isEmpty(config)) {
            this.configMap = JsonUtil.toEntity(config,
                new TypeReference<Map<String, Object>>() {
                });
        }
    }

    public DataConnector() {
    }

    public DataConnector(String name, DataType type, String version,
                         String config, String dataFrameName)
        throws IOException {
        this.name = name;
        this.type = type;
        this.version = version;
        this.config = config;
        this.configMap = JsonUtil.toEntity(config,
            new TypeReference<Map<String, Object>>() {
            });
        this.dataFrameName = dataFrameName;
    }

    public DataConnector(String name, String dataUnit, Map configMap,
                         List<SegmentPredicate> predicates) {
        this.name = name;
        this.dataUnit = dataUnit;
        this.configMap = configMap;
        this.predicates = predicates;
    }

    @Override
    public String toString() {
        return "DataConnector{" +
            "name=" + name +
            "type=" + type +
            ", version='" + version + '\'' +
            ", config=" + config +
            '}';
    }
}
