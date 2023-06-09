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

public class OffsetPaginator extends Paginator {

  private static final Logger logger = LoggerFactory.getLogger(OffsetPaginator.class);

  private final int limit;
  private final String limitParam;
  private final String offsetParam;
  private int offset;

  /**
   * This class implements the idea of an Offset Paginator. See here for complete explanation:
   * https://nordicapis.com/everything-you-need-to-know-about-api-pagination/
   * <p>
   *
   * @param builder     The okhttp3 URL builder which has the API root URL
   * @param limit       The limit clause from the sql query
   * @param pageSize    The page size from the API documentation. To minimize requests, it should be set to the max that the API allows.
   * @param limitParam  The field name which corresponds to the limit field from the API
   * @param offsetParam The field name which corresponds to the offset field from the API
   */
  public OffsetPaginator(Builder builder, int limit, int pageSize, String limitParam, String offsetParam) {
    super(builder, paginationMode.OFFSET, pageSize, limit);
    this.limit = limit;
    this.limitParam = limitParam;
    this.offsetParam = offsetParam;
    this.offset = 0;

    // Page size must be greater than zero
    if (pageSize <= 0) {
      throw UserException
              .validationError()
              .message("API page size must be a positive integer.")
              .build(logger);
    }
  }

  @Override
  public boolean hasNext() {
    return !partialPageReceived && (limit < 0 || offset < limit);
  }

  @Override
  public String next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    builder.removeAllEncodedQueryParameters(offsetParam);
    builder.removeAllEncodedQueryParameters(limitParam);

    builder.addQueryParameter(offsetParam, String.valueOf(offset));
    builder.addQueryParameter(limitParam, String.valueOf(pageSize));
    offset += pageSize;

    return builder.build().url().toString();
  }
}
