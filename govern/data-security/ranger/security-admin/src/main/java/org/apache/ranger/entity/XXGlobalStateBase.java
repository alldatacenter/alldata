/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@MappedSuperclass
@XmlRootElement
public abstract class XXGlobalStateBase extends XXDBBase {
    private static final long serialVersionUID = 1L;

    @Version
    @Column(name = "version")
    protected Long version;

    @Column(name = "state_name")
    protected String stateName;

    @Column(name = "app_data")
    protected String appData;

    public Long getVersion() { return version; }
    public String getStateName() { return stateName; }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public void setAppData(String appData) {this.appData = appData;}
    public String getAppData() { return appData; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }

        XXGlobalStateBase other = (XXGlobalStateBase) obj;

        return Objects.equals(version, other.version) &&
                Objects.equals(stateName, other.stateName) &&
                Objects.equals(appData, other.appData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version, stateName);
    }

    @Override
    public String toString() {
        String str = "XXGlobalStateBase={";
        str += super.toString();
        str += " [version=" + version + ", stateName=" + stateName + ", appData=" + appData + "]";
        str += "}";
        return str;
    }
}

