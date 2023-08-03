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

import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

public class OkResponse<R> extends Response {
  private static final OkResponse<?> OK = new OkResponse<>();
  private R result;

  protected OkResponse(R result) {
    super(200, "success");
    this.result = result;
  }

  protected OkResponse(String message, R result) {
    super(200, message);
    this.result = result;
  }

  protected OkResponse() {
    this(null);
  }

  public static OkResponse<?> ok() {
    return OK;
  }

  public static <R> OkResponse<R> of(R result) {
    return new OkResponse<>(result);
  }

  public static <R> OkResponse<R> of(String message, R result) {
    return new OkResponse<>(message, result);
  }

  public R getResult() {
    return result;
  }

  public void setResult(R result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("result", result)
        .toString();
  }
}
