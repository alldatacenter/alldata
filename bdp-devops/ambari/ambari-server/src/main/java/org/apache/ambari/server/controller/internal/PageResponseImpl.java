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

import org.apache.ambari.server.controller.spi.PageResponse;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Basic page response implementation.
 */
public class PageResponseImpl implements PageResponse{
  private final Iterable<Resource> iterable;
  private final int offset;
  private final Resource previousResource;
  private final Resource nextResource;
  private final Integer totalResourceCount;

  public PageResponseImpl(Iterable<Resource> iterable, int offset,
                          Resource previousResource, Resource nextResource,
                          Integer totalResourceCount) {
    this.iterable = iterable;
    this.offset = offset;
    this.previousResource = previousResource;
    this.nextResource = nextResource;
    this.totalResourceCount = totalResourceCount;
  }

  @Override
  public Iterable<Resource> getIterable() {
    return iterable;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public Resource getPreviousResource() {
    return previousResource;
  }

  @Override
  public Resource getNextResource() {
    return nextResource;
  }

  @Override
  public Integer getTotalResourceCount() {
    return totalResourceCount;
  }
}
