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
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import org.apache.griffin.core.job.entity.VirtualJob;

/**
 * Measures to publish metrics that processed externally
 */
@Entity
public class ExternalMeasure extends Measure {
    private static final long serialVersionUID = -7551493544224747244L;

    private String metricName;

    @JsonIgnore
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private VirtualJob virtualJob;

    public ExternalMeasure() {
        super();
    }

    public ExternalMeasure(String name, String description, String organization,
                           String owner, String metricName, VirtualJob vj) {
        super(name, description, organization, owner);
        this.metricName = metricName;
        this.virtualJob = vj;
    }

    @JsonProperty("metric.name")
    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public VirtualJob getVirtualJob() {
        return virtualJob;
    }

    public void setVirtualJob(VirtualJob virtualJob) {
        this.virtualJob = virtualJob;
    }

    @Override
    public String getType() {
        return "external";
    }
}
