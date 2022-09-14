/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.vo;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BulkResponse<T> {
    /**
     * Total count
     */
    private int total = 0;

    private Map<String, ResultStatistic<T>> result = new HashMap<>();


    public BulkResponse(){

    }

    public BulkResponse(Function<T, String> aggregateKeyFunc, List<T> resultElements){
        resultElements.forEach(element -> {
            String key = aggregateKeyFunc.apply(element);
            if (StringUtils.isNotBlank(key)){
                result.compute(key, (aggregateKey, statistic) -> {
                    if (null == statistic){
                        statistic = new ResultStatistic<>();
                    }
                    statistic.getData().add(element);
                    return statistic;
                });
            }
        });
        this.total = resultElements.size();
    }
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Map<String, ResultStatistic<T>> getResult() {
        return result;
    }

    public void setResult(Map<String, ResultStatistic<T>> result) {
        this.result = result;
    }

    public static class ResultStatistic<T>{

        /**
         * Result elements
         */
        private List<T> data = new ArrayList<>();

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }

        public int getCount(){
            return this.data.size();
        }
    }
}
