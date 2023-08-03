/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow.view;

import com.google.common.base.Objects;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.StructLike;

import java.util.List;

public class MatchResult {

  private final List<? extends StructLike> notInView;

  private final List<? extends StructLike> inViewButCountError;

  private final List<? extends StructLike> inViewButMiss;

  private MatchResult(
      List<? extends StructLike> notInView,
      List<? extends StructLike> inViewButCountError,
      List<? extends StructLike> inViewButMiss) {
    this.notInView = notInView;
    this.inViewButCountError = inViewButCountError;
    this.inViewButMiss = inViewButMiss;
  }

  public static MatchResult of(
      List<? extends StructLike> notInView,
      List<? extends StructLike> inViewButDuplicate,
      List<? extends StructLike> missInView) {
    return new MatchResult(notInView, inViewButDuplicate, missInView);
  }

  public static MatchResult ok() {
    return new MatchResult(null, null, null);
  }

  public boolean isOk() {
    return CollectionUtils.isEmpty(notInView) &&
        CollectionUtils.isEmpty(inViewButCountError) &&
        CollectionUtils.isEmpty(inViewButMiss);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("notInView", notInView)
        .add("inViewButCountError", inViewButCountError)
        .add("inViewButMiss", inViewButMiss)
        .toString();
  }
}
