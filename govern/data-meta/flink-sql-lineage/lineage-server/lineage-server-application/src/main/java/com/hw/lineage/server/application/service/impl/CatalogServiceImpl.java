package com.hw.lineage.server.application.service.impl;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.enums.CatalogType;
import com.hw.lineage.common.enums.TableKind;
import com.hw.lineage.common.result.TableResult;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.application.assembler.DtoAssembler;
import com.hw.lineage.server.application.command.catalog.CreateCatalogCmd;
import com.hw.lineage.server.application.command.catalog.CreateDatabaseCmd;
import com.hw.lineage.server.application.command.catalog.CreateTableCmd;
import com.hw.lineage.server.application.command.catalog.UpdateCatalogCmd;
import com.hw.lineage.server.application.dto.CatalogDTO;
import com.hw.lineage.server.application.dto.TableDTO;
import com.hw.lineage.server.application.service.CatalogService;
import com.hw.lineage.server.domain.entity.Catalog;
import com.hw.lineage.server.domain.entity.Plugin;
import com.hw.lineage.server.domain.facade.LineageFacade;
import com.hw.lineage.server.domain.facade.StorageFacade;
import com.hw.lineage.server.domain.query.catalog.CatalogCheck;
import com.hw.lineage.server.domain.query.catalog.CatalogEntry;
import com.hw.lineage.server.domain.query.catalog.CatalogQuery;
import com.hw.lineage.server.domain.repository.CatalogRepository;
import com.hw.lineage.server.domain.repository.PluginRepository;
import com.hw.lineage.server.domain.vo.CatalogId;
import com.hw.lineage.server.domain.vo.PluginId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hw.lineage.common.enums.CatalogType.HIVE;

/**
 * @description: CatalogServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogServiceImpl.class);

    @Resource
    private CatalogRepository catalogRepository;

    @Resource
    private PluginRepository pluginRepository;

    @Resource
    private StorageFacade storageFacade;

    @Resource
    private LineageFacade lineageFacade;

    @Resource
    private DtoAssembler assembler;

    @Override
    public Long createCatalog(CreateCatalogCmd command) {
        Catalog catalog = new Catalog()
                .setPluginId(new PluginId(command.getPluginId()))
                .setCatalogName(command.getCatalogName())
                .setCatalogType(command.getCatalogType())
                .setDefaultDatabase(command.getDefaultDatabase())
                .setDescr(command.getDescr())
                .setCatalogProperties(command.getCatalogProperties())
                .setDefaultCatalog(command.getDefaultCatalog());

        catalog.setCreateTime(System.currentTimeMillis())
                .setModifyTime(System.currentTimeMillis())
                .setInvalid(false);

        createCatalogInEngine(catalog);

        catalog = catalogRepository.save(catalog);
        return catalog.getCatalogId().getValue();
    }

    @Override
    public CatalogDTO queryCatalog(Long catalogId) {
        Catalog catalog = catalogRepository.find(new CatalogId(catalogId));
        return assembler.fromCatalog(catalog);
    }

    @Override
    public Boolean checkCatalogExist(CatalogCheck catalogCheck) {
        return catalogRepository.check(catalogCheck.getCatalogName());
    }

    @Override
    public PageInfo<CatalogDTO> queryCatalogs(CatalogQuery catalogQuery) {
        PageInfo<Catalog> pageInfo = catalogRepository.findAll(catalogQuery);
        return PageUtils.convertPage(pageInfo, assembler::fromCatalog);
    }

    @Override
    public void deleteCatalog(Long catalogId) {
        CatalogId id = new CatalogId(catalogId);
        CatalogEntry entry = catalogRepository.findEntry(id);
        lineageFacade.deleteCatalog(entry.getPluginCode(), entry.getCatalogName());
        catalogRepository.remove(id);
    }

    @Override
    public void updateCatalog(UpdateCatalogCmd command) {
        Catalog catalog = new Catalog()
                .setCatalogId(new CatalogId(command.getCatalogId()))
                .setCatalogName(command.getCatalogName())
                .setDefaultDatabase(command.getDefaultDatabase())
                .setDescr(command.getDescr())
                .setCatalogProperties(command.getCatalogProperties());

        catalog.setModifyTime(System.currentTimeMillis());
        catalogRepository.save(catalog);
    }

    @Override
    public void defaultCatalog(Long catalogId) {
        catalogRepository.setDefault(new CatalogId(catalogId));
    }

    @Override
    public void createDatabase(CreateDatabaseCmd command) {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(command.getCatalogId()));
        lineageFacade.createDatabase(entry.getPluginCode(), entry.getCatalogName()
                , command.getDatabase(), command.getComment()
        );
    }

    @Override
    public void deleteDatabase(Long catalogId, String database) {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(catalogId));
        lineageFacade.deleteDatabase(entry.getPluginCode(), entry.getCatalogName(), database);
    }

    @Override
    public List<String> queryDatabases(Long catalogId) throws Exception {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(catalogId));
        return lineageFacade.listDatabases(entry.getPluginCode(), entry.getCatalogName());
    }

    @Override
    public void createTable(CreateTableCmd command) {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(command.getCatalogId()));
        lineageFacade.createTable(entry.getPluginCode(), entry.getCatalogName()
                , command.getDatabase(), command.getCreateSql()
        );
    }

    @Override
    public void deleteTable(Long catalogId, String database, String tableName) throws Exception {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(catalogId));
        lineageFacade.deleteTable(entry.getPluginCode(), entry.getCatalogName(), database, tableName);
    }

    @Override
    public TableResult getTable(Long catalogId, String database, String tableName) throws Exception {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(catalogId));
        return lineageFacade.getTable(entry.getPluginCode(), entry.getCatalogName(), database, tableName);
    }

    @Override
    public List<TableDTO> queryTables(Long catalogId, String database) throws Exception {
        CatalogEntry entry = catalogRepository.findEntry(new CatalogId(catalogId));
        List<String> tableList = lineageFacade.listTables(entry.getPluginCode(), entry.getCatalogName(), database);
        List<String> viewList = lineageFacade.listViews(entry.getPluginCode(), entry.getCatalogName(), database);

        return tableList.stream().map(tableName -> {
            if (viewList.contains(tableName)) {
                return new TableDTO(tableName, TableKind.VIEW);
            }
            return new TableDTO(tableName, TableKind.TABLE);
        }).collect(Collectors.toList());
    }

    @Override
    public void createMemoryCatalogs() {
        CatalogQuery catalogQuery = new CatalogQuery();
        // when the pageSize parameter is equal to 0, query all data
        catalogQuery.setPageSize(0);
        catalogQuery.setCatalogType(CatalogType.MEMORY);
        PageInfo<Catalog> pageInfo = catalogRepository.findAll(catalogQuery);
        // create memory catalog in flink
        pageInfo.getList().forEach(this::createCatalogInEngine);
    }

    private void createCatalogInEngine(Catalog catalog) {
        Plugin plugin = pluginRepository.find(catalog.getPluginId());
        Map<String, String> propertiesMap = catalog.getPropertiesMap();
        if (catalog.getCatalogType().equals(HIVE)) {
            Arrays.asList("hive-conf-dir", "hadoop-conf-dir").forEach(option ->
                    propertiesMap.computeIfPresent(option, (key, value) -> storageFacade.getParentUri(value))
            );
        }
        lineageFacade.createCatalog(plugin.getPluginCode(), catalog.getCatalogName(), propertiesMap);
        LOG.info("created catalog: [{}] in plugin: [{}]", catalog.getCatalogName(), plugin.getPluginName());
    }
}
