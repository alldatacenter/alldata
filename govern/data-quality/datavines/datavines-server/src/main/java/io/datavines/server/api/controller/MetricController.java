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
package io.datavines.server.api.controller;

import io.datavines.common.enums.JobType;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.core.utils.LanguageUtils;
import io.datavines.engine.api.engine.EngineExecutor;
import io.datavines.metric.api.*;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.aop.RefreshToken;
import io.datavines.server.api.dto.vo.Item;
import io.datavines.server.api.dto.vo.MetricItem;
import io.datavines.spi.PluginLoader;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.*;

@Api(value = "metric", tags = "metric", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/metric", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class MetricController {

    @ApiOperation(value = "get metric list")
    @GetMapping(value = "/list")
    public Object getMetricList() {
        Set<String> metricList = PluginLoader.getPluginLoader(SqlMetric.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();
        metricList.forEach(it -> {
            SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(it);
            if (sqlMetric != null) {
                Item item = new Item(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()),it);
                items.add(item);
            }
        });

        return items;
    }

    @ApiOperation(value = "get metric list by type")
    @GetMapping(value = "/list/{type}")
    public Object getMetricListByType(@PathVariable("type") String type) {
        Set<String> metricList = PluginLoader.getPluginLoader(SqlMetric.class).getSupportedPlugins();
        List<MetricItem> items = new ArrayList<>();
        JobType jobType = JobType.of(type);
        if (jobType == null) {
            throw new DataVinesServerException(type + "type is not validate");
        }

        switch (jobType) {
            case DATA_QUALITY:
                metricList.forEach(it -> {
                    SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(it);
                    if (sqlMetric != null && sqlMetric.getType().isSingleTable()) {
                        MetricItem item = new MetricItem(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()), it, sqlMetric.getLevel().getDescription());
                        items.add(item);
                    }
                });
                break;
            case DATA_RECONCILIATION:
                metricList.forEach(it -> {
                    SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(it);
                    if (sqlMetric != null && !sqlMetric.getType().isSingleTable()) {
                        MetricItem item = new MetricItem(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()), it, sqlMetric.getLevel().getDescription());
                        items.add(item);
                    }
                });
                break;
            default:
                break;
        }
        return items;
    }

    @ApiOperation(value = "get reconciliation metric list")
    @GetMapping(value = "/reconciliation/list")
    public Object getReconciliationMetricList() {
        Set<String> metricList = PluginLoader.getPluginLoader(SqlMetric.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();

        metricList.forEach(it -> {
            SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(it);
            if (sqlMetric != null && !sqlMetric.getType().isSingleTable()) {
                Item item = new Item(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()),it);
                items.add(item);
            }
        });

        return items;
    }

    @ApiOperation(value = "get quality metric list")
    @GetMapping(value = "/quality/list/{level}")
    public Object getDataQualityMetricList(@NotNull @PathVariable("level") String level) {
        Set<String> metricList = PluginLoader.getPluginLoader(SqlMetric.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();

        metricList.forEach(it -> {
            SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(it);
            if (sqlMetric != null && sqlMetric.getType().isSingleTable() && level.equals(sqlMetric.getLevel().getDescription())) {
                Item item = new Item(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()),it);
                items.add(item);
            }
        });

        return items;
    }

    @ApiOperation(value = "get metric info")
    @GetMapping(value = "/configs/{name}")
    public Object getMetricConfig(@PathVariable("name") String name) {
        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(name);
        if (sqlMetric != null) {
            Map<String, ConfigItem> resultSet = sqlMetric.getConfigMap();
            List<Item> items = new ArrayList<>();
            resultSet.forEach((k,v) -> {
                Item item = new Item(v.getLabel(!LanguageUtils.isZhContext()),k);
                items.add(item);
            });
            return items;
        }

        return null;
    }

    @ApiOperation(value = "get expected value list")
    @GetMapping(value = "/expectedValue/list")
    public Object getExpectedTypeList() {
        Set<String> expectedValueList = PluginLoader.getPluginLoader(ExpectedValue.class).getSupportedPlugins();
        Set<String> afterFilterSet = new HashSet<>();
        expectedValueList.forEach(it -> {
            afterFilterSet.add(it.replace("local_", "").replace("spark_",""));
        });

        List<Item> items = new ArrayList<>();
        afterFilterSet.forEach(it -> {
            ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class).getOrCreatePlugin("local_" + it);
            if (expectedValue != null) {
                Item item = new Item(expectedValue.getNameByLanguage(!LanguageUtils.isZhContext()),it);
                items.add(item);
            }
        });

        return items;
    }

    @ApiOperation(value = "get engine type list")
    @GetMapping(value = "/engine/list")
    public Object getEngineTypeList() {
        Set<String> engineTypeList = PluginLoader.getPluginLoader(EngineExecutor.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();
        engineTypeList.forEach(it -> {
            Item item = new Item(it,it);
            items.add(item);
        });

        return items;
    }

    @ApiOperation(value = "get result formula list")
    @GetMapping(value = "/resultFormula/list")
    public Object getResultFormulaList() {
        Set<String> resultFormulaTypeList = PluginLoader.getPluginLoader(ResultFormula.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();
        resultFormulaTypeList.forEach(it -> {
            ResultFormula resultFormula = PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(it);
            if (resultFormula != null) {
                Item item = new Item(resultFormula.getNameByLanguage(!LanguageUtils.isZhContext()),it);
                items.add(item);
            }
        });

        return items;
    }

    @ApiOperation(value = "get operator list")
    @GetMapping(value = "/operator/list")
    public Object getOperatorList() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("=","eq"));
        items.add(new Item("<","lt"));
        items.add(new Item("<=","lte"));
        items.add(new Item(">","gt"));
        items.add(new Item(">=","gte"));
        items.add(new Item("!=","neq"));
        return items;
    }

}
