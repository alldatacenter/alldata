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
package org.apache.ambari.server.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Static Helper methods for sets
 */
public class SetUtils {

  /**
   * Split a set into subsets
   * @param original The original set to be split
   * @param subsetSize Size of the subset (except for final subset)
   * @param <T> Data type of set elements
   * @return List of subsets
   */
  public static <T> List<Set<T>> split(Set<T> original, int subsetSize) {

    if(subsetSize <= 0) {
      throw new IllegalArgumentException("Incorrect max size");
    }

    if(original == null || original.isEmpty()) {
      return Collections.emptyList();
    }

    int subsetCount = (int) (Math.ceil((double)original.size() / subsetSize));
    ArrayList<Set<T>> subsets = new ArrayList<>(subsetCount);
    Iterator<T> iterator = original.iterator();

    for(int i = 0; i < subsetCount; i++) {
      Set<T> subset = new LinkedHashSet<>(subsetSize);
      for(int j = 0; j < subsetSize && iterator.hasNext(); j++) {
        subset.add(iterator.next());
      }
      subsets.add(subset);
    }
    return subsets;
  }
}
