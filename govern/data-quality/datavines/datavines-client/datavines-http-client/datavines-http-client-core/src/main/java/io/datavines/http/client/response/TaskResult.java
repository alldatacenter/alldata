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
package io.datavines.http.client.response;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class TaskResult implements Serializable {

    private static final long serialVersionUID = -1L;

    private Long id;

    private String metricName;

    private String metricDimension;

    private String metricType;

    private Long taskId;

    private Double actualValue;

    private Double expectedValue;

    private String expectedType;

    private String resultFormula;

    private String operator;

    private Double threshold;

    private String failureStrategy;

    private String state;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricDimension() {
        return metricDimension;
    }

    public void setMetricDimension(String metricDimension) {
        this.metricDimension = metricDimension;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public void setActualValue(Double actualValue) {
        this.actualValue = actualValue;
    }

    public Double getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Double expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
    }

    public String getResultFormula() {
        return resultFormula;
    }

    public void setResultFormula(String resultFormula) {
        this.resultFormula = resultFormula;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(String failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskResult that = (TaskResult) o;
        return Objects.equals(id, that.id) && Objects.equals(metricName, that.metricName) && Objects.equals(metricDimension, that.metricDimension) && Objects.equals(metricType, that.metricType) && Objects.equals(taskId, that.taskId) && Objects.equals(actualValue, that.actualValue) && Objects.equals(expectedValue, that.expectedValue) && Objects.equals(expectedType, that.expectedType) && Objects.equals(resultFormula, that.resultFormula) && Objects.equals(operator, that.operator) && Objects.equals(threshold, that.threshold) && Objects.equals(failureStrategy, that.failureStrategy) && Objects.equals(state, that.state) && Objects.equals(createTime, that.createTime) && Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, metricName, metricDimension, metricType, taskId, actualValue, expectedValue, expectedType, resultFormula, operator, threshold, failureStrategy, state, createTime, updateTime);
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "id=" + id +
                ", metricName='" + metricName + '\'' +
                ", metricDimension='" + metricDimension + '\'' +
                ", metricType='" + metricType + '\'' +
                ", taskId=" + taskId +
                ", actualValue=" + actualValue +
                ", expectedValue=" + expectedValue +
                ", expectedType='" + expectedType + '\'' +
                ", resultFormula='" + resultFormula + '\'' +
                ", operator='" + operator + '\'' +
                ", threshold=" + threshold +
                ", failureStrategy='" + failureStrategy + '\'' +
                ", state='" + state + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
