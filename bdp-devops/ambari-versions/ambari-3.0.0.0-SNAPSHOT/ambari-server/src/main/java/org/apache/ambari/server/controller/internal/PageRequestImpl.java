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

package org.apache.ambari.server.controller.internal;

import java.util.Comparator;

import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Basic page request implementation.
 */
public class PageRequestImpl implements PageRequest{

  private final StartingPoint startingPoint;
  private final int pageSize;
  private final int offset;
  private final Predicate predicate;
  private final Comparator<Resource> comparator;

  public PageRequestImpl(StartingPoint startingPoint, int pageSize, int offset, Predicate predicate,
                         Comparator<Resource> comparator) {
    this.startingPoint = startingPoint;
    this.pageSize = pageSize;
    this.offset = offset;
    this.predicate = predicate;
    this.comparator = comparator;
  }

  @Override
  public StartingPoint getStartingPoint() {
    return startingPoint;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public Predicate getPredicate() {
    return predicate;
  }
}
