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
package org.apache.drill.exec.record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.types.TypeProtos.MajorType;

/**
 * Historically {@link BatchSchema} is used to represent the schema of a batch. However, it does not handle complex types well. If you have a choice, use
 * {@link org.apache.drill.exec.record.metadata.TupleMetadata} instead.
 */
public class BatchSchema implements Iterable<MaterializedField> {

  private final SelectionVectorMode selectionVectorMode;
  private final List<MaterializedField> fields;

  public BatchSchema(SelectionVectorMode selectionVector, List<MaterializedField> fields) {
    this.fields = fields;
    this.selectionVectorMode = selectionVector;
  }

  public static SchemaBuilder newBuilder() {
    return new SchemaBuilder();
  }

  public int getFieldCount() {
    return fields.size();
  }

  public MaterializedField getColumn(int index) {
    if (index < 0 || index >= fields.size()) {
      return null;
    }
    return fields.get(index);
  }

  @Override
  public Iterator<MaterializedField> iterator() {
    return fields.iterator();
  }

  public SelectionVectorMode getSelectionVectorMode() {
    return selectionVectorMode;
  }

  @Override
  public BatchSchema clone() {
    List<MaterializedField> newFields = Lists.newArrayList();
    newFields.addAll(fields);
    return new BatchSchema(selectionVectorMode, newFields);
  }

  @Override
  public String toString() {
    return "BatchSchema [fields=" + fields + ", selectionVector=" + selectionVectorMode + "]";
  }

  public enum SelectionVectorMode {
    NONE(-1, false), TWO_BYTE(2, true), FOUR_BYTE(4, true);

    public boolean hasSelectionVector;
    public final int size;
    SelectionVectorMode(int size, boolean hasSelectionVector) {
      this.size = size;
    }

    public static SelectionVectorMode[] DEFAULT = {NONE};
    public static SelectionVectorMode[] NONE_AND_TWO = {NONE, TWO_BYTE};
    public static SelectionVectorMode[] NONE_AND_FOUR = {NONE, FOUR_BYTE};
    public static SelectionVectorMode[] ALL = {NONE, TWO_BYTE, FOUR_BYTE};
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fields == null) ? 0 : fields.hashCode());
    result = prime * result + ((selectionVectorMode == null) ? 0 : selectionVectorMode.hashCode());
    return result;
  }

  /**
   * DRILL-5525: the semantics of this method are badly broken.
   * Caveat emptor.
   *
   * This check used for detecting actual schema change inside operator record batch will not work for
   * AbstractContainerVectors (like MapVector). In each record batch a reference to incoming batch schema is
   * stored (let say S:{a: int}) and then equals is called on that stored reference and current incoming batch schema.
   * Internally schema object has references to Materialized fields from vectors in container. If there is change in
   * incoming batch schema, then the upstream will create a new ValueVector in its output container with the new
   * detected type, which in turn will have new instance for Materialized Field. Then later a new BatchSchema object
   * is created for this new incoming batch (let say S":{a":varchar}). The operator calling equals will have reference
   * to old schema object (S) and hence first check will not be satisfied and then it will call equals on each of the
   * Materialized Field (a.equals(a")). Since new materialized field is created for newly created vector the equals
   * check on field will return false. And schema change will be detected in this case.
   * Now consider instead of int vector there is a MapVector such that initial schema was (let say S:{a:{b:int, c:int}}
   * and then later schema for Map field c changes, then in container Map vector will be found but later the children
   * vector for field c will be replaced. This new schema object will be created as (S":{a:{b:int, c":varchar}}). Now
   * when S.equals(S") is called it will eventually call a.equals(a) which will return true even though the schema of
   * children value vector c has changed. This is because no new vector is created for field (a) and hence it's object
   * reference to MaterializedField has not changed which will be reflected in both old and new schema instances.
   * Hence we should make use of {@link BatchSchema#isEquivalent(BatchSchema)} method instead since
   * {@link MaterializedField#isEquivalent(MaterializedField)} method is updated to remove the reference check.
   */
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
    BatchSchema other = (BatchSchema) obj;
    if (selectionVectorMode != other.selectionVectorMode) {
      return false;
    }
    if (fields == null) {
      return other.fields == null;
    }

    // Compare names.
    // (DRILL-5525: actually compares all fields.)
    if (!fields.equals(other.fields)) {
      return false;
    }

    // Compare types
    // (DRILL-5525: this code is redundant because any differences
    // will fail above.)
    for (int i = 0; i < fields.size(); i++) {
      MajorType t1 = fields.get(i).getType();
      MajorType t2 = other.fields.get(i).getType();
      if (t1 == null) {
        if (t2 != null) {
          return false;
        }
      } else {
        if (!majorTypeEqual(t1, t2)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Compare that two schemas are identical according to the rules defined
   * in {@link MaterializedField#isEquivalent(MaterializedField)}. In particular,
   * this method requires that the fields have a 1:1 ordered correspondence
   * in the two schemas.
   *
   * @param other another non-null batch schema
   * @return <tt>true</tt> if the two schemas are equivalent according to
   * the {@link MaterializedField#isEquivalent(MaterializedField)} rules,
   * false otherwise
   */
  public boolean isEquivalent(BatchSchema other) {
    if (this == other) {
      return true;
    }
    if (fields == null || other.fields == null) {
      return fields == other.fields;
    }
    if (fields.size() != other.fields.size()) {
      return false;
    }
    for (int i = 0; i < fields.size(); i++) {
      if (! fields.get(i).isEquivalent(other.fields.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * We treat fields with same set of Subtypes as equal, even if they are in a different order
   * @param t1
   * @param t2
   * @return
   */
  private boolean majorTypeEqual(MajorType t1, MajorType t2) {
    if (t1.equals(t2)) {
      return true;
    } else if (!t1.getMinorType().equals(t2.getMinorType())) {
      return false;
    } else if (!t1.getMode().equals(t2.getMode())) {
      return false;
    } else if (!Sets.newHashSet(t1.getSubTypeList()).equals(Sets.newHashSet(t2.getSubTypeList()))) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Merge two schemas to produce a new, merged schema. The caller is responsible
   * for ensuring that column names are unique. The order of the fields in the
   * new schema is the same as that of this schema, with the other schema's fields
   * appended in the order defined in the other schema.
   * <p>
   * Merging data with selection vectors is unlikely to be useful, or work well.
   * With a selection vector, the two record batches would have to be correlated
   * both in their selection vectors AND in the underlying vectors. Such a use case
   * is hard to imagine. So, for now, this method forbids merging schemas if either
   * of them carry a selection vector. If we discover a meaningful use case, we can
   * revisit the issue.
   * @param otherSchema the schema to merge with this one
   * @return the new, merged, schema
   */
  public BatchSchema merge(BatchSchema otherSchema) {
    if (selectionVectorMode != SelectionVectorMode.NONE ||
        otherSchema.selectionVectorMode != SelectionVectorMode.NONE) {
      throw new IllegalArgumentException("Cannot merge schemas with selection vectors");
    }
    List<MaterializedField> mergedFields =
        new ArrayList<>(fields.size() + otherSchema.fields.size());
    mergedFields.addAll(this.fields);
    mergedFields.addAll(otherSchema.fields);
    return new BatchSchema(selectionVectorMode, mergedFields);
  }

  /**
   * Format the schema into a multi-line format. Useful when debugging a query with
   * a very wide schema as the usual single-line format is far too hard to read.
   */
  public String format() {
    StringBuilder buf = new StringBuilder();
    buf.append("Batch Schema:\n");
    for (MaterializedField field : fields) {
      field.format(buf, 1);
    }
    return buf.toString();
  }
}
