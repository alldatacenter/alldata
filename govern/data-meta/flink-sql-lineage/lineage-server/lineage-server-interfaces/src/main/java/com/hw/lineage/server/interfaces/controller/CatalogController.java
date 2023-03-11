package com.hw.lineage.server.interfaces.controller;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.result.TableResult;
import com.hw.lineage.server.application.command.catalog.CreateCatalogCmd;
import com.hw.lineage.server.application.command.catalog.CreateDatabaseCmd;
import com.hw.lineage.server.application.command.catalog.CreateTableCmd;
import com.hw.lineage.server.application.command.catalog.UpdateCatalogCmd;
import com.hw.lineage.server.application.command.function.CreateFunctionCmd;
import com.hw.lineage.server.application.command.function.UpdateFunctionCmd;
import com.hw.lineage.server.application.dto.CatalogDTO;
import com.hw.lineage.server.application.dto.FunctionDTO;
import com.hw.lineage.server.application.dto.TableDTO;
import com.hw.lineage.server.application.service.CatalogService;
import com.hw.lineage.server.application.service.FunctionService;
import com.hw.lineage.server.domain.query.catalog.CatalogCheck;
import com.hw.lineage.server.domain.query.catalog.CatalogQuery;
import com.hw.lineage.server.domain.query.function.FunctionCheck;
import com.hw.lineage.server.domain.query.function.FunctionQuery;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @description: CatalogController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Catalogs API")
@RequestMapping("/catalogs")
public class CatalogController {

    @Resource
    private CatalogService catalogService;

    @Resource
    private FunctionService functionService;

    @GetMapping("/{catalogId}")
    public Result<CatalogDTO> queryCatalog(@PathVariable("catalogId") Long catalogId) {
        CatalogDTO catalogDTO = catalogService.queryCatalog(catalogId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, catalogDTO);
    }

    @GetMapping("")
    public Result<PageInfo<CatalogDTO>> queryCatalogs(CatalogQuery catalogQuery) {
        PageInfo<CatalogDTO> pageInfo = catalogService.queryCatalogs(catalogQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("")
    public Result<Long> createCatalog(@Valid @RequestBody CreateCatalogCmd command) {
        Long catalogId = catalogService.createCatalog(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, catalogId);
    }

    @GetMapping("/exist")
    public Result<Boolean> checkCatalogExist(@Valid CatalogCheck catalogCheck) {
        return Result.success(ResultMessage.CHECK_SUCCESS, catalogService.checkCatalogExist(catalogCheck));
    }

    @PutMapping("/{catalogId}")
    public Result<Boolean> updateCatalog(@PathVariable("catalogId") Long catalogId,
                                         @Valid @RequestBody UpdateCatalogCmd command) {
        command.setCatalogId(catalogId);
        catalogService.updateCatalog(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @PutMapping("/{catalogId}/default")
    public Result<Boolean> defaultPlugin(@PathVariable("catalogId") Long catalogId) {
        catalogService.defaultCatalog(catalogId);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{catalogId}")
    public Result<Boolean> deleteCatalog(@PathVariable("catalogId") Long catalogId) {
        catalogService.deleteCatalog(catalogId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

    @PostMapping("/{catalogId}/databases")
    public Result<Boolean> createDatabase(@PathVariable("catalogId") Long catalogId,
                                          @Valid @RequestBody CreateDatabaseCmd command) {
        command.setCatalogId(catalogId);
        catalogService.createDatabase(command);
        return Result.success(ResultMessage.CREATE_SUCCESS);
    }

    @DeleteMapping("/{catalogId}/databases/{database}")
    public Result<Boolean> deleteCatalog(@PathVariable("catalogId") Long catalogId,
                                         @PathVariable("database") String database) {
        catalogService.deleteDatabase(catalogId, database);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

    @GetMapping("/{catalogId}/databases")
    public Result<List<String>> queryDatabases(@PathVariable("catalogId") Long catalogId) throws Exception {
        List<String> databaseList = catalogService.queryDatabases(catalogId);
        return Result.success(ResultMessage.QUERY_SUCCESS, databaseList);
    }

    @PostMapping("/{catalogId}/databases/{database}/tables")
    public Result<Boolean> createTable(@PathVariable("catalogId") Long catalogId,
                                       @PathVariable("database") String database,
                                       @Valid @RequestBody CreateTableCmd command) {
        command.setCatalogId(catalogId);
        command.setDatabase(database);
        catalogService.createTable(command);
        return Result.success(ResultMessage.CREATE_SUCCESS);
    }

    @GetMapping("/{catalogId}/databases/{database}/tables/{tableName}")
    public Result<TableResult> queryTable(@PathVariable("catalogId") Long catalogId,
                                          @PathVariable("database") String database,
                                          @PathVariable("tableName") String tableName) throws Exception {
        TableResult tableResult = catalogService.getTable(catalogId, database, tableName);
        return Result.success(ResultMessage.DETAIL_SUCCESS, tableResult);
    }


    @GetMapping("/{catalogId}/databases/{database}/tables")
    public Result<List<TableDTO>> queryTables(@PathVariable("catalogId") Long catalogId,
                                              @PathVariable("database") String database) throws Exception {
        List<TableDTO> tableDTOList = catalogService.queryTables(catalogId, database);
        return Result.success(ResultMessage.QUERY_SUCCESS, tableDTOList);
    }

    @DeleteMapping("/{catalogId}/databases/{database}/tables/{tableName}")
    public Result<Boolean> deleteTable(@PathVariable("catalogId") Long catalogId,
                                       @PathVariable("database") String database,
                                       @PathVariable("tableName") String tableName) throws Exception {
        catalogService.deleteTable(catalogId, database, tableName);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

    @GetMapping("/{catalogId}/databases/{database}/functions/{functionId}")
    public Result<FunctionDTO> queryFunction(@PathVariable("catalogId") Long catalogId,
                                             @PathVariable("database") String database,
                                             @PathVariable("functionId") Long functionId) throws Exception {
        FunctionDTO functionDTO = functionService.queryFunction(functionId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, functionDTO);
    }

    @GetMapping("/{catalogId}/databases/{database}/functions")
    public Result<PageInfo<FunctionDTO>> queryFunctions(
            @PathVariable("catalogId") Long catalogId,
            @PathVariable("database") String database,
            FunctionQuery functionQuery) {
        functionQuery.setCatalogId(catalogId);
        functionQuery.setDatabase(database);
        PageInfo<FunctionDTO> pageInfo = functionService.queryFunctions(functionQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("/{catalogId}/databases/{database}/functions")
    public Result<Long> createFunction(
            @PathVariable("catalogId") Long catalogId,
            @PathVariable("database") String database,
            @Valid @RequestBody CreateFunctionCmd command) {
        command.setCatalogId(catalogId);
        command.setDatabase(database);
        Long functionId = functionService.createFunction(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, functionId);
    }

    @GetMapping("/{catalogId}/databases/{database}/functions/exist")
    public Result<Boolean> checkFunctionExist(
            @PathVariable("catalogId") Long catalogId,
            @PathVariable("database") String database,
            @Valid FunctionCheck functionCheck) {
        functionCheck.setCatalogId(catalogId);
        functionCheck.setDatabase(database);
        return Result.success(ResultMessage.CHECK_SUCCESS, functionService.checkFunctionExist(functionCheck));
    }

    @PutMapping("/{catalogId}/databases/{database}/functions/{functionId}")
    public Result<Boolean> updateFunction(@PathVariable("catalogId") Long catalogId,
                                          @PathVariable("database") String database,
                                          @PathVariable("functionId") Long functionId,
                                          @Valid @RequestBody UpdateFunctionCmd command) {
        command.setFunctionId(functionId);
        functionService.updateFunction(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{catalogId}/databases/{database}/functions/{functionId}")
    public Result<Boolean> deleteFunction(@PathVariable("catalogId") Long catalogId,
                                          @PathVariable("database") String database,
                                          @PathVariable("functionId") Long functionId) {
        functionService.deleteFunction(catalogId, database, functionId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

}
