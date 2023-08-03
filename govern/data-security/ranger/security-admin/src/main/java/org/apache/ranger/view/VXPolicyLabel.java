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

package org.apache.ranger.view;

/**
 * Policy
 */


import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class VXPolicyLabel extends VXDataObject implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        protected String policyLabel;

        /**
         * Default constructor. This will set all the attributes to default value.
         */
        public VXPolicyLabel() {
        }

        public String getPolicyLabel() {
                return policyLabel;
        }

        public void setPolicyLabel(String policyLabel) {
                this.policyLabel = policyLabel;
        }

        /**
         * This return the bean content in string format
         *
         * @return formatedStr
         */
        public String toString() {
                String str = "VXPolicyLabel={";
                str += super.toString();
                str += "policyLabel={" + policyLabel + "} ";
                str += "}";
                return str;
        }

}
