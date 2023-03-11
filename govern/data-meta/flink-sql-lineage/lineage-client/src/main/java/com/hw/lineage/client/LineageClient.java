package com.hw.lineage.client;

import com.google.common.collect.Lists;
import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.common.result.FunctionResult;
import com.hw.lineage.common.result.LineageResult;
import com.hw.lineage.common.result.TableResult;
import com.hw.lineage.common.service.LineageService;
import com.hw.lineage.common.util.Preconditions;
import com.hw.lineage.loader.classloading.TemporaryClassLoaderContext;
import com.hw.lineage.loader.plugin.PluginDescriptor;
import com.hw.lineage.loader.plugin.finder.DirectoryBasedPluginFinder;
import com.hw.lineage.loader.plugin.finder.PluginFinder;
import com.hw.lineage.loader.plugin.manager.DefaultPluginManager;
import com.hw.lineage.loader.plugin.manager.PluginManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hw.lineage.common.util.Preconditions.checkArgument;

/**
 * @description: LineageClient
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class LineageClient {

    /**
     * Catalog
     */
    private static final String CREATE_CATALOG_SQL = "CREATE CATALOG %s WITH (%s)";
    private static final String USE_CATALOG_SQL = "USE CATALOG %s";
    private static final String DROP_CATALOG_SQL = "DROP CATALOG IF EXISTS %s";

    /**
     * Database
     */
    private static final String CREATE_DATABASE_SQL = "CREATE DATABASE IF NOT EXISTS %s.`%s` COMMENT %s";
    private static final String USE_DATABASE_SQL = "USE %s.`%s`";
    private static final String DROP_DATABASE_SQL = "DROP DATABASE IF EXISTS %s.`%s`";

    /**
     * Function
     */
    private static final String CREATE_FUNCTION_SQL = "CREATE FUNCTION IF NOT EXISTS %s.`%s`.%s AS '%s' USING JAR '%s'";
    private static final String DROP_FUNCTION_SQL = "DROP FUNCTION IF EXISTS %s.`%s`.%s";


    private final Map<String, LineageService> lineageServiceMap;

    public LineageClient(String path) {
        Map<String, Iterator<LineageService>> pluginIteratorMap = loadPlugins(path);

        this.lineageServiceMap = pluginIteratorMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<LineageService> lineageServiceList = Lists.newArrayList(entry.getValue());
                    checkArgument(lineageServiceList.size() == 1,
                            "%s plugin no implementation of LineageService or greater than 1", entry.getKey());
                    return lineageServiceList.get(0);
                }
        ));
    }

    private Map<String, Iterator<LineageService>> loadPlugins(String path) {
        File pluginRootFolder = new File(path);
        if (!pluginRootFolder.exists()) {
            // for lineage-server-start maven test, user.dir is lineage-server/lineage-server-start, so go back two levels of directories
            pluginRootFolder = new File("../../" + path);
        }
        Path pluginRootFolderPath = pluginRootFolder.toPath();

        PluginFinder descriptorsFactory = new DirectoryBasedPluginFinder(pluginRootFolderPath);
        Collection<PluginDescriptor> descriptors;
        try {
            descriptors = descriptorsFactory.findPlugins();
        } catch (IOException e) {
            throw new LineageException("Exception when trying to initialize plugin system.", e);
        }

        // use AppClassLoader to load
        // String[] parentPatterns = {LineageService.class.getName(), LineageResult.class.getName()};
        String[] parentPatterns = {"com.hw.lineage.common"};

        PluginManager pluginManager =
                new DefaultPluginManager(descriptors, LineageService.class.getClassLoader(), parentPatterns);

        return pluginManager.load(LineageService.class);
    }


    /**
     * Parse the field blood relationship of the input SQL
     */
    public List<LineageResult> parseFieldLineage(String pluginCode, String catalogName, String database, String singleSql) {
        LineageService service = getLineageService(pluginCode);
        try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(service.getClassLoader())) {
            service.execute(String.format(USE_DATABASE_SQL, catalogName, database));
            return service.parseFieldLineage(singleSql);
        }
    }

    /**
     * Execute the single sql
     */
    public void execute(String pluginCode, String singleSql) {
        LineageService service = getLineageService(pluginCode);
        try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(service.getClassLoader())) {
            service.execute(singleSql);
        }
    }

    /**
     * Execute the single sql
     */
    public void execute(String pluginCode, String catalogName, String database, String singleSql) {
        LineageService service = getLineageService(pluginCode);
        try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(service.getClassLoader())) {
            service.execute(String.format(USE_DATABASE_SQL, catalogName, database));
            service.execute(singleSql);
        }
    }

    public List<FunctionResult> parseFunction(String pluginCode, File file) throws IOException, ClassNotFoundException {
        LineageService service = getLineageService(pluginCode);
        try (TemporaryClassLoaderContext ignored = TemporaryClassLoaderContext.of(service.getClassLoader())) {
            return service.parseFunction(file);
        }
    }

    public void createCatalog(String pluginCode, String catalogName, Map<String, String> propertiesMap) {
        String properties = propertiesMap.entrySet()
                .stream()
                .map(entry -> String.format("'%s'='%s'", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","));
        execute(pluginCode, String.format(CREATE_CATALOG_SQL, catalogName, properties));
    }

    public void useCatalog(String pluginCode, String catalogName) {
        execute(pluginCode, String.format(USE_CATALOG_SQL, catalogName));
    }

    public void deleteCatalog(String pluginCode, String catalogName) {
        execute(pluginCode, String.format(DROP_CATALOG_SQL, catalogName));
    }

    public void createDatabase(String pluginCode, String catalogName, String database, String comment) {
        comment = comment == null ? "" : comment;
        execute(pluginCode
                , String.format(CREATE_DATABASE_SQL, catalogName, database, comment)
        );
    }

    public void deleteDatabase(String pluginCode, String catalogName, String database) {
        execute(pluginCode, String.format(DROP_DATABASE_SQL, catalogName, database));
    }

    public void createFunction(String pluginCode, String catalogName, String database, String functionName
            , String className, String functionPath) {
        execute(pluginCode
                , String.format(CREATE_FUNCTION_SQL, catalogName, database, functionName, className, functionPath)
        );
    }

    public void deleteFunction(String pluginCode, String catalogName, String database, String functionName) {
        execute(pluginCode, String.format(DROP_FUNCTION_SQL, catalogName, database, functionName));
    }

    public void deleteTable(String pluginCode, String catalogName, String database, String tableName) throws Exception {
        LineageService service = getLineageService(pluginCode);
        service.dropTable(catalogName, database, tableName);
    }


    private LineageService getLineageService(String pluginCode) {
        LineageService lineageService = lineageServiceMap.get(pluginCode);
        Preconditions.checkNotNull(lineageService, "This plugin %s is not supported.", pluginCode);
        return lineageService;
    }

    /**
     * Get the names of all databases in this catalog.
     */
    public List<String> listDatabases(String pluginCode, String catalogName) throws Exception {
        LineageService service = getLineageService(pluginCode);
        return service.listDatabases(catalogName);
    }

    /**
     * Get names of all tables and views under this database. An empty list is returned if none exists.
     */
    public List<String> listTables(String pluginCode, String catalogName, String database) throws Exception {
        LineageService service = getLineageService(pluginCode);
        return service.listTables(catalogName, database);
    }

    /**
     * Get names of all views under this database. An empty list is returned if none exists.
     */
    public List<String> listViews(String pluginCode, String catalogName, String database) throws Exception {
        LineageService service = getLineageService(pluginCode);
        return service.listViews(catalogName, database);
    }

    /**
     * Reads a registered table and returns the tableResult.
     */
    public TableResult getTable(String pluginCode, String catalogName, String database, String tableName) throws Exception {
        LineageService service = getLineageService(pluginCode);
        return service.getTable(catalogName, database, tableName);
    }
}
