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

package org.apache.seatunnel.datasource.plugin.starrocks;

import org.apache.seatunnel.api.table.catalog.PrimaryKey;
import org.apache.seatunnel.api.table.catalog.TablePath;
import org.apache.seatunnel.api.table.catalog.exception.CatalogException;
import org.apache.seatunnel.api.table.catalog.exception.DatabaseNotExistException;
import org.apache.seatunnel.api.table.catalog.exception.TableNotExistException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class StarRocksCatalog {

    protected final String catalogName;
    protected final String username;
    protected final String pwd;
    protected final String baseUrl;
    protected final String defaultUrl;

    private static final Set<String> SYS_DATABASES = new HashSet<>();
    private static final Logger LOG = LoggerFactory.getLogger(StarRocksCatalog.class);

    static {
        SYS_DATABASES.add("information_schema");
        SYS_DATABASES.add("_statistics_");
    }

    public StarRocksCatalog(String catalogName, String username, String pwd, String defaultUrl) {

        checkArgument(StringUtils.isNotBlank(username));
        checkArgument(StringUtils.isNotBlank(pwd));
        checkArgument(StringUtils.isNotBlank(defaultUrl));

        defaultUrl = defaultUrl.trim();
        this.catalogName = catalogName;
        this.username = username;
        this.pwd = pwd;
        this.defaultUrl = defaultUrl;
        if (validateJdbcUrlWithDatabase(defaultUrl)) {
            this.baseUrl = splitDefaultUrl(defaultUrl);
        } else {
            this.baseUrl = defaultUrl.endsWith("/") ? defaultUrl : defaultUrl + "/";
        }
    }

    public List<String> listDatabases() throws CatalogException {
        List<String> databases = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(defaultUrl, username, pwd);
                PreparedStatement ps = conn.prepareStatement("SHOW DATABASES;");
                ResultSet rs = ps.executeQuery(); ) {

            while (rs.next()) {
                String databaseName = rs.getString(1);
                if (!SYS_DATABASES.contains(databaseName)) {
                    databases.add(rs.getString(1));
                }
            }

            return databases;
        } catch (Exception e) {
            throw new CatalogException(
                    String.format("Failed listing database in catalog %s", this.catalogName), e);
        }
    }

    public List<String> listTables(String databaseName)
            throws CatalogException, DatabaseNotExistException {
        if (!databaseExists(databaseName)) {
            throw new DatabaseNotExistException(this.catalogName, databaseName);
        }

        try (Connection conn = DriverManager.getConnection(baseUrl + databaseName, username, pwd);
                PreparedStatement ps = conn.prepareStatement("SHOW TABLES;");
                ResultSet rs = ps.executeQuery()) {

            List<String> tables = new ArrayList<>();

            while (rs.next()) {
                tables.add(rs.getString(1));
            }

            return tables;
        } catch (Exception e) {
            throw new CatalogException(
                    String.format("Failed listing database in catalog %s", catalogName), e);
        }
    }

    public List<TableField> getTable(TablePath tablePath)
            throws CatalogException, TableNotExistException {
        if (!tableExists(tablePath)) {
            throw new TableNotExistException(catalogName, tablePath);
        }

        String dbUrl = baseUrl + tablePath.getDatabaseName();
        try (Connection conn = DriverManager.getConnection(dbUrl, username, pwd);
                PreparedStatement statement =
                        conn.prepareStatement(
                                String.format(
                                        "SELECT * FROM %s WHERE 1 = 0;",
                                        String.format(
                                                "`%s`.`%s`",
                                                tablePath.getDatabaseName(),
                                                tablePath.getTableName()))); ) {

            Optional<PrimaryKey> primaryKey =
                    getPrimaryKey(tablePath.getDatabaseName(), tablePath.getTableName());

            ResultSetMetaData tableMetaData = statement.getMetaData();

            List<TableField> fields = new ArrayList<>();
            for (int i = 1; i <= tableMetaData.getColumnCount(); i++) {
                TableField tableField = new TableField();
                tableField.setName(tableMetaData.getColumnName(i));
                tableField.setType(tableMetaData.getColumnTypeName(i));
                tableField.setComment(tableMetaData.getColumnLabel(i));
                tableField.setNullable(
                        tableMetaData.isNullable(i) == ResultSetMetaData.columnNullable);
                tableField.setPrimaryKey(
                        primaryKey.isPresent()
                                && primaryKey
                                        .get()
                                        .getColumnNames()
                                        .contains(tableField.getName()));
                // TODO add default value
                tableField.setDefaultValue(null);
                fields.add(tableField);
            }
            return fields;
        } catch (Exception e) {
            throw new CatalogException(
                    String.format("Failed getting table %s", tablePath.getFullName()), e);
        }
    }

    /**
     * @return The array size is fixed at 2, index 0 is base url, and index 1 is default database.
     */
    public static String splitDefaultUrl(String defaultUrl) {
        int index = defaultUrl.lastIndexOf("/") + 1;
        return defaultUrl.substring(0, index);
    }

    protected Optional<PrimaryKey> getPrimaryKey(String schema, String table) throws SQLException {

        List<String> pkFields = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(defaultUrl, username, pwd);
                PreparedStatement statement =
                        conn.prepareStatement(
                                String.format(
                                        "SELECT COLUMN_NAME FROM information_schema.columns where TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s' AND COLUMN_KEY = 'PRI' ORDER BY ORDINAL_POSITION",
                                        schema, table));
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                pkFields.add(columnName);
            }
        }
        if (!pkFields.isEmpty()) {
            // PK_NAME maybe null according to the javadoc, generate a unique name in that case
            String pkName = "pk_" + String.join("_", pkFields);
            return Optional.of(PrimaryKey.of(pkName, pkFields));
        }
        return Optional.empty();
    }

    public boolean databaseExists(String databaseName) throws CatalogException {
        checkArgument(StringUtils.isNotBlank(databaseName));

        return listDatabases().contains(databaseName);
    }

    /**
     * URL has to be with database, like "jdbc:mysql://localhost:5432/db" rather than
     * "jdbc:mysql://localhost:5432/".
     */
    @SuppressWarnings("MagicNumber")
    public static boolean validateJdbcUrlWithDatabase(String url) {
        String[] parts = url.trim().split("\\/+");
        return parts.length == 3;
    }

    public boolean tableExists(TablePath tablePath) throws CatalogException {
        try {
            return databaseExists(tablePath.getDatabaseName())
                    && listTables(tablePath.getDatabaseName()).contains(tablePath.getTableName());
        } catch (DatabaseNotExistException e) {
            return false;
        }
    }
}
