/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.data.provider;

import com.alibaba.fastjson.JSON;
import datart.core.base.PageInfo;
import datart.core.data.provider.sql.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteParam implements Serializable {

    private List<SelectKeyword> keywords;

    private List<SelectColumn> columns;

    private List<AggregateOperator> aggregators;

    private List<FilterOperator> filters;

    private List<GroupByOperator> groups;

    private List<OrderOperator> orders;

    private List<FunctionColumn> functionColumns;

    private Set<SelectColumn> includeColumns;

    private PageInfo pageInfo;

    private boolean serverAggregate;

    private boolean concurrencyOptimize;

    private boolean cacheEnable;

    private int cacheExpires;

    @Override
    public String toString() {
        return JSON.toJSONString(JSON.toJSONString(this));
    }

    public static ExecuteParam empty() {
        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setPageInfo(PageInfo.builder().pageNo(1).pageSize(Integer.MAX_VALUE).build());
        return executeParam;
    }

    public static boolean isEmpty(ExecuteParam executeParam){
        return executeParam == null || (CollectionUtils.isEmpty(executeParam.getAggregators()) &&
                CollectionUtils.isEmpty(executeParam.getColumns()) &&
                CollectionUtils.isEmpty(executeParam.getFunctionColumns()) &&
                CollectionUtils.isEmpty(executeParam.getFilters()) &&
                CollectionUtils.isEmpty(executeParam.getGroups()) &&
                CollectionUtils.isEmpty(executeParam.getKeywords()) &&
                CollectionUtils.isEmpty(executeParam.getOrders()));
    }

}