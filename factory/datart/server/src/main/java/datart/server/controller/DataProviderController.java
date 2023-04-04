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

package datart.server.controller;


import datart.core.data.provider.*;
import datart.server.base.dto.ResponseData;
import datart.server.base.params.ViewExecuteParam;
import datart.server.base.params.TestExecuteParam;
import datart.server.service.DataProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@Api
@RestController
@RequestMapping(value = "/data-provider")
public class DataProviderController extends BaseController {

    private final DataProviderService dataProviderService;

    public DataProviderController(DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    @ApiOperation(value = "get supported data providers")
    @GetMapping(value = "/providers")
    public ResponseData<List<DataProviderInfo>> listSupportedDataProviders() {
        return ResponseData.success(dataProviderService.getSupportedDataProviders());
    }

    @ApiOperation(value = "get data provider config template")
    @GetMapping(value = "/{type}/config/template")
    public ResponseData<DataProviderConfigTemplate> getSourceConfigTemplate(@PathVariable String type) throws IOException {
        return ResponseData.success(dataProviderService.getSourceConfigTemplate(type));
    }

    @ApiOperation(value = "Test data source connection")
    @PostMapping(value = "/test")
    public ResponseData<Object> testConnection(@RequestBody DataProviderSource config) throws Exception {
        return ResponseData.success(dataProviderService.testConnection(config));
    }

    @ApiOperation(value = "List databases")
    @GetMapping(value = "/{sourceId}/databases")
    public ResponseData<Set<String>> listDatabases(@PathVariable String sourceId) throws SQLException {
        checkBlank(sourceId, "sourceId");
        return ResponseData.success(dataProviderService.readAllDatabases(sourceId));
    }

    @ApiOperation(value = "List tables")
    @GetMapping(value = "/{sourceId}/{database}/tables")
    public ResponseData<Set<String>> listTables(@PathVariable String sourceId,
                                                @PathVariable String database) throws SQLException {
        checkBlank(sourceId, "sourceId");
        checkBlank(database, "database");
        return ResponseData.success(dataProviderService.readTables(sourceId, database));
    }

    @ApiOperation(value = "Get table Info")
    @GetMapping(value = "/{sourceId}/{database}/{table}/columns")
    public ResponseData<Set<Column>> getTableInfo(@PathVariable String sourceId,
                                                  @PathVariable String database,
                                                  @PathVariable String table) throws SQLException {
        checkBlank(sourceId, "sourceId");
        checkBlank(database, "database");
        checkBlank(table, "table");
        return ResponseData.success(dataProviderService.readTableColumns(sourceId, database, table));
    }

    @ApiOperation(value = "Execute Script")
    @PostMapping(value = "/execute/test")
    public ResponseData<Dataframe> testExecute(@RequestBody TestExecuteParam executeParam) throws Exception {
        return ResponseData.success(dataProviderService.testExecute(executeParam));
    }

    @ApiOperation(value = "Execute Script")
    @PostMapping(value = "/execute")
    public ResponseData<Dataframe> execute(@RequestBody ViewExecuteParam viewExecuteParam) throws Exception {
        return ResponseData.success(dataProviderService.execute(viewExecuteParam));
    }

    @ApiOperation(value = "get all supported functions for this data source type")
    @PostMapping(value = "/function/support/{sourceId}")
    public ResponseData<Set<StdSqlOperator>> supportedStdFunctions(@PathVariable String sourceId) {
        return ResponseData.success(dataProviderService.supportedStdFunctions(sourceId));
    }

    @ApiOperation(value = "validate sql function")
    @PostMapping(value = "/function/validate")
    public ResponseData<Boolean> validateFunction(@RequestParam String sourceId,
                                                  @RequestParam String snippet) {
        return ResponseData.success(dataProviderService.validateFunction(sourceId, snippet));
    }

}