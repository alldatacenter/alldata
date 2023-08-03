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

package com.netease.arctic.server.dashboard.response;

import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageResult<R> {
  private static final PageResult<?> EMPTY = new PageResult<>(0);
  private List<R> list;
  private int total;

  private PageResult(List<R> list, int total) {
    this.list = list;
    this.total = total;
  }

  private PageResult(int total) {
    this.list = Collections.emptyList();
    this.total = total;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public static PageResult<?> empty() {
    return EMPTY;
  }

  public List<R> getList() {
    return list;
  }

  public void setList(List<R> list) {
    this.list = list;
  }

  /**
   * x.
   * @param list  the results have been intercepted
   * @param total original size
   * @return response contains paging information response
   */
  public static <R> PageResult<R> of(List<R> list, int total) {
    return new PageResult<>(list, total);
  }

  /**
   * generate paging results.
   * @param list   original queue
   * @param offset offset
   * @param limit  limit
   * @return response containing paging information
   */
  public static <R> PageResult<R> of(List<R> list, int offset, int limit) {
    if (CollectionUtils.isEmpty(list)) {
      return new PageResult<>(Collections.emptyList(), 0);
    } else {
      List<R> result = list.stream().skip(offset).limit(limit).collect(Collectors.toList());
      return new PageResult<>(result, list.size());
    }
  }

  public static <T,R> PageResult<R> of(List<T> list, int offset, int limit, Function<T,R> convert) {
    if (CollectionUtils.isEmpty(list)) {
      return new PageResult<>(Collections.emptyList(), 0);
    } else {
      List<R> result = list.stream().skip(offset).limit(limit).map(convert).collect(Collectors.toList());
      return new PageResult<>(result, list.size());
    }
  }
}
