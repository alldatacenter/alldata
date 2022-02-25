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

package org.apache.ambari.server.controller.logging;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.junit.Test;


public class LogLevelQueryResponseTest {

  private static final String TEST_JSON_INPUT_LOG_LEVEL_QUERY =
    "{\"pageSize\":\"0\",\"queryTimeMS\":\"1459970731998\",\"resultSize\":\"6\",\"startIndex\":\"0\",\"totalCount\":\"0\"," +
      "\"vNameValues\":[{\"name\":\"FATAL\",\"value\":\"0\"},{\"name\":\"ERROR\",\"value\":\"0\"}," +
      "{\"name\":\"WARN\",\"value\":\"41\"},{\"name\":\"INFO\",\"value\":\"186\"},{\"name\":\"DEBUG\",\"value\":\"0\"}," +
      "{\"name\":\"TRACE\",\"value\":\"0\"}]}";


  @Test
  public void testBasicParsing() throws Exception {
    // setup a reader for the test JSON data
    StringReader stringReader =
      new StringReader(TEST_JSON_INPUT_LOG_LEVEL_QUERY);

    // setup the Jackson mapper/reader to read in the data structure
    ObjectMapper mapper =
      new ObjectMapper();
    AnnotationIntrospector introspector =
      new JacksonAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);


    ObjectReader logLevelQueryResponseReader =
      mapper.reader(LogLevelQueryResponse.class);

    LogLevelQueryResponse result =
      logLevelQueryResponseReader.readValue(stringReader);

    // expected values taken from JSON input string declared above
    assertEquals("startIndex not parsed properly",
      "0", result.getStartIndex());
    assertEquals("pageSize not parsed properly",
      "0", result.getPageSize());
    assertEquals("totalCount not parsed properly",
      "0", result.getTotalCount());
    assertEquals("resultSize not parsed properly",
      "6", result.getResultSize());
    assertEquals("queryTimeMS not parsed properly",
      "1459970731998", result.getQueryTimeMS());

    assertEquals("Incorrect number of log level count items parsed",
      6, result.getNameValueList().size());

    List<NameValuePair> resultList =
      result.getNameValueList();
    assertNameValuePair("FATAL", "0", resultList.get(0));
    assertNameValuePair("ERROR", "0", resultList.get(1));
    assertNameValuePair("WARN", "41", resultList.get(2));
    assertNameValuePair("INFO", "186", resultList.get(3));
    assertNameValuePair("DEBUG", "0", resultList.get(4));
    assertNameValuePair("TRACE", "0", resultList.get(5));

  }

  /**
   * Convenience method for asserting on the values of NameValuePair instances
   *
   * @param expectedName the expected name
   * @param expectedValue the expected value
   * @param nameValuePair the NameValuePair instance to test
   */
  static void assertNameValuePair(String expectedName, String expectedValue, NameValuePair nameValuePair) {
    assertEquals("Unexpected name found in this pair",
      expectedName, nameValuePair.getName());
    assertEquals("Unexpected value found in this pair",
      expectedValue, nameValuePair.getValue());
  }

}
