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
package org.apache.ranger.entity;

import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_security_zone_ref_tag_srvc")
public class XXSecurityZoneRefTagService extends XXDBBase implements java.io.Serializable{
        private static final long serialVersionUID = 1L;
	@Id
    @SequenceGenerator(name = "x_sec_zone_ref_tag_srvc_SEQ", sequenceName = "x_sec_zone_ref_tag_srvc_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_sec_zone_ref_tag_srvc_SEQ")
    @Column(name = "id")
    protected Long id;

	@Column(name = "zone_id")
        protected Long zoneId;

	@Column(name = "tag_srvc_id")
        protected Long tagServiceId;

	@Column(name = "tag_srvc_name")
        protected String tagServiceName;

        @Override
        public void setId(Long id) {
                this.id=id;
        }

        @Override
        public Long getId() {
                return id;
        }

        public Long getZoneId() {
                return zoneId;
        }

        public Long getTagServiceId() {
                return tagServiceId;
        }

        public String getTagServiceName() {
                return tagServiceName;
        }

        public void setZoneId(Long zoneId) {
                this.zoneId = zoneId;
        }

        public void setTagServiceId(Long tagServiceId) {
                this.tagServiceId = tagServiceId;
        }

        public void setTagServiceName(String tagServiceName) {
                this.tagServiceName = tagServiceName;
        }

        @Override
        public int hashCode() {
                return Objects.hash(super.hashCode(), id, zoneId, tagServiceId, tagServiceName);
        }

        @Override
        public boolean equals(Object obj) {
                if (this == obj) {
                        return true;
                }

                if (getClass() != obj.getClass()) {
                        return false;
                }

                XXSecurityZoneRefTagService other = (XXSecurityZoneRefTagService) obj;

                return super.equals(obj) &&
                           Objects.equals(id, other.id) &&
                           Objects.equals(zoneId, other.zoneId) &&
                           Objects.equals(tagServiceId, other.tagServiceId) &&
                           Objects.equals(tagServiceName, other.tagServiceName);
        }

        @Override
        public String toString() {
                return "XXSecurityZoneRefTagService [id=" + id + ", zoneId=" + zoneId + ", tagServiceId=" + tagServiceId
                                + ", tagServiceName=" + tagServiceName + "]";
        }
}
