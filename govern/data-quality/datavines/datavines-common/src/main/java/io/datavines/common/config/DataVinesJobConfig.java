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
package io.datavines.common.config;

import io.datavines.common.utils.StringUtils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class DataVinesJobConfig implements IConfig {

    @JsonProperty("name")
    private String name;

    @JsonProperty("env")
    private EnvConfig envConfig;

    @JsonProperty("sources")
    private List<SourceConfig> sourceParameters;

    @JsonProperty("transforms")
    private List<TransformConfig> transformParameters;

    @JsonProperty("sinks")
    private List<SinkConfig> sinkParameters;

    public DataVinesJobConfig(){}

    public DataVinesJobConfig(String name,
                              EnvConfig envConfig,
                              List<SourceConfig> sourceParameters,
                              List<TransformConfig> transformParameters,
                              List<SinkConfig> sinkParameters) {
        this.name = name;
        this.envConfig = envConfig;
        this.sourceParameters = sourceParameters;
        this.sinkParameters = sinkParameters;
        this.transformParameters = transformParameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvConfig getEnvConfig() {
        return envConfig;
    }

    public void setEnvConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    public List<SourceConfig> getSourceParameters() {
        return sourceParameters;
    }

    public void setSourceParameters(List<SourceConfig> sourceParameters) {
        this.sourceParameters = sourceParameters;
    }

    public List<TransformConfig> getTransformParameters() {
        return transformParameters;
    }

    public void setTransformParameters(List<TransformConfig> transformParameters) {
        this.transformParameters = transformParameters;
    }

    public List<SinkConfig> getSinkParameters() {
        return sinkParameters;
    }

    public void setSinkParameters(List<SinkConfig> sinkParameters) {
        this.sinkParameters = sinkParameters;
    }

    @Override
    public void validate() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "name should not be empty");

        Preconditions.checkArgument(envConfig != null, "env param should not be empty");

        Preconditions.checkArgument(sourceParameters != null, "sources param should not be empty");
        for (SourceConfig sourceParameter : sourceParameters) {
            sourceParameter.validate();
        }

        Preconditions.checkArgument(transformParameters != null, "transform param should not be empty");
        for (TransformConfig transformParameter : transformParameters) {
            transformParameter.validate();
        }

        Preconditions.checkArgument(sinkParameters != null, "sink param should not be empty");
        for (SinkConfig sinkParameter :sinkParameters) {
            sinkParameter.validate();
        }
    }

    @Override
    public String toString() {
        return "DataVinesConfiguration{" +
                "name='" + name + '\'' +
                ", envConfig=" + envConfig +
                ", sourceParameters=" + sourceParameters +
                ", transformParameters=" + transformParameters +
                ", sinkParameters=" + sinkParameters +
                '}';
    }
}
