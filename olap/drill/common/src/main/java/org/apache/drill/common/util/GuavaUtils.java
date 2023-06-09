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
package org.apache.drill.common.util;

import java.util.List;

/**
 * Utility class which contain methods for conversion guava and shaded guava classes.
 * Once outer libraries API remove usage of these classes, these methods should be removed.
 */
public class GuavaUtils {

  /**
   * Transforms specified list of lists into {@link com.google.common.collect.ImmutableList} of
   * {@link com.google.common.collect.ImmutableList} lists to pass it into the methods from other libraries.
   * @param tuples list to be transformed
   * @return transformed list
   */
  public static <T> com.google.common.collect.ImmutableList
      <com.google.common.collect.ImmutableList<T>> convertToNestedUnshadedImmutableList(List<? extends List<T>> tuples) {
    com.google.common.collect.ImmutableList.Builder<com.google.common.collect.ImmutableList<T>> immutableListBuilder =
        com.google.common.collect.ImmutableList.builder();
    for (List<T> tuple : tuples) {
      immutableListBuilder.add(convertToUnshadedImmutableList(tuple));
    }
    return immutableListBuilder.build();
  }

  public static <T> com.google.common.collect.ImmutableList<T> convertToUnshadedImmutableList(List<? extends T> source) {
    return com.google.common.collect.ImmutableList.copyOf(source);
  }
}
