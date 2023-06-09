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
package org.apache.drill.exec.store.druid.rest;

import org.apache.drill.exec.store.druid.druid.DruidScanResponse;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

public class DruidQueryClientTest {

  @Mock
  private RestClient restClient;

  @Mock
  private Response httpResponse;

  @Mock
  private ResponseBody httpResponseBody;

  private DruidQueryClient druidQueryClient;
  private static final String BROKER_URI = "some broker uri";
  private static final String QUERY = "{\"queryType\":\"scan\",\"dataSource\":\"wikipedia\",\"descending\":false,\"filter\":{\"type\":\"and\",\"fields\":[{\"type\":\"selector\",\"dimension\":\"user\",\"value\":\"Dansker\"},{\"type\":\"search\",\"dimension\":\"comment\",\"query\":{\"type\":\"contains\",\"value\":\"Bitte\",\"caseSensitive\":false}}]},\"granularity\":\"all\",\"intervals\":[\"2016-06-27T00:00:00.000Z/2016-06-27T22:00:00.000Z\"],\"columns\":[],\"offset\":0,\"limit\":4096}";
  private static final Header ENCODING_HEADER =
      new BasicHeader(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());

  @Before
  public void setup() throws IOException {
    restClient = mock(RestClient.class);
    httpResponse = mock(Response.class);
    httpResponseBody = mock(ResponseBody.class);

    when(httpResponse.isSuccessful()).thenReturn(true);
    when(httpResponse.body()).thenReturn(httpResponseBody);
    when(restClient.post(BROKER_URI + "/druid/v2", QUERY))
        .thenReturn(httpResponse);

    druidQueryClient = new DruidQueryClient(BROKER_URI, restClient);
  }

  @Test(expected=Exception.class)
  public void executeQueryCalledDruidReturnsNon200ShouldThrowError()
      throws Exception {
    when(httpResponse.isSuccessful()).thenReturn(false);
    druidQueryClient.executeQuery(QUERY);
  }

  @Test
  public void executeQueryCalledNoResponsesFoundReturnsEmptyEventList()
      throws Exception {
    InputStream inputStream =
        new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8));
    when(httpResponseBody.byteStream()).thenReturn(inputStream);

    DruidScanResponse response = druidQueryClient.executeQuery(QUERY);
    assertThat(response.getEvents()).isEmpty();
  }

  @Test
  public void executeQueryCalledSuccessfullyParseQueryResults()
      throws Exception {
    String result = "[{\"segmentId\":\"wikipedia_2016-06-27T14:00:00.000Z_2016-06-27T15:00:00.000Z_2021-12-11T11:12:16.106Z\",\"columns\":[\"__time\",\"channel\",\"cityName\",\"comment\",\"countryIsoCode\",\"countryName\",\"diffUrl\",\"flags\",\"isAnonymous\",\"isMinor\",\"isNew\",\"isRobot\",\"isUnpatrolled\",\"metroCode\",\"namespace\",\"page\",\"regionIsoCode\",\"regionName\",\"user\",\"sum_deleted\",\"sum_deltaBucket\",\"sum_added\",\"sum_commentLength\",\"count\",\"sum_delta\"],\"events\":[{\"__time\":1467036000000,\"channel\":\"#de.wikipedia\",\"cityName\":null,\"comment\":\"Bitte [[WP:Literatur]] beachten.\",\"countryIsoCode\":null,\"countryName\":null,\"diffUrl\":\"https://de.wikipedia.org/w/index.php?diff=155672392&oldid=155667393\",\"flags\":null,\"isAnonymous\":\"false\",\"isMinor\":\"false\",\"isNew\":\"false\",\"isRobot\":\"false\",\"isUnpatrolled\":\"false\",\"metroCode\":null,\"namespace\":\"Main\",\"page\":\"Walfang\",\"regionIsoCode\":null,\"regionName\":null,\"user\":\"Dansker\",\"sum_deleted\":133,\"sum_deltaBucket\":-200,\"sum_added\":0,\"sum_commentLength\":32,\"count\":1,\"sum_delta\":-133}]}]";
    InputStream inputStream =
        new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    when(httpResponseBody.byteStream()).thenReturn(inputStream);

    DruidScanResponse response = druidQueryClient.executeQuery(QUERY);
    assertThat(response.getEvents()).isNotEmpty();
    assertThat(response.getEvents().size()).isEqualTo(1);
    assertThat(response.getEvents().get(0).get("user").textValue()).isEqualTo("Dansker");
    assertThat(response.getEvents().get(0).get("sum_deleted").intValue()).isEqualTo(133);
  }
}
