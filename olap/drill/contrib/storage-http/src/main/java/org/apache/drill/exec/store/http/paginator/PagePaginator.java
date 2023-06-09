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

package org.apache.drill.exec.store.http.paginator;

import okhttp3.HttpUrl.Builder;
import org.apache.drill.common.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class PagePaginator extends Paginator {

  private static final Logger logger = LoggerFactory.getLogger(OffsetPaginator.class);

  private final int limit;
  private final String pageParam;
  private final String pageSizeParam;
  private int currentPage;

  /**
   * The Page Paginator works similarly to the offset paginator.  It requires the user to supply a page number query
   * parameter, a page size variable, and a maximum page size.
   * @param builder The okHttp3 Request builder
   * @param limit The limit, passed down from the batch reader of the maximum number of results the user wants
   * @param pageSize The number of results per page.
   * @param pageParam The API Query parameter which indicates which page number the user is requesting
   * @param pageSizeParam The API Query parameter which specifies how many results per page
   */
  public PagePaginator(Builder builder, int limit, int pageSize, String pageParam, String pageSizeParam) {
    super(builder, paginationMode.PAGE, pageSize, limit);
    this.limit = limit;
    this.pageParam = pageParam;
    this.pageSizeParam = pageSizeParam;
    currentPage = 1;

    // Page size must be greater than zero
    if (pageSize <= 0) {
      throw UserException
        .validationError()
        .message("API limit cannot be zero")
        .build(logger);
    }
  }

  @Override
  public boolean hasNext() {
    return !partialPageReceived && (limit < 0 || (currentPage-1) * pageSize < limit);
  }

  @Override
  public String next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    builder.removeAllEncodedQueryParameters(pageParam);
    builder.removeAllEncodedQueryParameters(pageSizeParam);

    builder.addQueryParameter(pageParam, String.valueOf(currentPage));
    builder.addQueryParameter(pageSizeParam, String.valueOf(pageSize));
    currentPage++;

    return builder.build().url().toString();
  }
}
