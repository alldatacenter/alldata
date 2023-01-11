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

import java.io.Serializable;

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
@Table(name = "x_policy_label")
public class XXPolicyLabel extends XXDBBase implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * id of the XXPolicyLabel
         * <ul>
         * </ul>
         *
         */
        @Id
        @SequenceGenerator(name = "X_POLICY_LABEL_SEQ", sequenceName = "X_POLICY_LABEL_SEQ", allocationSize = 1)
        @GeneratedValue(strategy = GenerationType.AUTO, generator = "X_POLICY_LABEL_SEQ")
        @Column(name = "id")
        protected Long id;

        /**
         * Global Id for the object
         * <ul>
         * <li>The maximum length for this attribute is <b>512</b>.
         * </ul>
         *
         */
        @Column(name = "guid", unique = true, nullable = false, length = 512)
        protected String guid;

        /**
         * policyLabel of the XXPolicyLabel
         * <ul>
         * </ul>
         *
         */
        @Column(name = "label_name")
        protected String policyLabel;


        public void setId(Long id) {
                this.id = id;
        }

        public Long getId() {
                return id;
        }

        /**
         * @return the gUID
         */
        public String getGuid() {
                return guid;
        }

        /**
         * @param gUID
         *            the gUID to set
         */
        public void setGuid(String gUID) {
                guid = gUID;
        }

        /**
         * @param policyLabel
         *            the policyLabel to set
         */
        public void setPolicyLabel(String policyLabel) {
                this.policyLabel = policyLabel;
        }

        /**
         * @return the policyLabel
         */
        public String getPolicyLabel() {
                return policyLabel;
        }

        @Override
        public boolean equals(Object obj) {
                if (this == obj)
                        return true;
                if (!super.equals(obj))
                        return false;
                if (getClass() != obj.getClass())
                        return false;
                XXPolicyLabel other = (XXPolicyLabel) obj;
                if (id == null) {
                        if (other.id != null)
                                return false;
                } else if (!id.equals(other.id))
                        return false;
                if (guid == null) {
                        if (other.guid != null) {
                                return false;
                        }
                } else if (!guid.equals(other.guid)) {
                        return false;
                }
                if (policyLabel == null) {
                        if (other.policyLabel != null) {
                                return false;
                        }
                } else if (!policyLabel.equals(other.policyLabel)) {
                        return false;
                }
                return true;
        }

        @Override
        public String toString() {
                String str = "XXPolicyLabel={[id=" + id + ", ";
                str += super.toString();
                str += " , guid=" + guid + ", policyLabel=" + policyLabel + "]";
                str += "}";
                return str;
        }
}
