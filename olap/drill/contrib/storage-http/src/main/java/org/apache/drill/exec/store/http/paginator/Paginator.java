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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * This class is the abstraction for the Paginator class.  There are
 * different pagination methods, however, all essentially require the query
 * engine to generate URLs to retrieve the next batch of data and also
 * to determine whether the URL has more data.
 *
 * The Offset and Page paginators work either with a limit or without, but function
 * slightly differently.  If a limit is specified and pushed down, the paginator will
 * generate a list of URLs with the appropriate pagination parameters.  In the future
 * this could be parallelized, however in the V1 all these requests are run in series.
 */
public abstract class Paginator implements Iterator<String> {

  private static final Logger logger = LoggerFactory.getLogger(Paginator.class);
  private static final int MAX_ATTEMPTS = 100;
  protected final int pageSize;

  public enum paginationMode {
    OFFSET,
    PAGE
  }

  protected final paginationMode MODE;
  protected final int limit;
  protected boolean partialPageReceived;
  protected Builder builder;

  public Paginator(Builder builder, paginationMode mode, int pageSize, int limit) {
    this.MODE = mode;
    this.builder = builder;
    this.pageSize = pageSize;
    this.limit = limit;
    this.partialPageReceived = false;
  }

  public void setBuilder(Builder builder) {
    this.builder = builder;
  }

  /**
   * This method is used in pagination queries when no limit is present.  The intended
   * flow is that if no limit is present, if the batch reader encounters a page which has
   * less data than the page size, the batch reader should call this method to stop
   * the Paginator from generating additional URLs to call.
   *
   * In the event that the API simply runs out of data, the reader will return false anyway
   * and the pagination will stop.
   */
  public void notifyPartialPage() {
    partialPageReceived = true;
    logger.debug("Ending pagination: partial page received");
  }

  public int getPageSize() { return pageSize; }
}
