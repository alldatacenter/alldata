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

import org.apache.drill.common.types.TypeProtos;

/**
 * Used to represent path to nested field within schema as a chain of path segments.
 * Where each segment in the chain may be either named or array segment.
 *
 * @see NameSegment
 * @see ArraySegment
 * @see SchemaPath
 */
public abstract class PathSegment {

  /**
   * Holds original value associated with the path segment.
   * Used when reading data from DICT.
   */
  protected final Object originalValue;

  /**
   * Indicates the type of original value.
   * @see #originalValue
   */
  protected final TypeProtos.MajorType originalValueType;

  private PathSegment child;

  private int hash;

  public PathSegment(PathSegment child, Object originalValue, TypeProtos.MajorType originalValueType) {
    this.originalValue = originalValue;
    this.originalValueType = originalValueType;
    this.child = child;
  }

  /**
   * Makes copy of segment chain with {@code newChild} added at the end.
   *
   * @param newChild new child to add
   * @return full copy of segment chain with new child added at the end of chain
   */
  public abstract PathSegment cloneWithNewChild(PathSegment newChild);

  @Override
  public abstract PathSegment clone();

  public Object getOriginalValue() {
    return originalValue;
  }

  public TypeProtos.MajorType getOriginalValueType() {
    return originalValueType;
  }

  public static final class ArraySegment extends PathSegment {
    private final int index;

    public ArraySegment(String numberAsText, PathSegment child) {
      this(Integer.parseInt(numberAsText), child);
    }

    public ArraySegment(String numberAsText) {
      this(Integer.parseInt(numberAsText), null);
    }

    public ArraySegment(int index, PathSegment child) {
      super(child, index, null);
      this.index = index;
      assert index >= 0;
    }

    public ArraySegment(PathSegment child) {
      super(child, -1, null);
      this.index = -1;
    }

    public ArraySegment(int index) {
      this(index, null);
      if (index < 0 ) {
        throw new IllegalArgumentException();
      }
    }

    public ArraySegment(int index, Object originalValue, TypeProtos.MajorType valueType) {
      super(null, originalValue, valueType);
      this.index = index;
    }

    public boolean hasIndex() {
      return index != -1;
    }

    public int getIndex() {
      return index;
    }

    @Override
    public boolean isArray() {
      return true;
    }

    @Override
    public boolean isNamed() {
      return false;
    }

    @Override
    public ArraySegment getArraySegment() {
      return this;
    }

    @Override
    public String toString() {
      return "ArraySegment [index=" + index + ", getChild()=" + getChild() + "]";
    }

    @Override
    public int segmentHashCode() {
      return index;
    }

    @Override
    public boolean segmentEquals(PathSegment obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (obj instanceof ArraySegment) {
        return index == ((ArraySegment)obj).getIndex();
      }
      return false;
    }

    @Override
    public PathSegment clone() {
      int index = this.index < 0 ? -1 : this.index;
      PathSegment seg = new ArraySegment(index, originalValue, originalValueType);
      if (getChild() != null) {
        seg.setChild(getChild().clone());
      }
      return seg;
    }

    @Override
    public ArraySegment cloneWithNewChild(PathSegment newChild) {
      int index = this.index < 0 ? -1 : this.index;
      ArraySegment seg = new ArraySegment(index, originalValue, originalValueType);
      if (getChild() != null) {
        seg.setChild(getChild().cloneWithNewChild(newChild));
      } else {
        seg.setChild(newChild);
      }
      return seg;
    }
  }

  public static final class NameSegment extends PathSegment {
    private final String path;

    public NameSegment(CharSequence n, Object originalValue, TypeProtos.MajorType valueType) {
      super(null, originalValue, valueType);
      this.path = n.toString();
    }

    public NameSegment(CharSequence n, PathSegment child) {
      super(child, n.toString(), null);
      this.path = n.toString();
    }

    public NameSegment(CharSequence n) {
      this(n, null);
    }

    public String getPath() { return path; }

    @Override
    public boolean isArray() { return false; }

    @Override
    public boolean isNamed() { return true; }

    @Override
    public NameSegment getNameSegment() { return this; }

    @Override
    public String toString() {
      return "NameSegment [path=" + path + ", getChild()=" + getChild() + "]";
    }

    @Override
    public int segmentHashCode() {
      return ((path == null) ? 0 : path.toLowerCase().hashCode());
    }

    @Override
    public boolean segmentEquals(PathSegment obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (getClass() != obj.getClass()) {
        return false;
      }

      NameSegment other = (NameSegment) obj;
      if (path == null) {
        return other.path == null;
      }
      return path.equalsIgnoreCase(other.path);
    }

    public boolean nameEquals(String name) {
      return path == null && name == null ||
             path != null && path.equalsIgnoreCase(name);
    }

    @Override
    public NameSegment clone() {
      NameSegment s = new NameSegment(this.path, originalValue, originalValueType);
      if (getChild() != null) {
        s.setChild(getChild().clone());
      }
      return s;
    }

    @Override
    public NameSegment cloneWithNewChild(PathSegment newChild) {
      NameSegment s = new NameSegment(this.path, originalValue, originalValueType);
      if (getChild() != null) {
        s.setChild(getChild().cloneWithNewChild(newChild));
      } else {
        s.setChild(newChild);
      }
      return s;
    }
  }

  public NameSegment getNameSegment() {
    throw new UnsupportedOperationException();
  }

  public ArraySegment getArraySegment() {
    throw new UnsupportedOperationException();
  }

  public abstract boolean isArray();
  public abstract boolean isNamed();

  public boolean isLastPath() {
    return child == null;
  }

  public PathSegment getChild() {
    return child;
  }

  void setChild(PathSegment child) {
    this.child = child;
  }

  protected abstract int segmentHashCode();
  protected abstract boolean segmentEquals(PathSegment other);

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = segmentHashCode();
      h = h + ((child == null) ? 0 : 31 * child.hashCode());
      hash = h;
    }
    return h;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    PathSegment other = (PathSegment) obj;
    if (!segmentEquals(other)) {
      return false;
    } else if (child == null) {
      return (other.child == null);
    } else {
      return child.equals(other.child);
    }
  }

  /**
   * Check if another path is contained in this one. This is useful for 2 cases. The first
   * is checking if the other is lower down in the tree, below this path. The other is if
   * a path is actually contained above the current one.
   *
   * Examples:
   * [a] . contains( [a.b.c] ) returns true
   * [a.b.c] . contains( [a] ) returns true
   *
   * This behavior is used for cases like scanning json in an event based fashion, when we arrive at
   * a node in a complex type, we will know the complete path back to the root. This method can
   * be used to determine if we need the data below. This is true in both the cases where the
   * column requested from the user is below the current node (in which case we may ignore other nodes
   * further down the tree, while keeping others). This is also the case if the requested path is further
   * up the tree, if we know we are at position a.b.c and a.b was a requested column, we need to scan
   * all of the data at and below the current a.b.c node.
   *
   * @param otherSeg - path segment to check if it is contained below this one.
   * @return - is this a match
   */

  public boolean contains(PathSegment otherSeg) {
    if (this == otherSeg) {
      return true;
    }
    if (otherSeg == null) {
      return false;
    }
    // TODO - fix this in the future to match array segments are part of the path
    // the current behavior to always return true when we hit an array may be useful in some cases,
    // but we can get better performance in the JSON reader if we avoid reading unwanted elements in arrays
    if (otherSeg.isArray() || this.isArray()) {
      return true;
    }
    if (getClass() != otherSeg.getClass()) {
      return false;
    }

    if (!segmentEquals(otherSeg)) {
      return false;
    }
    else if (child == null || otherSeg.child == null) {
      return true;
    } else {
      return child.contains(otherSeg.child);
    }
  }
}
