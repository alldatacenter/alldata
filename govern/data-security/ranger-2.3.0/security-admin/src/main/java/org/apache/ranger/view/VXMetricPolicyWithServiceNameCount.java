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
package org.apache.ranger.view;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VXMetricPolicyWithServiceNameCount implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        protected Map<String,VXMetricServiceNameCount> policyCountList = new HashMap<String,VXMetricServiceNameCount>();
        protected long totalCount;

        /**
         * Default constructor. This will set all the attributes to default value.
         */
        public VXMetricPolicyWithServiceNameCount() {
        }

        /**
         * @return the policyCountList
         */
        public Map<String, VXMetricServiceNameCount> getPolicyCountList() {
                return policyCountList;
        }

        /**
         * @param policyCountList the policyCountList to set
         */
        public void setPolicyCountList(Map<String, VXMetricServiceNameCount> policyCountList) {
                this.policyCountList = policyCountList;
        }

        /**
         * @return the totalCount
         */
        public long getTotalCount() {
                return totalCount;
        }

        /**
         * @param totalCount the totalCount to set
         */
        public void setTotalCount(long totalCount) {
                this.totalCount = totalCount;
        }

        @Override
        public String toString() {
                return "VXMetricPolicyWithServiceNameCount={totalCount="
                                + totalCount +", VXMetricPolicyWithServiceNameCount=["
                                + policyCountList.toString()
                                 + "]}";
        }
}
