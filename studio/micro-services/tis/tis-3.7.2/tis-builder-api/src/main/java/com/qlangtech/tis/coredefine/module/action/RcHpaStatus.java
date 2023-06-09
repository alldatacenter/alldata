/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.coredefine.module.action;

import java.util.List;

/**
 * k8s 弹性伸缩状态
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-08 10:05
 **/
public class RcHpaStatus {
    private final List<HpaConditionEvent> conditions;
    private final List<RcHpaStatus.HpaMetrics> currentMetrics;

    private HpaAutoscalerStatus autoscalerStatus;
    private HpaAutoscalerSpec autoscalerSpec;

    public RcHpaStatus(List<HpaConditionEvent> conditions, List<HpaMetrics> currentMetrics) {
        this.conditions = conditions;
        this.currentMetrics = currentMetrics;
    }

    public HpaAutoscalerStatus getAutoscalerStatus() {
        return autoscalerStatus;
    }

    public void setAutoscalerStatus(HpaAutoscalerStatus autoscalerStatus) {
        this.autoscalerStatus = autoscalerStatus;
    }

    public HpaAutoscalerSpec getAutoscalerSpec() {
        return autoscalerSpec;
    }

    public void setAutoscalerSpec(HpaAutoscalerSpec autoscalerSpec) {
        this.autoscalerSpec = autoscalerSpec;
    }

    public List<HpaConditionEvent> getConditions() {
        return this.conditions;
    }

    public List<HpaMetrics> getCurrentMetrics() {
        return this.currentMetrics;
    }

    //            [{
//                "type": "Resource",
//                 "resource": {
//                    "name": "cpu",
//                    "currentAverageUtilization": 0,
//                    "currentAverageValue": "1m"
//                }
//            }]
    public static class HpaMetrics {
        private String type;
        private UsingResource resource;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public UsingResource getResource() {
            return resource;
        }

        public void setResource(UsingResource resource) {
            this.resource = resource;
        }
    }

    public static class UsingResource {
        private String name;
        private Object currentAverageUtilization;
        private Object currentAverageValue;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getCurrentAverageUtilization() {
            return currentAverageUtilization;
        }

        public void setCurrentAverageUtilization(Object currentAverageUtilization) {
            this.currentAverageUtilization = currentAverageUtilization;
        }

        public Object getCurrentAverageValue() {
            return currentAverageValue;
        }

        public void setCurrentAverageValue(Object currentAverageValue) {
            this.currentAverageValue = currentAverageValue;
        }
    }


    ////{
    ////                "type": "AbleToScale",
    ////                        "status": "True",
    ////                        "lastTransitionTime": "2021-06-07T03:52:46Z",
    ////                        "reason": "ReadyForNewScale",
    ////                        "message": "recommended		size matches current size "
    ////}
    public static class HpaConditionEvent {
        private String type;
        private String status;
        private String lastTransitionTime;
        private String reason;
        private String message;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLastTransitionTime() {
            return lastTransitionTime;
        }

        public void setLastTransitionTime(String lastTransitionTime) {
            this.lastTransitionTime = lastTransitionTime;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class HpaAutoscalerStatus {

        private Integer currentCPUUtilizationPercentage;

        private Integer currentReplicas;

        private Integer desiredReplicas;

        private long lastScaleTime;

        public Integer getCurrentCPUUtilizationPercentage() {
            return currentCPUUtilizationPercentage;
        }

        public void setCurrentCPUUtilizationPercentage(Integer currentCPUUtilizationPercentage) {
            this.currentCPUUtilizationPercentage = currentCPUUtilizationPercentage;
        }

        public Integer getCurrentReplicas() {
            return currentReplicas;
        }

        public void setCurrentReplicas(Integer currentReplicas) {
            this.currentReplicas = currentReplicas;
        }

        public Integer getDesiredReplicas() {
            return desiredReplicas;
        }

        public void setDesiredReplicas(Integer desiredReplicas) {
            this.desiredReplicas = desiredReplicas;
        }

        public long getLastScaleTime() {
            return lastScaleTime;
        }

        public void setLastScaleTime(long lastScaleTime) {
            this.lastScaleTime = lastScaleTime;
        }
    }

    public static class HpaAutoscalerSpec {
        private Integer maxReplicas;
        private Integer minReplicas;
        private Integer targetCPUUtilizationPercentage;

        public Integer getMaxReplicas() {
            return maxReplicas;
        }

        public void setMaxReplicas(Integer maxReplicas) {
            this.maxReplicas = maxReplicas;
        }

        public Integer getMinReplicas() {
            return minReplicas;
        }

        public void setMinReplicas(Integer minReplicas) {
            this.minReplicas = minReplicas;
        }

        public Integer getTargetCPUUtilizationPercentage() {
            return targetCPUUtilizationPercentage;
        }

        public void setTargetCPUUtilizationPercentage(Integer targetCPUUtilizationPercentage) {
            this.targetCPUUtilizationPercentage = targetCPUUtilizationPercentage;
        }
    }
}
