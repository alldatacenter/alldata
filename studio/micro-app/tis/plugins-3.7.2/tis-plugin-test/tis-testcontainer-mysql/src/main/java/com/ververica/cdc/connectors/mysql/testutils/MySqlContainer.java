/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.connectors.mysql.testutils;

import com.qlangtech.plugins.incr.flink.slf4j.TISLoggerConsumer;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.SplitTableStrategy;
import com.qlangtech.tis.realtime.utils.NetUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Docker container for MySQL. The difference between this class and {@link
 * org.testcontainers.containers.MySQLContainer} is that TC MySQLContainer has problems when
 * overriding mysql conf file, i.e. my.cnf.
 */
@SuppressWarnings("rawtypes")
public class MySqlContainer extends JdbcDatabaseContainer {

    //protected static final int DEFAULT_PARALLELISM = 4;


    private static Logger LOG = LoggerFactory.getLogger(MySqlContainer.class);


    public static final String IMAGE = "mysql";
    public static final String DEFAULT_TAG = "5.7";

    public static final Integer MYSQL_PORT = 3306;

    public static final String INITIAL_DB_SQL = "/docker/setup.sql";

    public static final MySqlContainer MYSQL5_CONTAINER =
            createMysqlContainer(DEFAULT_TAG, "/docker/server-gtids/my.cnf", INITIAL_DB_SQL);
//    public static final MySqlContainer MYSQL8_CONTAINER =
//            createMysqlContainer(VERSION_8, "/docker/server-gtids/my.cnf", "/docker/setup.sql");

    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "MY_CNF";
    private static final String SETUP_SQL_PARAM_NAME = "SETUP_SQL";
    private static final String MYSQL_ROOT_USER = "root";

    private String databaseName = "test";
    private String username = "test";
    private String password = "test";
    private final String imageTag;

    private MySqlContainer() {
        this(DEFAULT_TAG);
    }

    private MySqlContainer(String tag) {
        super(IMAGE + ":" + tag);
        addExposedPort(MYSQL_PORT);
        this.imageTag = tag;
    }

    public static final MySqlContainer createMysqlContainer(String myConf, String sqlClasspath) {
        return createMysqlContainer(DEFAULT_TAG, myConf, sqlClasspath);
    }

    public static final MySqlContainer createMysqlContainer(String tag, String myConf, String sqlClasspath) {
        MySqlContainer container =
                (MySqlContainer) new MySqlContainer(tag)
                        .withConfigurationOverride(myConf)
                        .withSetupSQL(sqlClasspath)
                        .withDatabaseName("flink-test")
                        .withUsername("flinkuser")
                        .withPassword("flinkpw")
                        .withLogConsumer(new TISLoggerConsumer(LOG));

        //container.withCopyFileToContainer(MountableFile.forClasspathResource(myConf), "/etc/mysql/my.cnf");
        //container.withCopyFileToContainer(MountableFile.forClasspathResource(sqlClasspath), "/docker-entrypoint-initdb.d/setup.sql");

//        container.copyFileToContainer(
//                Transferable.of(IOUtils.loadResourceFromClasspath(MySqlContainer.class
//                        , myConf, true, (i) -> org.apache.commons.io.IOUtils.toByteArray(i)))
//                , "/etc/mysql/my.cnf");
//
//        container.copyFileToContainer(
//                Transferable.of(IOUtils.loadResourceFromClasspath(MySqlContainer.class
//                        , sqlClasspath, true, (i) -> org.apache.commons.io.IOUtils.toByteArray(i)))
//                , "/docker-entrypoint-initdb.d/setup.sql");

        return container;
    }

    DataSourceFactory ds;

    public DataSourceFactory createMySqlDataSourceFactory(
            TargetResName dataxName) {
        return this.createMySqlDataSourceFactory(dataxName, true);
    }

    /**
     * @param dataxName
     * @param splitTabStrategy 开启分表策略
     * @return
     */
    public DataSourceFactory createMySqlDataSourceFactory(
            TargetResName dataxName, boolean splitTabStrategy) {
        if (this.ds != null) {
            return this.ds;
        }
        return this.ds = getBasicDataSourceFactory(dataxName, this.imageTag, this, splitTabStrategy);
    }

    public static DataSourceFactory getBasicDataSourceFactory(TargetResName dataxName, String imageTag, JdbcDatabaseContainer container, boolean splitTabStrategy) {
        LOG.info("Starting containers...");
        Startables.deepStart(Stream.of(container)).join();
        LOG.info("Containers are started.");

        Descriptor mySqlV5DataSourceFactory = TIS.get().getDescriptor(imageTag.equals(DEFAULT_TAG) ? "MySQLV5DataSourceFactory" : "MySQLV8DataSourceFactory");
        Assert.assertNotNull("desc of mySqlV5DataSourceFactory can not be null", mySqlV5DataSourceFactory);

        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("name", "mysql");
        formData.addProp("dbName", container.getDatabaseName());
        // formData.addProp("nodeDesc", mySqlContainer.getHost());

        formData.addProp("nodeDesc", NetUtils.getHost());

        if (splitTabStrategy) {
            Descriptor.FormData splitStrategyForm = new Descriptor.FormData();
            splitStrategyForm.addProp("tabPattern", SplitTableStrategy.PATTERN_PHYSICS_TABLE.pattern());
            formData.addSubForm("splitTableStrategy"
                    , "com.qlangtech.tis.plugin.ds.split.DefaultSplitTableStrategy", splitStrategyForm);
        } else {
            formData.addSubForm("splitTableStrategy"
                    , "com.qlangtech.tis.plugin.ds.split.NoneSplitTableStrategy", new Descriptor.FormData());
        }


        formData.addProp("password", container.getPassword());
        formData.addProp("userName", container.getUsername());
        formData.addProp("port", String.valueOf(container.getMappedPort(MYSQL_PORT)));
        formData.addProp("encode", "utf8");
        formData.addProp("useCompression", "true");

        Descriptor.ParseDescribable<DataSourceFactory> parseDescribable
                = mySqlV5DataSourceFactory.newInstance(dataxName.getName(), formData);
        Assert.assertNotNull(parseDescribable.getInstance());

        return parseDescribable.getInstance();
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(MYSQL_PORT));
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(
                MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/", "mysql-default-conf");

        if (parameters.containsKey(SETUP_SQL_PARAM_NAME)) {
            optionallyMapResourceParameterAsVolume(
                    SETUP_SQL_PARAM_NAME, "/docker-entrypoint-initdb.d/", "N/A");
        }

        addEnv("MYSQL_DATABASE", databaseName);
        addEnv("MYSQL_USER", username);
        if (password != null && !password.isEmpty()) {
            addEnv("MYSQL_PASSWORD", password);
            addEnv("MYSQL_ROOT_PASSWORD", password);
        } else if (MYSQL_ROOT_USER.equalsIgnoreCase(username)) {
            addEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes");
        } else {
            throw new ContainerLaunchException(
                    "Empty password can be used only with the root user");
        }
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return "com.mysql.cj.jdbc.Driver";
        } catch (ClassNotFoundException e) {
            return "com.mysql.jdbc.Driver";
        }
    }

    public String getJdbcUrl(String databaseName) {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mysql://"
                + getHost()
                + ":"
                + getDatabasePort()
                + "/"
                + databaseName
                + additionalUrlParams;
    }

    @Override
    public String getJdbcUrl() {
        return getJdbcUrl(databaseName);
    }

    public int getDatabasePort() {
        return getMappedPort(MYSQL_PORT);
    }

    @Override
    protected String constructUrlForConnection(String queryString) {
        String url = super.constructUrlForConnection(queryString);

        if (!url.contains("useSSL=")) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "useSSL=false";
        }

        if (!url.contains("allowPublicKeyRetrieval=")) {
            url = url + "&allowPublicKeyRetrieval=true";
        }

        return url;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @SuppressWarnings("unchecked")
    public MySqlContainer withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return this;
    }

    @SuppressWarnings("unchecked")
    public MySqlContainer withSetupSQL(String sqlPath) {
        parameters.put(SETUP_SQL_PARAM_NAME, sqlPath);
        return this;
    }

    @Override
    public MySqlContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    @Override
    public MySqlContainer withUsername(final String username) {
        this.username = username;
        return this;
    }

    @Override
    public MySqlContainer withPassword(final String password) {
        this.password = password;
        return this;
    }
}
