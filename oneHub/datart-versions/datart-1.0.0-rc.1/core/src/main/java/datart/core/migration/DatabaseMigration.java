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

package datart.core.migration;

import datart.core.base.consts.Const;
import de.vandermeer.asciitable.AT_Context;
import de.vandermeer.asciitable.AsciiTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DatabaseMigration {

    private static final String OLD_TABLE_NAME = "flyway_schema_history";

    private static final String MIGRATION_TABLE_NAME = "migration_history";

    private static final String BASELINE = "baseline";

    private static final TreeSet<Migration> allMigrations = new TreeSet<>();

    private static final String TABLE_CREATE_SQL = "CREATE TABLE `migration_history`  (" +
            "  `id` varchar(32) NOT NULL," +
            "  `version` varchar(128) NOT NULL," +
            "  `file_name` varchar(255) NOT NULL," +
            "  `execute_user` varchar(128) NOT NULL," +
            "  `execute_date` timestamp NOT NULL," +
            "  `success` tinyint(1) NOT NULL," +
            "  PRIMARY KEY (`id`)" +
            ");";

    private static final String HISTORY_TRANSFER_SQL = "INSERT INTO migration_history ( `id`, `version`, `file_name`, `execute_user`, `execute_date`, `success` ) SELECT " +
            "`version`," +
            "`description`," +
            "`script`," +
            "`installed_by`," +
            "`installed_on`," +
            "`success` " +
            "FROM " +
            "flyway_schema_history";

    private static final String DROP_OLD_TABLE_SQL = "DROP TABLE IF EXISTS `flyway_schema_history`";

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void migration() throws Exception {
        prepareMigrationTable();
        Set<Migration> migrations = getMigrations();
        if (CollectionUtils.isEmpty(migrations)) {
            printCurrentVersion();
            log.info("No migration required , current database version is " + getAllMigrations().last().getVersion());
            return;
        }
        RuntimeException exception = doMigrations((TreeSet<Migration>) migrations);
        printCurrentVersion();
        if (exception != null) {
            throw exception;
        }
    }

    private Set<Migration> getMigrations() throws IOException {
        TreeSet<Migration> migrations = getAllMigrations();
        if (migrations.size() == 0) {
            log.warn("No migration file was found!");
            return Collections.emptySet();
        }
        // query migration histories
        TreeSet<Migration> migrationHistories = new TreeSet<>(jdbcTemplate.query("select * from migration_history", new BeanPropertyRowMapper<>(Migration.class)));
        // filter the migrations that need to do
        Set<Migration> migrationsToDo;
        if (CollectionUtils.isEmpty(migrationHistories)) {
            return migrations;
        } else {
            Migration lastMigration = migrationHistories.last();
            migrationsToDo = migrations.tailSet(lastMigration, !lastMigration.isSuccess());
        }
        return migrationsToDo;
    }

    private RuntimeException doMigrations(TreeSet<Migration> migrations) throws IOException, SQLException, ClassNotFoundException {
        Migration first = migrations.first();
        if (BASELINE.equalsIgnoreCase(first.getVersion())) {
            log.info("The database has no version management , performs an baseline migration");
            if (!doBaseLine(first)) {
                return new RuntimeException("Baseline migration failed! ");
            }
            migrations.remove(first);
        }
        for (Migration migration : migrations) {
            log.info("start migration, version: " + migration.getVersion());
            migration.setExecuteUser(username);
            migration.setExecuteDate(new Date());
            migration.setSuccess(doMigration(migration));
            upsertMigration(migration);
            if (!migration.isSuccess()) {
                return new RuntimeException("Migration break at version  " + migration.getVersion());
            } else {
                log.info("Migration success! version: " + migration.getVersion());
            }
        }
        log.info("The migration is complete , The latest database version is " + migrations.last().getVersion());
        return null;
    }

    private boolean doMigration(Migration migration) {
        try {
            Resource upgradeFile = migration.getUpgradeFile();
            boolean success = runScript(upgradeFile, true);
            if (success) {
                return success;
            }
            log.error("Migration failure! version: " + migration.getVersion() + ". A rollback is about to be performed");
            Resource rollbackFile = migration.getRollbackFile();
            if (rollbackFile != null) {
                runScript(rollbackFile, false);
                log.info("The rollback script (" + rollbackFile.getFilename() + ") is successfully executed");
            } else {
                log.warn("The rollback script does not exist. Skip execution");
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean doBaseLine(Migration baseline) throws SQLException {
        List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
        tables.remove(MIGRATION_TABLE_NAME);
        if (!CollectionUtils.isEmpty(tables)) {
            log.info("Do baseline on an non-empty database...");
            baseline.setSuccess(true);
        } else {
            log.info("Do baseline on an empty database...");
            baseline.setSuccess(runScript(baseline.getUpgradeFile(), true));
        }
        baseline.setExecuteUser(username);
        baseline.setExecuteDate(new Date());
        upsertMigration(baseline);
        return baseline.isSuccess();
    }

    private void prepareMigrationTable() {
        List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
        if (!tables.contains(MIGRATION_TABLE_NAME)) {
            jdbcTemplate.execute(TABLE_CREATE_SQL);
        }
        if (tables.contains(OLD_TABLE_NAME)) {
            try {
                jdbcTemplate.execute(HISTORY_TRANSFER_SQL);
                jdbcTemplate.execute(DROP_OLD_TABLE_SQL);
            } catch (Exception e) {
                log.warn("Migration history transfer error : " + e.getMessage());
            }
        }
    }

    private TreeSet<Migration> getAllMigrations() throws IOException {
        synchronized (this) {
            if (CollectionUtils.isEmpty(allMigrations)) {
                // load migration files
                Resource[] resources = new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + "db/migration/*.sql");
                Map<String, List<Resource>> resourceMap = Arrays.stream(resources)
                        .filter(Migration::isMigrationFile)
                        .collect(Collectors.groupingBy(r -> r.getFilename().substring(1)));

                for (List<Resource> resourceList : resourceMap.values()) {
                    Resource upgradeFile = null, rollbackFile = null;
                    for (Resource resource : resourceList) {
                        if (resource.getFilename().startsWith(ScriptType.UPGRADE.getPrefix())) {
                            upgradeFile = resource;
                        } else if (resource.getFilename().startsWith(ScriptType.ROLLBACK.getPrefix())) {
                            rollbackFile = resource;
                        }
                    }
                    if (upgradeFile != null) {
                        Migration migration = new Migration(upgradeFile, rollbackFile);
                        if (!allMigrations.add(migration)) {
                            throw new RuntimeException("Duplicate version " + migration.getVersion());
                        }
                    }
                }
            }
        }
        return allMigrations;
    }

    private void upsertMigration(Migration migration) throws SQLException {
        SQL query = new SQL();
        query.SELECT("*").FROM(MIGRATION_TABLE_NAME)
                .WHERE("`id` = '" + migration.getId() + "'");
        List<Migration> migrations = jdbcTemplate.query(query.toString(), new BeanPropertyRowMapper<>(Migration.class));
        SQL sql = new SQL();
        if (!CollectionUtils.isEmpty(migrations) && !migrations.get(0).isSuccess()) {
            sql.UPDATE(MIGRATION_TABLE_NAME)
                    .SET("success = " + String.format("'%s'", (migration.isSuccess() ? "1" : "0"))
                            , "execute_date = " + String.format("'%s'", DateFormatUtils.format(new Date(), Const.DEFAULT_DATE_FORMAT)))
                    .WHERE("`id` = " + String.format("'%s'", migration.getId()));
        } else {
            sql.INSERT_INTO(MIGRATION_TABLE_NAME)
                    .INTO_VALUES(String.format("'%s'", migration.getId())
                            , String.format("'%s'", migration.getVersion())
                            , String.format("'%s'", migration.getFileName())
                            , String.format("'%s'", migration.getExecuteUser())
                            , String.format("'%s'", DateFormatUtils.format(migration.getExecuteDate(), Const.DEFAULT_DATE_FORMAT))
                            , String.format("'%s'", migration.isSuccess() ? "1" : "0"));
        }
        jdbcTemplate.execute(sql.toString());
    }

    private boolean runScript(Resource resource, boolean stopOnError) {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            scriptRunner.setAutoCommit(false);
            scriptRunner.setStopOnError(stopOnError);
            scriptRunner.setSendFullScript(false);
            LogWriter logWriter = new LogWriter(System.out);
            if (!stopOnError) {
                scriptRunner.setErrorLogWriter(logWriter);
            }
            scriptRunner.setLogWriter(logWriter);
            scriptRunner.runScript(new InputStreamReader(resource.getInputStream()));
            return true;
        } catch (Exception e) {
            log.error("Script execute failed! " + resource.getFilename());
            return false;
        }
    }

    private void printCurrentVersion() {
        String currentVersion = null, lastVersion = null;
        TreeSet<Migration> migrations = queryMigrationHistory();
        if (!CollectionUtils.isEmpty(migrations)) {
            Migration last = migrations.last();
            while (last != null && !last.isSuccess()) {
                last = migrations.lower(last);
            }
            if (last != null) {
                currentVersion = last.getVersion();
            }
        }

        try {
            TreeSet<Migration> allMigrations = getAllMigrations();
            lastVersion = allMigrations.last().getVersion();
        } catch (Exception e) {
            e.printStackTrace();
        }
        AT_Context at_context = new AT_Context();
        AsciiTable asciiTable = new AsciiTable(at_context);
        asciiTable.addRule();
        asciiTable.addRow("Last Script Version ", lastVersion + "");
        asciiTable.addRule();
        asciiTable.addRow("Current Database Version ", currentVersion + "");
        asciiTable.addRule();
        if (Objects.equals(lastVersion, currentVersion)) {
            System.out.println(asciiTable.render());
        } else {
            System.err.println(asciiTable.render());
        }
    }

    private TreeSet<Migration> queryMigrationHistory() {
        return new TreeSet<>(jdbcTemplate.query("select * from migration_history", new BeanPropertyRowMapper<>(Migration.class)));
    }

    static class LogWriter extends PrintWriter {

        public LogWriter(OutputStream out) {
            super(out);
        }

        @Override
        public void write(String s) {
            log.info(s);
        }
    }

}