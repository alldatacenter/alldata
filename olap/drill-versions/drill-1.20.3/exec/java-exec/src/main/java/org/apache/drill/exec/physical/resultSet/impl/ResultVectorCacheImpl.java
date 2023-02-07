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
package org.apache.drill.exec.physical.resultSet.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Manages an inventory of value vectors used across row batch readers.
 * Drill semantics for batches is complex. Each operator logically returns
 * a batch of records on each call of the Drill Volcano iterator protocol
 * <tt>next()</tt> operation. However, the batches "returned" are not
 * separate objects. Instead, Drill enforces the following semantics:
 * <ul>
 * <li>If a <tt>next()</tt> call returns <tt>OK</tt> then the set of vectors
 * in the "returned" batch must be identical to those in the prior batch. Not
 * just the same type; they must be the same <tt>ValueVector</tt> objects.
 * (The buffers within the vectors will be different.)</li>
 * <li>If the set of vectors changes in any way (add a vector, remove a
 * vector, change the type of a vector), then the <tt>next()</tt> call
 * <b>must</b> return <tt>OK_NEW_SCHEMA</tt>.</ul>
 * </ul>
 * These rules create interesting constraints for the scan operator.
 * Conceptually, each batch is distinct. But, it must share vectors. The
 * {@link ResultSetLoader} class handles this by managing the set of vectors
 * used by a single reader.
 * <p>
 * Readers are independent: each may read a distinct schema (as in JSON.)
 * Yet, the Drill protocol requires minimizing spurious <tt>OK_NEW_SCHEMA</tt>
 * events. As a result, two readers run by the same scan operator must
 * share the same set of vectors, despite the fact that they may have
 * different schemas and thus different <tt>ResultSetLoader</tt>s.
 * <p>
 * The purpose of this inventory is to persist vectors across readers, even
 * when, say, reader B does not use a vector that reader A created.
 * <p>
 * The semantics supported by this class include:
 * <ul>
 * <li>Ability to "pre-declare" columns based on columns that appear in
 * an explicit select list. This ensures that the columns are known (but
 * not their types).</li>
 * <li>Ability to reuse a vector across readers if the column retains the same
 * name and type (minor type and mode.)</li>
 * <li>Ability to flush unused vectors for readers with changing schemas
 * if a schema change occurs.</li>
 * <li>Support schema "hysteresis"; that is, the a "sticky" schema that
 * minimizes spurious changes. Once a vector is declared, it can be included
 * in all subsequent batches (provided the column is nullable or an array.)</li>
 * </ul>
 */
public class ResultVectorCacheImpl implements ResultVectorCache {

  /**
   * State of a projected vector. At first all we have is a name.
   * Later, we'll discover the type.
   */

  private static class VectorState {
    protected final String name;
    protected ValueVector vector;
    protected boolean touched;

    public VectorState(String name) {
      this.name = name;
    }

    public boolean satisfies(MaterializedField colSchema, boolean permissive) {
      if (vector == null) {
        return false;
      }
      MaterializedField vectorSchema = vector.getField();
      if (permissive) {
        return colSchema.isPromotableTo(vectorSchema, true);
      } else {
        return Types.isEquivalent(vectorSchema.getType(),
            colSchema.getType());
      }
    }
  }

  private final BufferAllocator allocator;

  /**
   * Permissive mode loosens the rules for finding a match.
   * <ul>
   * <li>A request for a non-nullable vector matches a nullable
   * vector in the cache.</li>
   * <li>A request for a smaller precision Varchar matches a
   * larger precision Varchar in the cache.</li>
   * </ul>
   * When not in permissive mode, an exact match is required.
   */

  private final boolean permissiveMode;
  private final Map<String, VectorState> vectors = CaseInsensitiveMap.newHashMap();
  private Map<String, ResultVectorCacheImpl> children;

  public ResultVectorCacheImpl(BufferAllocator allocator) {
    this.allocator = allocator;
    permissiveMode = false;
  }

  public ResultVectorCacheImpl(BufferAllocator allocator, boolean permissiveMode) {
    this.allocator = allocator;
    this.permissiveMode = permissiveMode;
  }

  @Override
  public BufferAllocator allocator() { return allocator; }

  public void predefine(List<String> selected) {
    for (String colName : selected) {
      addVector(colName);
    }
  }

  private VectorState addVector(String colName) {
    VectorState vs = new VectorState(colName);
    vectors.put(vs.name, vs);
    return vs;
  }

  public void newBatch() {
    for (VectorState vs : vectors.values()) {
      vs.touched = false;
    }
  }

  public void trimUnused() {
    List<VectorState> unused = new ArrayList<>();
    for (VectorState vs : vectors.values()) {
      if (! vs.touched) {
        unused.add(vs);
      }
    }
    if (unused.isEmpty()) {
      return;
    }
    for (VectorState vs : unused) {
      vectors.remove(vs.name);
    }
  }

  @Override
  public ValueVector vectorFor(MaterializedField colSchema) {
    VectorState vs = vectors.get(colSchema.getName());

    // If the vector is found, and is of the right type, reuse it.

    if (vs != null && vs.satisfies(colSchema, permissiveMode)) {
      return vs.vector;
    }

    // If no vector, this is a late schema. Create the vector.

    if (vs == null) {
      vs = addVector(colSchema.getName());

    // Else, if the vector changed type, close the old one.

    } else if (vs.vector != null) {
      vs.vector.close();
      vs.vector = null;
    }

    // Create the new vector.

    vs.touched = true;
    vs.vector = TypeHelper.getNewVector(colSchema, allocator, null);
    return vs.vector;
  }

  @Override
  public MajorType getType(String name) {
    VectorState vs = vectors.get(name);
    if (vs == null || vs.vector == null) {
      return null;
    }
    return vs.vector.getField().getType();
  }

  public void close() {
    for (VectorState vs : vectors.values()) {
      vs.vector.close();
    }
    vectors.clear();
    if (children != null) {
      for (ResultVectorCacheImpl child : children.values()) {
        child.close();
      }
      children = null;
    }
  }

  @Override
  public boolean isPermissive() { return permissiveMode; }

  @Override
  public ResultVectorCache childCache(String colName) {
    if (children == null) {
      children = new HashMap<>();
    }
    String key = colName.toLowerCase();
    ResultVectorCacheImpl child = children.get(key);
    if (child == null) {
      child = new ResultVectorCacheImpl(allocator);
      children.put(key, child);
    }
    return child;
  }
}
