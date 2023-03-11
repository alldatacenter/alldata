package com.hw.lineage.common.service;

import com.hw.lineage.common.plugin.Plugin;
import com.hw.lineage.common.result.FunctionResult;
import com.hw.lineage.common.result.LineageResult;
import com.hw.lineage.common.result.TableResult;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @description: LinageService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface LineageService extends Plugin {

    /**
     * Parse the field blood relationship of the input SQL
     */
    List<LineageResult> parseFieldLineage(String singleSql);

    /**
     * Execute the single sql
     */
    void execute(String singleSql);

    /**
     * Parse the function name, function format, function main class and description from the jar file
     */
    List<FunctionResult> parseFunction(File file) throws IOException, ClassNotFoundException;

    /**
     * Get the names of all databases in this catalog.
     */
    List<String> listDatabases(String catalogName) throws Exception;

    /**
     * Get names of all tables and views under this database. An empty list is returned if none exists.
     */
    List<String> listTables(String catalogName, String database) throws Exception;

    /**
     * Get names of all views under this database. An empty list is returned if none exists.
     */
    List<String> listViews(String catalogName, String database) throws Exception;

    /**
     * Reads a registered table and returns the tableResult.
     */
    TableResult getTable(String catalogName, String database, String tableName)  throws Exception;

     void dropTable(String catalogName, String database, String tableName) throws Exception;
}
