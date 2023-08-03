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
public abstract class XXRoleBase extends XXDBBase {
    private static final long serialVersionUID = 1L;

    @Version
    @Column(name = "version")
    protected Long version;

    @Column(name = "name")
    protected String name;

    @Column(name = "description")
    protected String description;

    @Column(name = "role_options")
    protected String options;

    @Column(name = "role_text")
    protected String roleText;


    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOptions() { return options; }
    public String getRoleText() { return roleText; }

    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setOptions(String options) {
        this.options = options;
    }
    public void setRoleText(String roleText) {
        this.roleText = roleText;
    }

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

        XXRoleBase other = (XXRoleBase) obj;

        return Objects.equals(version, other.version) &&
                Objects.equals(name, other.name) &&
                Objects.equals(options, other.options) &&
                Objects.equals(roleText, other.roleText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version, name, options, roleText);
    }

    @Override
    public String toString() {
        String str = "XXRoleBase={";
        str += super.toString();
        str += " [version=" + version + ", name=" + name + ", description=" + description + ", options=" + options + ", roleText=" + roleText + "]";
        str += "}";
        return str;
    }
}

