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
package org.apache.drill.common.expression;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import org.apache.drill.common.expression.PathSegment.ArraySegment;
import org.apache.drill.common.expression.PathSegment.NameSegment;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.parser.LogicalExpressionParser;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.proto.UserBitShared.NamePart;
import org.apache.drill.exec.proto.UserBitShared.NamePart.Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * This is the path for the column in the table
 */
public class SchemaPath extends LogicalExpressionBase {

  // AKA "Wildcard": expand all columns
  public static final String DYNAMIC_STAR = "**";
  public static final SchemaPath STAR_COLUMN = getSimplePath(DYNAMIC_STAR);

  private final NameSegment rootSegment;

  public SchemaPath(SchemaPath path) {
    this(path.rootSegment, path.getPosition());
  }

  public SchemaPath(NameSegment rootSegment) {
    this(rootSegment, ExpressionPosition.UNKNOWN);
  }

  /**
   * @deprecated Use {@link #SchemaPath(NameSegment)}
   * or {@link #SchemaPath(NameSegment, ExpressionPosition)} instead
   */
  @Deprecated
  public SchemaPath(String simpleName, ExpressionPosition pos) {
    this(new NameSegment(simpleName), pos);
  }

  public SchemaPath(NameSegment rootSegment, ExpressionPosition pos) {
    super(pos);
    this.rootSegment = rootSegment;
  }

  public static SchemaPath getSimplePath(String name) {
    return getCompoundPath(name);
  }

  public static SchemaPath getCompoundPath(String... path) {
    return getCompoundPath(path.length, path);
  }

  /**
   * Constructs {@code SchemaPath} based on given {@code path} array up to {@literal n}th element (inclusively).
   *
   * Example: for case when {@code n = 2} and {@code path = {"a", "b", "c", "d", "e", ...}}
   * the method returns {@code a.b}
   *
   * @param n number of elements in {@literal path} array to take when constructing {@code SchemaPath}
   * @param path column path used to construct schema path
   * @return schema path containing {@literal n - 1} children
   */
  public static SchemaPath getCompoundPath(int n, String... path) {
    Preconditions.checkArgument(n > 0);
    NameSegment s = null;
    // loop through strings in reverse order
    for (int i = n - 1; i >= 0; i--) {
      s = new NameSegment(path[i], s);
    }
    return new SchemaPath(s);
  }

  public PathSegment getLastSegment() {
    PathSegment s = rootSegment;
    while (s.getChild() != null) {
      s = s.getChild();
    }
    return s;
  }

  public NamePart getAsNamePart() {
    return getNamePart(rootSegment);
  }

  private static NamePart getNamePart(PathSegment s) {
    if (s == null) {
      return null;
    }
    NamePart.Builder b = NamePart.newBuilder();
    if (s.getChild() != null) {
      NamePart namePart = getNamePart(s.getChild());
      if (namePart != null) {
        b.setChild(namePart);
      }
    }

    if (s.isArray()) {
      if (s.getArraySegment().hasIndex()) {
        throw new IllegalStateException("You cannot convert a indexed schema path to a NamePart.  NameParts can only reference Vectors, not individual records or values.");
      }
      b.setType(Type.ARRAY);
    } else {
      b.setType(Type.NAME);
      b.setName(s.getNameSegment().getPath());
    }
    return b.build();
  }

  private static PathSegment getPathSegment(NamePart n) {
    PathSegment child = n.hasChild() ? getPathSegment(n.getChild()) : null;
    if (n.getType() == Type.ARRAY) {
      return new ArraySegment(child);
    } else {
      return new NameSegment(n.getName(), child);
    }
  }

  public static SchemaPath create(NamePart namePart) {
    Preconditions.checkArgument(namePart.getType() == NamePart.Type.NAME);
    return new SchemaPath((NameSegment) getPathSegment(namePart));
  }

  /**
   * Returns schema path with for arrays without index.
   * Is used to find column statistics in parquet metadata.
   * Example: a.b.c[0] -> a.b.c, a[0].b[1] -> a.b
   *
   * @return un-indexed schema path
   */
  public SchemaPath getUnIndexed() {
    return new SchemaPath(getUnIndexedNameSegment(rootSegment, null));
  }

  /**
   * Traverses path segment to extract named segments, omits all other segments (i.e. array).
   * Order of named segments appearance will be preserved.
   *
   * @param currentSegment current segment
   * @param resultingSegment resulting segment
   * @return named segment
   */
  private NameSegment getUnIndexedNameSegment(PathSegment currentSegment, NameSegment resultingSegment) {
    if (!currentSegment.isLastPath()) {
      resultingSegment = getUnIndexedNameSegment(currentSegment.getChild(), resultingSegment);
    }

    if (currentSegment.isNamed()) {
      String path = currentSegment.getNameSegment().getPath();
      return new NameSegment(path, resultingSegment);
    }

    return resultingSegment;
  }

  /**
   * Parses input string using the same rules which are used for the field in the query.
   * If a string contains dot outside back-ticks, or there are no backticks in the string,
   * will be created {@link SchemaPath} with the {@link NameSegment}
   * which contains one else {@link NameSegment}, etc.
   * If a string contains [] then {@link ArraySegment} will be created.
   *
   * @param expr input string to be parsed
   * @return {@link SchemaPath} instance
   */
  public static SchemaPath parseFromString(String expr) {
    if (expr == null || expr.isEmpty()) {
      return null;
    }

    if (SchemaPath.DYNAMIC_STAR.equals(expr)) {
      return SchemaPath.getSimplePath(expr);
    }

    LogicalExpression logicalExpression = LogicalExpressionParser.parse(expr);
    if (logicalExpression instanceof SchemaPath) {
      return (SchemaPath) logicalExpression;
    } else {
      throw new IllegalStateException(String.format("Schema path is not a valid format: %s.", logicalExpression));
    }
  }

  /**
   * A simple is a path where there are no repeated elements outside the lowest level of the path.
   * @return Whether this path is a simple path.
   */
  public boolean isSimplePath() {
    PathSegment seg = rootSegment;
    while (seg != null) {
      if (seg.isArray() && !seg.isLastPath()) {
        return false;
      }
      seg = seg.getChild();
    }
    return true;
  }

  /**
   * Return whether this name refers to an array. The path must be an array if it
   * ends with an array index; else it may or may not be an entire array.
   *
   * @return true if the path ends with an array index, false otherwise
   */

  public boolean isArray() {
    PathSegment seg = rootSegment;
    while (seg != null) {
      if (seg.isArray()) {
        return true;
      }
      seg = seg.getChild();
    }
    return false;
  }

  /**
   * Determine if this is a one-part name. In general, special columns work only
   * if they are single-part names.
   *
   * @return true if this is a one-part name, false if this is a multi-part
   * name (with either map member or array index parts.)
   */

  public boolean isLeaf() {
    return rootSegment.isLastPath();
  }

  /**
   * Return if this column is the special wildcard ("**") column which means to
   * project all table columns.
   *
   * @return true if the column is "**"
   */

  public boolean isDynamicStar() {
    return isLeaf() && nameEquals(DYNAMIC_STAR);
  }

  /**
   * Returns if this is a simple column and the name matches the given
   * name (ignoring case.) This does not check if the name is an entire
   * match, only the the first (or only) part of the name matches.
   * Also check {@link #isLeaf()} to check for a single-part name.
   *
   * @param name name to match
   * @return true if this is a single-part column with that name.
   */

  public boolean nameEquals(String name) {
    return rootSegment.nameEquals(name);
  }

  /**
   * Return the root name: either the entire name (if one part) or
   * the first part (if multi-part.)
   * <ul>
   * <li>a: returns a</li>
   * <li>a.b: returns a</li>
   * <li>a[10]: returns a</li>
   * </ul>
   *
   * @return the root (or only) name
   */

  public String rootName() {
    return rootSegment.getPath();
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitSchemaPath(this, value);
  }

  public SchemaPath getChild(String childPath) {
    NameSegment newRoot = rootSegment.cloneWithNewChild(new NameSegment(childPath));
    return new SchemaPath(newRoot);
  }

  public SchemaPath getChild(String childPath, Object originalValue, TypeProtos.MajorType valueType) {
    NameSegment newRoot = rootSegment.cloneWithNewChild(new NameSegment(childPath, originalValue, valueType));
    return new SchemaPath(newRoot);
  }

  public SchemaPath getChild(int index) {
    NameSegment newRoot = rootSegment.cloneWithNewChild(new ArraySegment(index));
    return new SchemaPath(newRoot);
  }

  public SchemaPath getChild(int index, Object originalValue, TypeProtos.MajorType valueType) {
    NameSegment newRoot = rootSegment.cloneWithNewChild(new ArraySegment(index, originalValue, valueType));
    return new SchemaPath(newRoot);
  }

  public NameSegment getRootSegment() {
    return rootSegment;
  }

  public String getAsUnescapedPath() {
    StringBuilder sb = new StringBuilder();
    PathSegment seg = getRootSegment();
    if (seg.isArray()) {
      throw new IllegalStateException("Drill doesn't currently support top level arrays");
    }
    sb.append(seg.getNameSegment().getPath());

    while ( (seg = seg.getChild()) != null) {
      if (seg.isNamed()) {
        sb.append('.');
        sb.append(seg.getNameSegment().getPath());
      } else {
        sb.append('[');
        sb.append(seg.getArraySegment().getIndex());
        sb.append(']');
      }
    }
    return sb.toString();
  }

  @Override
  public MajorType getMajorType() {
    return Types.LATE_BIND_TYPE;
  }

  @Override
  public int hashCode() {
    return ((rootSegment == null) ? 0 : rootSegment.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof SchemaPath
        && Objects.equals(rootSegment, ((SchemaPath) obj).rootSegment);
  }

  public boolean contains(SchemaPath path) {
    return this == path || path != null
        && (rootSegment == null || rootSegment.contains(path.rootSegment));
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public String toString() {
    return ExpressionStringBuilder.toString(this);
  }

  public String toExpr() {
    return ExpressionStringBuilder.toString(this);
  }

  /**
   * Returns path string of {@code rootSegment}
   *
   * @return path string of {@code rootSegment}
   */
  public String getRootSegmentPath() {
    return rootSegment.getPath();
  }

  @SuppressWarnings("serial")
  public static class De extends StdDeserializer<SchemaPath> {

    public De() {
      super(LogicalExpression.class);
    }

    @Override
    public SchemaPath deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      return parseFromString(jp.getText());
    }
  }
}
