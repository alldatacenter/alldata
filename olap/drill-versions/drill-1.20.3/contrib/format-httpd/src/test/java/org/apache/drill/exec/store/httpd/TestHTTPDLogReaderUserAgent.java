/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.httpd;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Category(RowSetTests.class)
public class TestHTTPDLogReaderUserAgent extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("httpd/"));

    defineHttpdPlugin();
  }

  private static void defineHttpdPlugin() {
    Map<String, FormatPluginConfig> formats = new HashMap<>();
    formats.put("multiformat", new HttpdLogFormatConfig(
            Collections.singletonList("access_log"),
            "combined" + '\n' +
            "common" + '\n' +
            "%h %l %u %t \"%r\" %s %b \"%{Referer}i\"" + '\n' +
            "%h %l %u %t \"%r\" %s %b \"%{Referer}i\" \"%{User-agent}i\"" + '\n' +
            "%%%h %a %A %l %u %t \"%r\" %>s %b %p \"%q\" \"%!200,304,302{Referer}i\" %D " +
            "\"%200{User-agent}i\" \"%{Cookie}i\" \"%{Set-Cookie}o\" \"%{If-None-Match}i\" \"%{Etag}o\"" + '\n',
            null,
            0,
            true,
            true,
            null));

    // Define a temporary plugin for the "cp" storage plugin.
    cluster.defineFormats("cp", formats);
  }

  @Test
  public void testMultiFormatUserAgent() throws RpcException {
    String sql =
            "SELECT                                                       " +
            "          `request_receive_time_epoch`,                      " +
            "          `request_user-agent`,                              " +
            "          `request_user-agent_device__name`,                 " +
            "          `request_user-agent_agent__name__version__major`   " +
            "FROM   cp.`httpd/multiformat.access_log`                     ";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
            .addNullable("request_receive_time_epoch",                      MinorType.TIMESTAMP)
            .addNullable("request_user-agent",                              MinorType.VARCHAR)
            .addNullable("request_user-agent_device__name",                 MinorType.VARCHAR)
            .addNullable("request_user-agent_agent__name__version__major",  MinorType.VARCHAR)
            .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
            .addRow(1_356_994_180_000L, "Mozilla/5.0 (X11; Linux i686 on x86_64; rv:11.0) Gecko/20100101 Firefox/11.0", "Linux Desktop", "Firefox 11")
            .addRow(1_356_994_181_000L, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36", "Apple Macintosh", "Chrome 66")
            .addRow(1_388_530_181_000L, null, null, null) // This line in the input does not have the useragent field at all.
            .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testUserAgentEnabled() throws Exception {
    String sql =
            "SELECT                                                               " +
                    "          `request_receive_time_epoch`,                      " +
                    "          `request_user-agent`,                              " +
                    "          `request_user-agent_device__name`,                 " +
                    "          `request_user-agent_agent__name__version__major`   " +
                    "FROM       table(                                            " +
                    "             cp.`httpd/typeremap.log`                        " +
                    "                 (                                           " +
                    "                   type => 'httpd',                          " +
                    "                   logFormat => 'common\ncombined\n%h %l %u %t \"%r\" %>s %b %{RequestId}o\n',\n" +
                    "                   flattenWildcards => true,                 " +
                    "                   parseUserAgent => true                    " +
                    "                 )                                           " +
                    "           )                                                 ";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
            .addNullable("request_receive_time_epoch",                      MinorType.TIMESTAMP)
            .addNullable("request_user-agent",                              MinorType.VARCHAR)
            .addNullable("request_user-agent_device__name",                 MinorType.VARCHAR)
            .addNullable("request_user-agent_agent__name__version__major",  MinorType.VARCHAR)
            .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
            .addRow(1_388_530_181_000L,
                    "Mozilla/5.0 (compatible; Googlebot/2.1; Yauaa Bot/42.123; +https://yauaa.basjes.nl)", "Basjes Googlebot Imitator", "Yauaa Bot 42")
            .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testUserAgentDisabled() throws Exception {
    String sql =
            "SELECT                                                               " +
            "          `request_receive_time_epoch`,                      " +
            "          `request_user-agent`,                              " +
            "          `request_user-agent_device__name`,                 " +
            "          `request_user-agent_agent__name__version__major`   " +
            "FROM       table(                                            " +
            "             cp.`httpd/typeremap.log`                        " +
            "                 (                                           " +
            "                   type => 'httpd',                          " +
            "                   logFormat => 'common\ncombined\n%h %l %u %t \"%r\" %>s %b %{RequestId}o\n',\n" +
            "                   flattenWildcards => true                  " +
            "                 )                                           " +
            "           )                                                 " +
            "LIMIT 1                                                      ";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
            .addNullable("request_receive_time_epoch",                      MinorType.TIMESTAMP)
            .addNullable("request_user-agent",                              MinorType.VARCHAR)
            .addNullable("request_user-agent_device__name",                 MinorType.VARCHAR)
            .addNullable("request_user-agent_agent__name__version__major",  MinorType.VARCHAR)
            .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
            .addRow(1_388_530_181_000L,
                    "Mozilla/5.0 (compatible; Googlebot/2.1; Yauaa Bot/42.123; +https://yauaa.basjes.nl)", null, null)
            .build();

    RowSetUtilities.verify(expected, results);
  }


  @Test
  public void testUserAgentAndTypeRemapping() throws Exception {
    String sql =
            "SELECT                                                                           \n" +
            "          `request_receive_time_epoch`                                           \n" +
            "        , `request_user-agent`                                                   \n" +
            "        , `request_user-agent_device__name`                                      \n" +
            "        , `request_user-agent_agent__name__version__major`                       \n" +
            "        , `request_firstline_uri_query_timestamp`                                \n" +
            "        , `request_firstline_uri_query_ua`                                       \n" +
            "        , `request_firstline_uri_query_ua_device__name`                          \n" +
            "        , `request_firstline_uri_query_ua_agent__name__version__major`           \n" +
            "        , `response_header_requestid_epoch`                                      \n" +
//            "        , *                                                                     \n"+
            "FROM       table(                                                                \n" +
            "             cp.`httpd/typeremap.log`                                            \n" +
            "                 (                                                               \n" +
            "                   type => 'httpd',                                              \n" +
            //                  LogFormat: Mind the leading and trailing spaces! Empty lines are ignored
            "                   logFormat => 'common\ncombined\n%h %l %u %t \"%r\" %>s %b %{RequestId}o\n',\n" +
            "                   flattenWildcards => true,                                     \n" +
            "                   parseUserAgent => true,                                       \n" +
            "                   logParserRemapping => '                                       \n" +
            "                       request.firstline.uri.query.ua        :HTTP.USERAGENT ;   \n" +
            "                       response.header.requestid             :MOD_UNIQUE_ID  ;   \n" +
            "                       request.firstline.uri.query.timestamp :TIME.EPOCH : LONG  \n" +
            "                   '                                                             \n" +
            "                 )                                                               \n" +
            "           )                                                                     \n";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
            .addNullable("request_receive_time_epoch",                          MinorType.TIMESTAMP)
            .addNullable("request_user-agent",                                  MinorType.VARCHAR)
            .addNullable("request_user-agent_device__name",                     MinorType.VARCHAR)
            .addNullable("request_user-agent_agent__name__version__major",      MinorType.VARCHAR)
            .addNullable("request_firstline_uri_query_timestamp",               MinorType.TIMESTAMP)
            .addNullable("request_firstline_uri_query_ua",                      MinorType.VARCHAR)
            .addNullable("request_firstline_uri_query_ua_device__name",         MinorType.VARCHAR)
            .addNullable("request_firstline_uri_query_ua_agent__name__version__major", MinorType.VARCHAR)
            .addNullable("response_header_requestid_epoch",                     MinorType.TIMESTAMP)
            .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
            .addRow(// These are directly parsed from the line
                    1_388_530_181_000L, // 2013-12-31T22:49:41.000Z
                    "Mozilla/5.0 (compatible; Googlebot/2.1; Yauaa Bot/42.123; +https://yauaa.basjes.nl)",
                    "Basjes Googlebot Imitator", "Yauaa Bot 42",

                    // These are parsed by casting the query string parameters to something else
                    1_607_506_430_621L, // 2020-12-09T09:33:50.621
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.139 Safari/537.36",
                    "Apple Macintosh", "Chrome 66",

                    null // No mod_unique_id field present
            )
            .addRow(// These are directly parsed from the line
                    1_388_530_181_000L, // 2013-12-31T22:49:41.000Z
                    null,               // The second line in the test file does not have a useragent field.
                    null, null,

                    // These are parsed by casting the query string parameters to something else
                    1_607_506_430_621L, // 2020-12-09T09:33:50.621
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3359.139 Safari/537.36",
                    "Apple Macintosh", "Chrome 77",

                    1_372_024_799_000L // 2013-06-23T21:59:59.000Z ==> The timestamp of the mod_unique_id value
            )
            .addRow(// These are directly parsed from the line
                    1_388_530_181_000L, // 2013-12-31T22:49:41.000Z
                    null,               // The second line in the test file does not have a useragent field.
                    null, null,

                    // These are parsed by casting the query string parameters to something else
                    1_607_506_430_621L, // 2020-12-09T09:33:50.621
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.3359.139 Safari/537.36",
                    "Apple Macintosh", "Chrome 55",

                    null // No mod_unique_id field present
            )
            .build();

    RowSetUtilities.verify(expected, results);
  }

}
