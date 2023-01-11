/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.store;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)

public class StoredServiceResource implements java.io.Serializable {
    private final Map<String, RangerPolicy.RangerPolicyResource> resourceElements;
    private final String                            ownerName;
    private final Map<String, String>               additionalInfo;

    public StoredServiceResource() {
        this(null, null, null);
    }

    public StoredServiceResource(Map<String, RangerPolicy.RangerPolicyResource> resourceElements, String ownerName, Map<String, String> additionalInfo) {
        this.resourceElements = resourceElements;
        this.ownerName        = ownerName;
        this.additionalInfo   = additionalInfo;
    }

    public Map<String, RangerPolicy.RangerPolicyResource> getResourceElements() {
        return resourceElements;
    }
    public String getOwnerName() {
        return ownerName;
    }
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }
}