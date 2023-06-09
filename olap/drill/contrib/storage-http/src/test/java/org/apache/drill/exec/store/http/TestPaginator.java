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

package org.apache.drill.exec.store.http;

import okhttp3.HttpUrl;
import org.apache.drill.exec.store.http.paginator.OffsetPaginator;
import org.apache.drill.exec.store.http.paginator.PagePaginator;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class tests the functionality of the various paginator classes as it pertains
 * to generating the correct URLS.
 */
public class TestPaginator {

  @Test
  public void TestOffsetPaginator() {
    String baseUrl = "https://myapi.com";
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();

    OffsetPaginator op = new OffsetPaginator(urlBuilder, 25, 5, "limit", "offset");

    for (int i = 0; i < 25; i += 5) {
      assertEquals(String.format("%s/?offset=%d&limit=5", baseUrl, i), op.next());
    }
    assertFalse(op.hasNext());
  }

  @Test
  public void TestPagePaginatorIterator() {
    String baseUrl = "https://api.github.com/orgs/apache/repos";
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();

    PagePaginator pp = new PagePaginator(urlBuilder, 10, 2, "page", "per_page");
    for (int i = 1; i <= 5; i++) {
      assertEquals(String.format("%s?page=%d&per_page=%d", baseUrl, i, 2), pp.next());
    }
    assertFalse(pp.hasNext());

    PagePaginator pp2 = new PagePaginator(urlBuilder, 10, 100, "page", "per_page");
    assertEquals(String.format("%s?page=%d&per_page=%d", baseUrl, 1, 100), pp2.next());
    assertFalse(pp.hasNext());
  }
}
