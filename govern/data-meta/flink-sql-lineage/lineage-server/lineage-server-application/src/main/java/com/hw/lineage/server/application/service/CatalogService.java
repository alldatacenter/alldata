package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.result.TableResult;
import com.hw.lineage.server.application.command.catalog.CreateCatalogCmd;
import com.hw.lineage.server.application.command.catalog.CreateDatabaseCmd;
import com.hw.lineage.server.application.command.catalog.CreateTableCmd;
import com.hw.lineage.server.application.command.catalog.UpdateCatalogCmd;
import com.hw.lineage.server.application.dto.CatalogDTO;
import com.hw.lineage.server.application.dto.TableDTO;
import com.hw.lineage.server.domain.query.catalog.CatalogCheck;
import com.hw.lineage.server.domain.query.catalog.CatalogQuery;

import java.util.List;


/**
 * @description: CatalogService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface CatalogService {

    Long createCatalog(CreateCatalogCmd command);

    CatalogDTO queryCatalog(Long catalogId);

    Boolean checkCatalogExist(CatalogCheck catalogCheck);

    PageInfo<CatalogDTO> queryCatalogs(CatalogQuery catalogQuery);

    void deleteCatalog(Long catalogId);

    void updateCatalog(UpdateCatalogCmd command);

    void defaultCatalog(Long catalogId);

    void createDatabase(CreateDatabaseCmd command);

    void deleteDatabase(Long catalogId, String database);

    List<String> queryDatabases(Long catalogId) throws Exception;

    void createTable(CreateTableCmd command);

    void deleteTable(Long catalogId, String database,String tableName)  throws Exception;

    TableResult getTable(Long catalogId, String database, String tableName) throws Exception;

    List<TableDTO> queryTables(Long catalogId, String database) throws Exception;

    /**
     * Create the memory type catalog to flink when the application start
     */
    void createMemoryCatalogs();

}
