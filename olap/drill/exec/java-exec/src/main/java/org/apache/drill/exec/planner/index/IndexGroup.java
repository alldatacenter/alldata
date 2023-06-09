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
package org.apache.drill.exec.planner.index;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import java.util.List;

/**
 * Encapsulates one or more IndexProperties representing (non)covering or intersecting indexes. The encapsulated
 * IndexProperties are used to rank the index in comparison with other IndexGroups.
 */
public class IndexGroup {
  private List<IndexProperties> indexProps;

  public IndexGroup() {
    indexProps = Lists.newArrayList();
  }

  public boolean isIntersectIndex() {
    return indexProps.size() > 1;
  }

  public int numIndexes() {
    return indexProps.size();
  }

  public void addIndexProp(IndexProperties prop) {
    indexProps.add(prop);
  }

  public void addIndexProp(List<IndexProperties> prop) {
    indexProps.addAll(prop);
  }

  public boolean removeIndexProp(IndexProperties prop) {
    return indexProps.remove(prop);
  }

  public List<IndexProperties> getIndexProps() {
    return indexProps;
  }
}

