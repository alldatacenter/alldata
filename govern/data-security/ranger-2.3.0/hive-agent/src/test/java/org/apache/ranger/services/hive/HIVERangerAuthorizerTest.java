/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.hive;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hive.service.server.HiveServer2;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * Here we plug the Ranger RangerHiveAuthorizerFactory into HIVE.
 *
 * A custom RangerAdminClient is plugged into Ranger in turn, which loads security policies from a local file. These policies were
 * generated in the Ranger Admin UI for a service called "HIVETest":
 *
 * a) A user "bob" can do a select/update on the table "words"
 * b) A group called "IT" can do a select only on the "count" column in "words"
 * c) "bob" can create any database
 * d) "dave" can do a select on the table "words" but only if the "count" column is >= 80
 * e) "jane" can do a select on the table "words", but only get a "hash" of the word, and not the word itself.
 * f) "da_test_user" is delegate admin for rangerauthz database.
 * g) "tom" has all permissions on database "test1" and has all permissions on all databases with regard to UDF
 *
 * In addition we have some TAG based policies created in Atlas and synced into Ranger:
 *
 * a) The tag "HiveTableTag" is associated with "select" permission to the "dev" group to the "words" table in the "hivetable" database.
 * b) The tag "HiveDatabaseTag" is associated with "create" permission to the "dev" group to the "hivetable" database.
 * c) The tag "HiveColumnTag" is associated with "select" permission to the "frank" user to the "word" column of the "words" table.
 */
@org.junit.Ignore
public class HIVERangerAuthorizerTest {

    private static final File hdfsBaseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static HiveServer2 hiveServer;
    private static int port;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();

        HiveConf conf = new HiveConf();

        // Warehouse
        File warehouseDir = new File("./target/hdfs/warehouse").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTOREWAREHOUSE.varname, warehouseDir.getPath());

        // Scratchdir
        File scratchDir = new File("./target/hdfs/scratchdir").getAbsoluteFile();
        conf.set("hive.exec.scratchdir", scratchDir.getPath());

        // REPL DUMP target folder
        File replRootDir = new File("./target/user/hive").getAbsoluteFile();
        conf.set("hive.repl.rootdir", replRootDir.getPath());

        // Create a temporary directory for the Hive metastore
        File metastoreDir = new File("./metastore_db/").getAbsoluteFile();
        conf.set(HiveConf.ConfVars.METASTORECONNECTURLKEY.varname,
                 String.format("jdbc:derby:;databaseName=%s;create=true",  metastoreDir.getPath()));

        conf.set(HiveConf.ConfVars.METASTORE_AUTO_CREATE_ALL.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_THRIFT_PORT.varname, "" + port);
        conf.set(HiveConf.ConfVars.METASTORE_SCHEMA_VERIFICATION.toString(), "false");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_WEBUI_PORT.varname, "0");
        conf.set(HiveConf.ConfVars.HIVE_EXECUTION_ENGINE.varname,"mr");

        hiveServer = new HiveServer2();
        hiveServer.init(conf);
        hiveServer.start();

        Class.forName("org.apache.hive.jdbc.HiveDriver");

        // Create database
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE IF NOT EXISTS rangerauthz with dbproperties ('repl.source.for'='1,2,3')");
        statement.execute("CREATE DATABASE IF NOT EXISTS demo");

        statement.close();
        connection.close();

        // Load data into HIVE
        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();
        // statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("create table if not exists words (word STRING, count INT) row format delimited fields terminated by '\t' stored as textfile");

        // Copy "wordcount.txt" to "target" to avoid overwriting it during load
        File inputFile = new File(HIVERangerAuthorizerTest.class.getResource("../../../../../wordcount.txt").toURI());
        Path outputPath = Paths.get(inputFile.toPath().getParent().getParent().toString() + File.separator + "wordcountout.txt");
        Files.copy(inputFile.toPath(), outputPath);

        statement.execute("LOAD DATA INPATH '" + outputPath + "' OVERWRITE INTO TABLE words");

        // Just test to make sure it's working
        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        if (resultSet.next()) {
            Assert.assertEquals("Mr.", resultSet.getString(1));
        } else {
            Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();

        // Enable ranger authorization after the initial db setup and table creating is done.
        conf.set(HiveConf.ConfVars.HIVE_AUTHORIZATION_ENABLED.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS.varname, "true");
        conf.set(HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER.varname,
                 "org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory");

    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        hiveServer.stop();
        FileUtil.fullyDelete(hdfsBaseDir);
        File metastoreDir = new File("./metastore_db/").getAbsoluteFile();
        FileUtil.fullyDelete(metastoreDir);
    }

    // this should be allowed (by the policy - user)
    @Test
    public void testHiveSelectAllAsBob() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        if (resultSet.next()) {
            Assert.assertEquals("Mr.", resultSet.getString(1));
            Assert.assertEquals(100, resultSet.getInt(2));
        } else {
            Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();
    }

    // the "IT" group doesn't have permission to select all
    @Test
    public void testHiveSelectAllAsAlice() throws Exception {

        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
                Connection connection = DriverManager.getConnection(url, "alice", "alice");
                Statement statement = connection.createStatement();

                try {
                    statement.executeQuery("SELECT * FROM words where count == '100'");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }

                statement.close();
                connection.close();
                return null;
            }
        });
    }

    // this should be allowed (by the policy - user)
    @Test
    public void testHiveSelectSpecificColumnAsBob() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery("SELECT count FROM words where count == '100'");
        if (resultSet.next()) {
            Assert.assertEquals(100, resultSet.getInt(1));
        } else {
            Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();
    }

    // this should be allowed (by the policy - group)
    @Test
    public void testHiveSelectSpecificColumnAsAlice() throws Exception {

        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
                Connection connection = DriverManager.getConnection(url, "alice", "alice");
                Statement statement = connection.createStatement();

                ResultSet resultSet = statement.executeQuery("SELECT count FROM words where count == '100'");
                if (resultSet.next()) {
                    Assert.assertEquals(100, resultSet.getInt(1));
                } else {
                    Assert.fail("No ResultSet found");
                }

                statement.close();
                connection.close();
                return null;
            }
        });
    }

    // An unknown user shouldn't be allowed
    @Test
    public void testHiveSelectSpecificColumnAsEve() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "eve", "eve");
        Statement statement = connection.createStatement();

        try {
            statement.executeQuery("SELECT count FROM words where count == '100'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();
    }

    // test "alice", but in the wrong group
    @Test
    public void testHiveSelectSpecificColumnAsAliceWrongGroup() throws Exception {

        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"DevOps"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
                Connection connection = DriverManager.getConnection(url, "alice", "alice");
                Statement statement = connection.createStatement();

                try {
                    statement.executeQuery("SELECT count FROM words where count == '100'");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }

                statement.close();
                connection.close();
                return null;
            }
        });
    }

    // this should be allowed (by the policy - user)
    // Insert launches a MR job which fails in the unit test
    @Test
    @org.junit.Ignore
    public void testHiveUpdateAllAsBob() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        statement.execute("insert into words (word, count) values ('newword', 5)");

        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where word == 'newword'");
        if (resultSet.next()) {
            Assert.assertEquals("newword", resultSet.getString(1));
            Assert.assertEquals(5, resultSet.getInt(2));
        } else {
            Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();
    }

    // this should not be allowed as "alice" can't insert into the table
    @Test
    public void testHiveUpdateAllAsAlice() throws Exception {
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
                Connection connection = DriverManager.getConnection(url, "alice", "alice");
                Statement statement = connection.createStatement();

                try {
                    statement.execute("insert into words (word, count) values ('newword2', 5)");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }


                statement.close();
                connection.close();
                return null;
            }
        });
    }

    @Test
    public void testHiveUdfCreateOnWildcardDatabase() throws Exception {
		String url = "jdbc:hive2://localhost:" + port;
		// "tom" has:
		// ranger permissions to create/read/update/drop the database
		// ranger permissions to create/read/update/drop UDFs on test1 database
		try (	Connection connection = DriverManager.getConnection(url, "tom", "tom");
				Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE IF EXISTS test1");
			statement.execute("CREATE DATABASE test1");
			statement.execute("USE test1");
			statement.execute("CREATE TEMPORARY FUNCTION tmp AS \"org.apache.hadoop.hive.ql.udf.UDFPI\"");
			statement.execute("CREATE FUNCTION tmp AS \"org.apache.hadoop.hive.ql.udf.UDFPI\"");
			ResultSet resultSet = statement.executeQuery("SHOW FUNCTIONS LIKE '*tmp'");
			int rowCounter = 0;
			while (resultSet.next()) {
				String value = resultSet.getString(1);
				if (value.contains("tmp")) {
				  ++rowCounter;
				}
			}
			Assert.assertEquals(2, rowCounter);
			// clean up
			statement.execute("DROP FUNCTION IF EXISTS tmp");
			statement.execute("DROP FUNCTION IF EXISTS test1.tmp");
			statement.execute("DROP DATABASE IF EXISTS test1");
		}
	}

    @Test
    public void testHiveCreateDropDatabase() throws Exception {

        String url = "jdbc:hive2://localhost:" + port;

        // Try to create a database as "bob" - this should be allowed
        Connection connection = DriverManager.getConnection(url, "bob", "bob");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE if not exists bobtemp");

        statement.close();
        connection.close();

        // Try to create a database as "alice" - this should not be allowed
        connection = DriverManager.getConnection(url, "alice", "alice");
        statement = connection.createStatement();

        try {
            statement.execute("CREATE DATABASE if not exists alicetemp");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        // Try to drop a database as "bob" - this should not be allowed
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        try {
            statement.execute("drop DATABASE bobtemp");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        // Try to drop a database as "admin" - this should be allowed
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop DATABASE bobtemp");

        statement.close();
        connection.close();
    }

    @Test
    public void testBobSelectOnDifferentDatabase() throws Exception {

        String url = "jdbc:hive2://localhost:" + port;

        // Create a database as "admin"
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE if not exists admintemp");

        statement.close();
        connection.close();

        // Create a "words" table in "admintemp"
        url = "jdbc:hive2://localhost:" + port + "/admintemp";
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();
        statement.execute("CREATE TABLE if not exists  WORDS (word STRING, count INT)");

        statement.close();
        connection.close();

        // Now try to read it as "bob"
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        try {
            statement.executeQuery("SELECT count FROM words where count == '100'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();

        // Drop the table and database as "admin"
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop TABLE words");
        statement.execute("drop DATABASE admintemp");

        statement.close();
        connection.close();
    }

    @Test
    public void testBobSelectOnDifferentTables() throws Exception {

        // Create a "words2" table in "rangerauthz"
        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE if not exists WORDS2 (word STRING, count INT)");

        statement.close();
        connection.close();

        // Now try to read it as "bob"
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        try {
            statement.executeQuery("SELECT count FROM words2 where count == '100'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();

        // Drop the table as "admin"
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop TABLE words2");

        statement.close();
        connection.close();
    }

    @Test
    public void testBobAlter() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";

        // Create a new table as admin
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS WORDS2 (word STRING, count INT)");

        statement.close();
        connection.close();

        // Try to add a new column in words as "bob" - this should fail
        url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        try {
            statement.execute("ALTER TABLE WORDS2 ADD COLUMNS (newcol STRING)");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();

        // Now alter it as "admin"
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("ALTER TABLE WORDS2 ADD COLUMNS (newcol STRING)");

        statement.close();
        connection.close();

        // Try to alter it as "bob" - this should fail
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        try {
            statement.execute("ALTER TABLE WORDS2 REPLACE COLUMNS (word STRING, count INT)");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            // expected
        }

        statement.close();
        connection.close();

        // Now alter it as "admin"
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("ALTER TABLE WORDS2 REPLACE COLUMNS (word STRING, count INT)");

        statement.close();
        connection.close();

        // Drop the table as "admin"
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop TABLE words2");

        statement.close();
        connection.close();
    }

    @Test
    public void testHiveRowFilter() throws Exception {

        // dave can do a select where the count is >= 80
        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "dave", "dave");
        Statement statement = connection.createStatement();

        // "dave" can select where count >= 80
        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        if (resultSet.next()) {
        	Assert.assertEquals("Mr.", resultSet.getString(1));
        	Assert.assertEquals(100, resultSet.getInt(2));
        } else {
        	Assert.fail("No ResultSet found");
        }

        resultSet = statement.executeQuery("SELECT * FROM words where count == '79'");
        if (resultSet.next()) {
        	Assert.fail("Authorization should not be granted for count < 80");
        }

        statement.close();
        connection.close();

        // "bob" should be able to read a count of "79" as the filter doesn't apply to him
        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();

        resultSet = statement.executeQuery("SELECT * FROM words where count == '79'");
        if (resultSet.next()) {
        	Assert.assertEquals("cannot", resultSet.getString(1));
        	Assert.assertEquals(79, resultSet.getInt(2));
        } else {
        	Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();
    }

    @Test
    public void testHiveDataMasking() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "jane", "jane");
        Statement statement = connection.createStatement();

        // "jane" can only set a hash of the word, and not the word itself
        ResultSet resultSet = statement.executeQuery("SELECT * FROM words where count == '100'");
        if (resultSet.next()) {
        	Assert.assertNotEquals("Mr.", resultSet.getString(1));
            Assert.assertEquals("1a24b7688c199c24d87b5984d152b37d1d528911ec852d9cdf98c3ef29b916ea", resultSet.getString(1));
        	Assert.assertEquals(100, resultSet.getInt(2));
        } else {
        	Assert.fail("No ResultSet found");
        }

        statement.close();
        connection.close();
    }

    // Insert launches a MR job which fails in the unit test
    @Test
    @org.junit.Ignore
    public void testCreateDropMacro() throws Exception {
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, "admin", "admin");
        Statement statement = connection.createStatement();
        statement.execute("CREATE DATABASE IF NOT EXISTS rangerauthz2");

        statement.close();
        connection.close();

        // Load data into HIVE
        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz2";
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("create table if not exists rangerauthz2.macro_testing (a INT, b INT)");
        statement.execute("insert into rangerauthz2.macro_testing (a, b) values (4, 5)");
        statement.execute("insert into rangerauthz2.macro_testing (a, b) values (3, 5)");

        ResultSet resultSet = statement.executeQuery("SELECT * FROM rangerauthz2.macro_testing where b == '5'");
        //Verify Table Created And Contains Data

        if (resultSet.next()) {
            Assert.assertEquals(5, resultSet.getInt(2));
        } else {
            Assert.fail("No Resultset Found");
        }

        statement.execute("create temporary macro math_cube(x int) x*x*x");
        ResultSet resultSet2 = statement.executeQuery("select math_cube(b) from rangerauthz2.macro_testing");

        if (resultSet2.next()) {
            Assert.assertEquals(125, resultSet2.getInt(1));
        } else {
            Assert.fail("Macro Not Created Properly");
        }

        statement.execute("drop temporary macro math_cube");

        try{
            statement.executeQuery("select math_cube(b) from rangerauthz2.macro_testing");
            Assert.fail("macro deleted already");
        }
        catch(SQLException ex){
            //expected
        }

        statement.execute("DROP TABLE rangerauthz2.macro_testing");
        statement.execute("DROP DATABASE rangerauthz2");

        statement.close();
        connection.close();
    }

    // Insert launches a MR job which fails in the unit test
    @Test
    @org.junit.Ignore
    public void testCreateDropFunction() throws Exception {
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE IF NOT EXISTS rangerauthz3");
        statement.close();
        connection.close();

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz3";
        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();
        statement.execute("CREATE TABLE if not exists rangerauthz3.function_testing (a DOUBLE, b DOUBLE)");
        statement.execute("insert into rangerauthz3.function_testing (a, b) values (4.54845, 5.5487)");
        ResultSet resultSet2 = statement.executeQuery("select round(b) from rangerauthz3.function_testing");

        if (resultSet2.next()) {
            Assert.assertEquals(6, resultSet2.getInt(1));
        } else {
            Assert.fail("No Resultset Found");
        }

        statement.execute("DROP TABLE rangerauthz3.function_testing");
        statement.execute("DROP DATABASE rangerauthz3");

        statement.close();
        connection.close();
    }

    // S3 location URI authorization (by the policy - user bob)
    @Test
    public void testS3URIAuthorization() throws Exception {
        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = null;
        Statement statement   = null;
        try {
            connection = DriverManager.getConnection(url, "bob", "bob");
            statement = connection.createStatement();
            statement.executeQuery("create table if not exists words (word STRING, count INT) row format delimited fields terminated by '\t' stored as textfile LOCATION 's3a://test/data'");
            Assert.fail("Failure expected on an unauthorized call");
            //expected we don't get any resultset here
        } catch(SQLException ex){
                //expected
        } finally {
            statement.close();
            connection.close();
        }
    }

    @Test
    public void testGrantrevoke() throws Exception {
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, "admin", "admin");
        Statement statement = connection.createStatement();
        statement.execute("CREATE DATABASE IF NOT EXISTS rangerauthzx");
        statement.execute("use rangerauthzx");
        statement.execute("CREATE TABLE rangerauthzx.tbl1 (a INT, b INT)");
        statement.close();
        connection.close();

        String url = "jdbc:hive2://localhost:" + port;
        connection = DriverManager.getConnection(url, "dave", "dave");
        statement = connection.createStatement();
        try{
            statement.execute("use rangerauthzx");
            statement.execute("grant select ON TABLE rangerauthzx.tbl1 to USER jane with grant option");
            Assert.fail("access should not have been granted");
        }
        catch(SQLException ex){
            //expected
        }

        connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");
        statement = connection.createStatement();
        try{
            statement.execute("use rangerauthzx");
            statement.execute("grant select ON TABLE rangerauthzx.tbl1 to USER jane with grant option");
        }
        catch(SQLException ex){
            Assert.fail("access should have been granted to da_test_user");
        }
        statement.close();
        connection.close();

        connection = DriverManager.getConnection(url, "admin", "admin");
        statement = connection.createStatement();
        statement.execute("DROP TABLE rangerauthzx.tbl1");
    }

    @Test
    public void testTagBasedPolicyForTable() throws Exception {

        String url = "jdbc:hive2://localhost:" + port;

        // Create a database as "admin"
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE hivetable");

        statement.close();
        connection.close();

        // Create a "words" table in "hivetable"
        final String tableUrl = "jdbc:hive2://localhost:" + port + "/hivetable";
        connection = DriverManager.getConnection(tableUrl, "admin", "admin");
        statement = connection.createStatement();
        statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("CREATE TABLE WORDS2 (word STRING, count INT)");

        statement.close();
        connection.close();

        // Now try to read it as the "public" group
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"dev"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection connection = DriverManager.getConnection(tableUrl, "alice", "alice");
                Statement statement = connection.createStatement();

                // "words" should work
                ResultSet resultSet = statement.executeQuery("SELECT * FROM words");
                Assert.assertNotNull(resultSet);

                statement.close();

                statement = connection.createStatement();
                try {
                    // "words2" should not
                    statement.executeQuery("SELECT * FROM words2");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }

                statement.close();
                connection.close();
                return null;
            }
        });

        // Drop the table and database as "admin"
        connection = DriverManager.getConnection(tableUrl, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop TABLE words");
        statement.execute("drop TABLE words2");
        statement.execute("drop DATABASE hivetable");

        statement.close();
        connection.close();
    }

    @Test
    public void testTagBasedPolicyForDatabase() throws Exception {

        final String url = "jdbc:hive2://localhost:" + port;

        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"dev"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                // Create a database
                Connection connection = DriverManager.getConnection(url, "alice", "alice");
                Statement statement = connection.createStatement();

                statement.execute("CREATE DATABASE hivetable");
                statement.close();

                statement = connection.createStatement();
                try {
                    // "hivetable2" should not be allowed to be created by the "dev" group
                    statement.execute("CREATE DATABASE hivetable2");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }

                statement.close();
                connection.close();
                return null;
            }
        });

        // Drop the database as "admin"
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("drop DATABASE hivetable");

        statement.close();
        connection.close();
    }

    @Test
    public void testTagBasedPolicyForColumn() throws Exception {

        String url = "jdbc:hive2://localhost:" + port;

        // Create a database as "admin"
        Connection connection = DriverManager.getConnection(url, "admin", "admin");
        Statement statement = connection.createStatement();

        statement.execute("CREATE DATABASE hivetable");

        statement.close();
        connection.close();

        // Create a "words" table in "hivetable"
        final String tableUrl = "jdbc:hive2://localhost:" + port + "/hivetable";
        connection = DriverManager.getConnection(tableUrl, "admin", "admin");
        statement = connection.createStatement();
        statement.execute("CREATE TABLE WORDS (word STRING, count INT)");
        statement.execute("CREATE TABLE WORDS2 (word STRING, count INT)");

        statement.close();
        connection.close();

        // Now try to read it as the user "frank"
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("frank", new String[] {"unknown"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                Connection connection = DriverManager.getConnection(tableUrl, "frank", "frank");

                // we can select "word" from "words"
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT word FROM words");
                Assert.assertNotNull(resultSet);
                statement.close();

                try {
                    // we can't select "word" from "words2" as "frank"
                    statement.executeQuery("SELECT word FROM words2");
                    Assert.fail("Failure expected on an unauthorized call");
                } catch (SQLException ex) {
                    // expected
                }

                statement.close();
                connection.close();
                return null;
            }
        });

        // Drop the table and database as "admin"
        connection = DriverManager.getConnection(tableUrl, "admin", "admin");
        statement = connection.createStatement();

        statement.execute("drop TABLE words");
        statement.execute("drop TABLE words2");
        statement.execute("drop DATABASE hivetable");

        statement.close();
        connection.close();
    }

    @Test
    public void testShowPrivileges() throws Exception {
        String initialUrl = "jdbc:hive2://localhost:" + port;
        Connection connection = DriverManager.getConnection(initialUrl, "admin", "admin");
        Statement statement = connection.createStatement();
        Assert.assertTrue(statement.execute("show grant user admin on table words"));
        statement.close();
        connection.close();
    }

        // test "dat_test_user", to do REPL DUMP
        @Test
        public void testREPLDUMPAuth() throws Exception {

            String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
            Connection connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");

            Statement statement = connection.createStatement();
            try {
                statement.execute("repl dump rangerauthz");
            } catch (SQLException ex) {
                Assert.fail("access should have been granted to da_test_user");
            }
            statement.close();
            connection.close();

            connection = DriverManager.getConnection(url, "bob", "bob");
            statement = connection.createStatement();
            try {
                statement.execute("repl dump rangerauthz");
                Assert.fail("Failure expected on an unauthorized call");
            } catch (SQLException ex) {
              //Excepted
            }
            statement.close();
            connection.close();
        }

    // test "dat_test_user", to do REPL DUMP
    @Test
    public void testREPLDUMPTableAuth() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");

        Statement statement = connection.createStatement();
        try {
            statement.execute("repl dump rangerauthz.words");
        } catch (SQLException ex) {
            Assert.fail("access should have been granted to da_test_user");
        }
        statement.close();
        connection.close();

        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();
        try {
            statement.execute("repl dump rangerauthz.words");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            //Excepted
        }
        statement.close();
        connection.close();
    }

    @Test
    public void testKillQuery() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");

        Statement statement = connection.createStatement();
        try {
            statement.execute("kill query 'dummyQueryId'");
        } catch (SQLException ex) {
            Assert.fail("access should have been granted to da_test_user");
        }
        statement.close();
        connection.close();

        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();
        try {
            statement.execute("kill query 'dummyQueryId'");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            //Excepted
        }
        statement.close();
        connection.close();
    }

    @Test
    public void testWorkLoadManagementCommands() throws Exception {

        String url = "jdbc:hive2://localhost:" + port + "/rangerauthz";
        Connection connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");

        Statement statement = connection.createStatement();
        try {
            statement.execute("show resource plans");
        } catch (SQLException ex) {
            Assert.fail("access should have been granted to da_test_user");
        }
        statement.close();
        connection.close();

        connection = DriverManager.getConnection(url, "da_test_user", "da_test_user");
        statement = connection.createStatement();
        try {
            statement.execute("create resource plan myplan1");
        } catch (SQLException ex) {
            Assert.fail("access should have been granted to da_test_user");
        }
        statement.close();
        connection.close();

        connection = DriverManager.getConnection(url, "bob", "bob");
        statement = connection.createStatement();
        try {
            statement.execute("create resource plan myplan1");
            Assert.fail("Failure expected on an unauthorized call");
        } catch (SQLException ex) {
            //Excepted
        }
        statement.close();
        connection.close();
    }
}
