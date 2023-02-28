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
package io.datavines.metric.api;

import io.datavines.common.enums.OperatorType;
import io.datavines.spi.PluginLoader;

import java.math.BigDecimal;

import static io.datavines.common.enums.OperatorType.EQ;

public class MetricValidator {

    /**
     * It is used to judge whether the result of the data quality task is failed
     * @return boolean
     */
    public static boolean isSuccess(MetricExecutionResult executionResult) {

        Double actualValue = executionResult.getActualValue();
        Double expectedValue = executionResult.getExpectedValue();

        OperatorType operatorType = OperatorType.of(executionResult.getOperator());

        ResultFormula resultFormula = PluginLoader.getPluginLoader(ResultFormula.class)
                .getOrCreatePlugin(executionResult.getResultFormula());
        return getCompareResult(operatorType,
                resultFormula.getResult(actualValue, expectedValue),
                executionResult.getThreshold());
    }

    private static boolean getCompareResult(OperatorType operatorType, Double srcValue, Double targetValue) {
        if (srcValue == null || targetValue == null) {
            return false;
        }

        BigDecimal src = BigDecimal.valueOf(srcValue);
        BigDecimal target = BigDecimal.valueOf(targetValue);
        switch (operatorType) {
            case EQ:
                return src.compareTo(target) == 0;
            case LT:
                return src.compareTo(target) <= -1;
            case LTE:
                return src.compareTo(target) == 0 || src.compareTo(target) <= -1;
            case GT:
                return src.compareTo(target) >= 1;
            case GTE:
                return src.compareTo(target) == 0 || src.compareTo(target) >= 1;
            case NE:
                return src.compareTo(target) != 0;
            default:
                return true;
        }
    }
}
