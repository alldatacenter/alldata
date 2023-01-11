/**
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
package org.apache.ranger.services.knox;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.knox.gateway.GatewayTestConfig;
import org.apache.knox.gateway.GatewayTestDriver;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * Test Apache Knox secured by Apache Ranger.
 */
public class KnoxRangerTest {

    private static GatewayTestDriver driver = new GatewayTestDriver();

    @BeforeClass
    public static void setupSuite() throws Exception {
        driver.setResourceBase(KnoxRangerTest.class);
        driver.setupLdap(0);
        GatewayTestConfig config = new GatewayTestConfig();
        driver.setupService("WEBHDFS", "http://localhost:50070/webhdfs", "/cluster/webhdfs", true);
        driver.setupService("STORM", "http://localhost:8477", "/cluster/storm", true);
        driver.setupService("SOLR", "http://localhost:8983", "/cluster/solr", true);
        driver.setupService("WEBHBASE", "http://localhost:60080", "/cluster/hbase", true);
        driver.setupService("KAFKA", "http://localhost:8477", "/cluster/kafka", true);

        driver.setupGateway(config, "cluster", createTopology(), true);
    }

    @AfterClass
    public static void cleanupSuite() throws Exception {
        driver.cleanup();
    }

    /**
     * Creates a topology that is deployed to the gateway instance for the test suite.
     * Note that this topology is shared by all of the test methods in this suite.
     * @return A populated XML structure for a topology file.
     */
    private static XMLTag createTopology() {
        XMLTag xml = XMLDoc.newDocument( true )
            .addRoot( "topology" )
            .addTag( "gateway" )
            .addTag( "provider" )
            .addTag( "role" ).addText( "webappsec" )
            .addTag("name").addText("WebAppSec")
            .addTag("enabled").addText("true")
            .addTag( "param" )
            .addTag("name").addText("csrf.enabled")
            .addTag("value").addText("true").gotoParent().gotoParent()
            .addTag("provider")
            .addTag("role").addText("authentication")
            .addTag("name").addText("ShiroProvider")
            .addTag("enabled").addText("true")
            .addTag( "param" )
            .addTag("name").addText("main.ldapRealm")
            .addTag("value").addText("org.apache.hadoop.gateway.shirorealm.KnoxLdapRealm").gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.userDnTemplate" )
            .addTag( "value" ).addText( "uid={0},ou=people,dc=hadoop,dc=apache,dc=org" ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.contextFactory.url" )
            .addTag( "value" ).addText(driver.getLdapUrl() ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "main.ldapRealm.contextFactory.authenticationMechanism" )
            .addTag( "value" ).addText( "simple" ).gotoParent()
            .addTag( "param" )
            .addTag( "name" ).addText( "urls./**" )
            .addTag( "value" ).addText( "authcBasic" ).gotoParent().gotoParent()
            .addTag("provider")
            .addTag("role").addText("identity-assertion")
            .addTag("enabled").addText("true")
            .addTag("name").addText("Default").gotoParent()
            .addTag("provider")
            .addTag( "role" ).addText( "authorization" )
            .addTag("name").addText("XASecurePDPKnox")
            .addTag( "enabled" ).addText( "true" )
            .gotoRoot()
            .addTag("service")
            .addTag("role").addText("WEBHDFS")
            .addTag("url").addText(driver.getRealUrl("WEBHDFS")).gotoParent()
            .addTag("service")
            .addTag("role").addText("STORM")
            .addTag("url").addText(driver.getRealUrl("STORM")).gotoParent()
            .addTag("service")
            .addTag("role").addText("WEBHBASE")
            .addTag("url").addText(driver.getRealUrl("WEBHBASE")).gotoParent()
            .addTag("service")
            .addTag("role").addText("KAFKA")
            .addTag("url").addText(driver.getRealUrl("KAFKA")).gotoParent()
            .addTag("service")
            .addTag("role").addText("SOLR")
            .addTag("url").addText(driver.getRealUrl("SOLR")).gotoParent()
            .gotoRoot();
        return xml;
    }

    @Test
    public void testHDFSAllowed() throws IOException {
        makeWebHDFSInvocation(HttpStatus.SC_OK, "alice", "password");
    }

    @Test
    public void testHDFSNotAllowed() throws IOException {
        makeWebHDFSInvocation(HttpStatus.SC_FORBIDDEN, "bob", "password");
    }

    @Test
    public void testStormUiAllowed() throws Exception {
        makeStormUIInvocation(HttpStatus.SC_OK, "bob", "password");
    }

    @Test
    public void testStormNotUiAllowed() throws Exception {
        makeStormUIInvocation(HttpStatus.SC_FORBIDDEN, "alice", "password");
    }

    @Test
    public void testHBaseAllowed() throws Exception {
        makeHBaseInvocation(HttpStatus.SC_OK, "alice", "password");
    }

    @Test
    public void testHBaseNotAllowed() throws Exception {
        makeHBaseInvocation(HttpStatus.SC_FORBIDDEN, "bob", "password");
    }

    @Test
    public void testKafkaAllowed() throws IOException {
        makeKafkaInvocation(HttpStatus.SC_OK, "alice", "password");
    }

    @Test
    public void testKafkaNotAllowed() throws IOException {
        makeKafkaInvocation(HttpStatus.SC_FORBIDDEN, "bob", "password");
    }

    @Test
    public void testSolrAllowed() throws Exception {
        makeSolrInvocation(HttpStatus.SC_OK, "alice", "password");
    }

    @Test
    public void testSolrNotAllowed() throws Exception {
        makeSolrInvocation(HttpStatus.SC_FORBIDDEN, "bob", "password");
    }

    private void makeWebHDFSInvocation(int statusCode, String user, String password) throws IOException {

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/webhdfs-liststatus-test.json");

        driver.getMock("WEBHDFS")
        .expect()
          .method( "GET" )
          .pathInfo( "/v1/hdfstest" )
          .queryParam( "op", "LISTSTATUS" )
        .respond()
          .status( HttpStatus.SC_OK )
          .content( IOUtils.toByteArray( path.toUri() ) )
          .contentType( "application/json" );

        ValidatableResponse response = given()
          .log().all()
          .auth().preemptive().basic( user, password )
          .header("X-XSRF-Header", "jksdhfkhdsf")
          .queryParam( "op", "LISTSTATUS" )
        .when()
          .get( driver.getUrl("WEBHDFS") + "/v1/hdfstest" )
        .then()
          .statusCode(statusCode)
          .log().body();

        if (statusCode == HttpStatus.SC_OK) {
            response.body( "FileStatuses.FileStatus[0].pathSuffix", is ("dir") );
        }
    }

    private void makeStormUIInvocation(int statusCode, String user, String password) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/cluster-configuration.json");

        driver.getMock("STORM")
            .expect()
            .method("GET")
            .pathInfo("/api/v1/cluster/configuration")
            .respond()
            .status(HttpStatus.SC_OK)
            .content(IOUtils.toByteArray( path.toUri() ))
            .contentType("application/json");

        given()
            .auth().preemptive().basic(user, password)
            .header("X-XSRF-Header", "jksdhfkhdsf")
            .header("Accept", "application/json")
            .when().get( driver.getUrl("STORM") + "/api/v1/cluster/configuration")
            .then()
            .log().all()
            .statusCode(statusCode);

      }

    private void makeHBaseInvocation(int statusCode, String user, String password) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/webhbase-table-list.xml");


        driver.getMock("WEBHBASE")
        .expect()
        .method( "GET" )
        .pathInfo( "/" )
        .header( "Accept", ContentType.XML.toString() )
        .respond()
        .status( HttpStatus.SC_OK )
        .content( IOUtils.toByteArray( path.toUri() ) )
        .contentType( ContentType.XML.toString() );

        given()
            .log().all()
            .auth().preemptive().basic( user, password )
            .header("X-XSRF-Header", "jksdhfkhdsf")
            .header( "Accept", ContentType.XML.toString() )
            .when().get( driver.getUrl("WEBHBASE") )
            .then()
            .statusCode( statusCode )
            .log().body();
    }

    private void makeKafkaInvocation(int statusCode, String user, String password) throws IOException {

        driver.getMock("KAFKA")
        .expect()
        .method( "GET" )
        .pathInfo( "/topics" )
        .respond()
        .status( HttpStatus.SC_OK );

        given()
            .log().all()
            .auth().preemptive().basic( user, password )
            .header("X-XSRF-Header", "jksdhfkhdsf")
        .when()
            .get( driver.getUrl("KAFKA") + "/topics" )
        .then()
            .statusCode(statusCode)
            .log().body();

    }

    private void makeSolrInvocation(int statusCode, String user, String password) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/query_response.xml");

        driver.getMock("SOLR")
        .expect()
        .method("GET")
        .pathInfo("/gettingstarted/select")
        .queryParam("q", "author_s:William+Shakespeare")
        .respond()
        .status(HttpStatus.SC_OK)
        .content(IOUtils.toByteArray( path.toUri() ))
        .contentType("application/json");

        given()
        .auth().preemptive().basic(user, password)
        .header("X-XSRF-Header", "jksdhfkhdsf")
        .header("Accept", "application/json")
        .when().get( driver.getUrl("SOLR")
            + "/gettingstarted/select?q=author_s:William+Shakespeare")
        .then()
        .log().all()
        .statusCode(statusCode);

    }
}
