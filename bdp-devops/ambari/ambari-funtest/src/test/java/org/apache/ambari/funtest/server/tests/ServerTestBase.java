/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.funtest.server.tests;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Properties;

import org.apache.ambari.funtest.server.LocalAmbariServer;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Base test infrastructure.
 */
public class ServerTestBase {
    private static Log LOG = LogFactory.getLog(ServerTestBase.class);

    /**
     * Run the ambari server on a thread.
     */
    protected static Thread serverThread = null;

    /**
     * Instance of the local ambari server, which wraps the actual
     * ambari server with test configuration.
     */
    protected static LocalAmbariServer server = null;

    /**
     * Server port
     */
    protected static int serverPort = 9995;

    /**
     * Server agent port
     */
    protected static int serverAgentPort = 9000;

    /**
     * Guice injector using an in-memory DB.
     */
    protected static Injector injector = null;

    /**
     * Server URL
     */
    protected static String SERVER_URL_FORMAT = "http://localhost:%d";

    /**
     * Initialize the AmbariServer and database once for the entire
     * duration of the tests since AmbariServer is a singleton.
     */
    private static boolean isInitialized;

    /**
     * Create and populate the DB. Start the AmbariServer.
     * @throws Exception
     */
    @BeforeClass
    public static void setupTest() throws Exception {
        if (!isInitialized) {
            Properties properties = new Properties();
            properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "remote");
            properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), Configuration.JDBC_IN_MEMORY_URL);
            properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), Configuration.JDBC_IN_MEMORY_DRIVER);
            properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), "src/test/resources/stacks");
            properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), "src/test/resources/version");
            properties.setProperty(Configuration.OS_VERSION.getKey(), "centos6");
            properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");

            properties.setProperty(Configuration.AGENT_USE_SSL.getKey(), "false");
            properties.setProperty(Configuration.CLIENT_API_PORT.getKey(), Integer.toString(serverPort));
            properties.setProperty(Configuration.SRVR_ONE_WAY_SSL_PORT.getKey(), Integer.toString(serverAgentPort));
            String tmpDir = System.getProperty("java.io.tmpdir");
            properties.setProperty(Configuration.SRVR_KSTR_DIR.getKey(), tmpDir);

            ControllerModule testModule = new ControllerModule(properties);

            injector = Guice.createInjector(testModule);
            injector.getInstance(PersistService.class).start();
            initDB();

            server = injector.getInstance(LocalAmbariServer.class);
            serverThread = new Thread(server);
            serverThread.start();
            waitForServer();

            isInitialized = true;
        }
    }

    /**
     * Creates the basic authentication string for admin:admin
     *
     * @return
     */
    protected static String getBasicAdminAuthentication() {
        String authString = getAdminUserName() + ":" + getAdminPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);

        return "Basic " + authStringEnc;
    }

    /**
     * Creates the DB and populates it.
     *
     * @throws IOException
     * @throws SQLException
     */
    protected static void initDB() throws IOException, SQLException {
        createSourceDatabase();
    }

    /**
     * Drops the Derby DB.
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    protected static void dropDatabase() throws ClassNotFoundException, SQLException {
        String DROP_DERBY_URL = "jdbc:derby:memory:myDB/ambari;drop=true";
        Class.forName(Configuration.JDBC_IN_MEMORY_DRIVER);
        try {
            DriverManager.getConnection(DROP_DERBY_URL);
        } catch (SQLNonTransientConnectionException ignored) {
            LOG.info("Database dropped ", ignored); //error 08006 expected
        }
    }

    /**
     * Executes Ambari-DDL-Derby-CREATE.sql
     *
     * @throws IOException
     * @throws SQLException
     */
    private static void createSourceDatabase() throws IOException, SQLException {
        //create database
        File projectDir = new File(System.getProperty("user.dir"));
        File ddlFile = new File(projectDir.getParentFile(), "ambari-server/src/main/resources/Ambari-DDL-Derby-CREATE.sql");
        String ddlFilename = ddlFile.getPath();
        DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);
        dbAccessor.executeScript(ddlFilename);
    }

    /**
     * Gets the default administration user name
     *
     * @return
     */
    protected static String getAdminUserName() {
        return "admin";
    }

    /**
     * Gets the default administrator password
     *
     * @return
     */
    protected static String getAdminPassword() {
        return "admin";
    }

    /**
     * Waits for the local server until it is ready to accept requests.
     *
     * @throws Exception
     */
    private static void waitForServer() throws Exception {
        int count = 1;

        while (!isServerUp()) {
            serverThread.join(count * 10000);     // Give a few seconds for the ambari server to start up
            //count += 1; // progressive back off
            //count *= 2; // exponential back off
        }
    }

    /**
     * Attempt to query the server for the stack. If the server is up,
     * we will get a response. If not, an exception will be thrown.
     *
     * @return - True if the local server is responsive to queries.
     *           False, otherwise.
     */
    private static boolean isServerUp() throws IOException {
        String apiPath = "/api/v1/stacks";

        String apiUrl = String.format(SERVER_URL_FORMAT, serverPort) + apiPath;
        CloseableHttpClient httpClient = HttpClients.createDefault();;

        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.addHeader("Authorization", getBasicAdminAuthentication());
            httpGet.addHeader("X-Requested-By", "ambari");
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : null;

            return true;
        } catch (IOException ex) {

        } finally {
            httpClient.close();
        }

        return false;
    }

    /**
     * Perform common initialization for each test case.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {

    }

    /**
     * Perform common clean up for each test case.
     *
     * @throws Exception
     */
    @After
    public void teardown() throws Exception {
    }
}
