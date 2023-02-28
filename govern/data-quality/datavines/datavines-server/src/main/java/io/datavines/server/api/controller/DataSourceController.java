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

import io.datavines.connector.api.ConnectorFactory;
import io.datavines.server.api.dto.vo.Item;
import io.datavines.common.param.TestConnectionRequestParam;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.aop.RefreshToken;
import io.datavines.server.api.dto.bo.datasource.DataSourceCreate;
import io.datavines.server.api.dto.bo.datasource.DataSourceUpdate;
import io.datavines.server.api.dto.bo.datasource.ExecuteRequest;
import io.datavines.server.repository.service.DataSourceService;
import io.datavines.spi.PluginLoader;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(value = "datasource", tags = "datasource", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/datasource", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @ApiOperation(value = "test connection")
    @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object testConnection(@RequestBody TestConnectionRequestParam param)  {
        return dataSourceService.testConnect(param);
    }

    @ApiOperation(value = "create datasource")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createDataSource(@RequestBody DataSourceCreate dataSourceCreate)  {
        return dataSourceService.insert(dataSourceCreate);
    }

    @ApiOperation(value = "update datasource")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object updateDataSource(@RequestBody DataSourceUpdate dataSourceUpdate) {
        return dataSourceService.update(dataSourceUpdate)>0;
    }

    @ApiOperation(value = "delete databases")
    @DeleteMapping(value = "/{id}")
    public Object deleteDataSource(@PathVariable Long id)  {
        return dataSourceService.delete(id);
    }

    @ApiOperation(value = "get datasource page")
    @GetMapping(value = "/page")
    public Object page(@RequestParam(value = "searchVal", required = false) String searchVal,
                       @RequestParam("workSpaceId") Long workSpaceId,
                       @RequestParam("pageNumber") Integer pageNumber,
                       @RequestParam("pageSize") Integer pageSize)  {
        return dataSourceService.getDataSourcePage(searchVal, workSpaceId, pageNumber, pageSize);
    }

    @ApiOperation(value = "get databases")
    @GetMapping(value = "/{id}/databases")
    public Object getDatabaseList(@PathVariable Long id) {
        return dataSourceService.getDatabaseList(id);
    }

    @ApiOperation(value = "get tables")
    @GetMapping(value = "/{id}/{database}/tables")
    public Object getTableList(@PathVariable Long id, @PathVariable String database) {
        return dataSourceService.getTableList(id, database);
    }

    @ApiOperation(value = "get columns")
    @GetMapping(value = "/{id}/{database}/{table}/columns")
    public Object getColumnList(@PathVariable Long id, @PathVariable String database, @PathVariable String table) {
        return dataSourceService.getColumnList(id, database, table);
    }

    @ApiOperation(value = "execute script")
    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object execute(@RequestBody ExecuteRequest param)  {
        return dataSourceService.executeScript(param);
    }

    @ApiOperation(value = "get config json")
    @GetMapping(value = "/config/{type}")
    public Object getConfigJson(@PathVariable String type){
        return dataSourceService.getConfigJson(type);
    }

    @ApiOperation(value = "get connector type list")
    @GetMapping(value = "/type/list")
    public Object getConnectorTypeList() {
        Set<String> connectorList = PluginLoader.getPluginLoader(ConnectorFactory.class).getSupportedPlugins();
        List<Item> items = new ArrayList<>();
        connectorList.forEach(it -> {
            Item item = new Item(it,it);
            items.add(item);
        });

        return items;
    }

    @ApiOperation(value = "get datasource list by workspaceId and type")
    @GetMapping(value = "/list/{workspaceId}/{type}")
    public Object getDataSourceListByType(@PathVariable Long workspaceId, @PathVariable String type) {
        return dataSourceService.listByWorkSpaceIdAndType(workspaceId,type);
    }
}
