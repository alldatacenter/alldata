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
package org.apache.drill.exec.physical.impl.common;

import org.apache.drill.common.logical.data.NamedExpression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.planner.common.JoinControl;

import java.util.List;

@JsonTypeName("hashtable-config")
public class HashTableConfig  {

  private final int initialCapacity;
  private final boolean initialSizeIsFinal;
  private final float loadFactor;
  private final List<NamedExpression> keyExprsBuild;
  private final List<NamedExpression> keyExprsProbe;
  private final List<Comparator> comparators;
  private final int joinControl;

  public HashTableConfig(
      int initialCapacity,
      float loadFactor,
      List<NamedExpression> keyExprsBuild,
      List<NamedExpression> keyExprsProbe,
      List<Comparator> comparators
  ) {
    this(initialCapacity, false, loadFactor, keyExprsBuild, keyExprsProbe, comparators, JoinControl.DEFAULT);
  }

  @JsonCreator
  public HashTableConfig(@JsonProperty("initialCapacity") int initialCapacity,
                         @JsonProperty("loadFactor") float loadFactor,
                         @JsonProperty("keyExprsBuild") List<NamedExpression> keyExprsBuild,
                         @JsonProperty("keyExprsProbe") List<NamedExpression> keyExprsProbe,
                         @JsonProperty("comparators") List<Comparator> comparators,
                         @JsonProperty("joinControl") int joinControl) {
    this(initialCapacity, false, loadFactor, keyExprsBuild, keyExprsProbe, comparators, joinControl);
  }

  public HashTableConfig(
      int initialCapacity,
      boolean initialSizeIsFinal,
      float loadFactor,
      List<NamedExpression> keyExprsBuild,
      List<NamedExpression> keyExprsProbe,
      List<Comparator> comparators
  ) {
    this(initialCapacity, initialSizeIsFinal, loadFactor, keyExprsBuild, keyExprsProbe, comparators, JoinControl.DEFAULT);
  }

  @JsonCreator
  public HashTableConfig(@JsonProperty("initialCapacity") int initialCapacity,
                         @JsonProperty("initialCapacity") boolean initialSizeIsFinal,
                         @JsonProperty("loadFactor") float loadFactor,
                         @JsonProperty("keyExprsBuild") List<NamedExpression> keyExprsBuild,
                         @JsonProperty("keyExprsProbe") List<NamedExpression> keyExprsProbe,
                         @JsonProperty("comparators") List<Comparator> comparators,
                         @JsonProperty("joinControl") int joinControl
  ) {
    this.initialCapacity = initialCapacity;
    this.initialSizeIsFinal = initialSizeIsFinal;
    this.loadFactor = loadFactor;
    this.keyExprsBuild = keyExprsBuild;
    this.keyExprsProbe = keyExprsProbe;
    this.comparators = comparators;
    this.joinControl = joinControl;
  }

  public HashTableConfig withInitialCapacity(int initialCapacity) {
    return new HashTableConfig(initialCapacity,
      initialSizeIsFinal,
      loadFactor,
      keyExprsBuild,
      keyExprsProbe,
      comparators,
      JoinControl.DEFAULT
    );
  }

  public int getInitialCapacity() {
    return initialCapacity;
  }

  public boolean getInitialSizeIsFinal() {
    return initialSizeIsFinal;
  }

  public float getLoadFactor() {
    return loadFactor;
  }

  public List<NamedExpression> getKeyExprsBuild() {
    return keyExprsBuild;
  }

  public List<NamedExpression> getKeyExprsProbe() {
    return keyExprsProbe;
  }

  public List<Comparator> getComparators() {
    return comparators;
  }

  public int getJoinControl() {
    return joinControl;
  }
}
