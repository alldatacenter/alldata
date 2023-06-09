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
package org.apache.drill.exec.planner.physical;

import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitDef;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DrillDistributionTrait implements RelTrait {

  public static DrillDistributionTrait SINGLETON = new DrillDistributionTrait(DistributionType.SINGLETON);
  public static DrillDistributionTrait RANDOM_DISTRIBUTED = new DrillDistributionTrait(DistributionType.RANDOM_DISTRIBUTED);
  public static DrillDistributionTrait ANY = new DrillDistributionTrait(DistributionType.ANY);

  public static DrillDistributionTrait DEFAULT = ANY;

  private final DistributionType type;
  private final List<DistributionField> fields;
  private PartitionFunction partitionFunction;

  public DrillDistributionTrait(DistributionType type) {
    assert (type == DistributionType.SINGLETON || type == DistributionType.RANDOM_DISTRIBUTED || type == DistributionType.ANY
            || type == DistributionType.ROUND_ROBIN_DISTRIBUTED || type == DistributionType.BROADCAST_DISTRIBUTED);
    this.type = type;
    this.fields = Collections.emptyList();
  }

  public DrillDistributionTrait(DistributionType type, List<DistributionField> fields) {
    assert (type == DistributionType.HASH_DISTRIBUTED || type == DistributionType.RANGE_DISTRIBUTED);
    this.type = type;
    this.fields = fields;
  }

  public DrillDistributionTrait(DistributionType type, List<DistributionField> fields,
      PartitionFunction partitionFunction) {
    assert (type == DistributionType.HASH_DISTRIBUTED || type == DistributionType.RANGE_DISTRIBUTED);
    this.type = type;
    this.fields = fields;
    this.partitionFunction = partitionFunction;
  }

  @Override
  public void register(RelOptPlanner planner) {
  }

  @Override
  public boolean satisfies(RelTrait trait) {

    if (trait instanceof DrillDistributionTrait) {
      DistributionType requiredDist = ((DrillDistributionTrait) trait).getType();
      if (requiredDist == DistributionType.ANY) {
        return true;
      }

      if (this.type == DistributionType.HASH_DISTRIBUTED) {
        if (requiredDist == DistributionType.HASH_DISTRIBUTED) {
          // A subset of the required distribution columns can satisfy (subsume) the requirement
          // e.g: required distribution: {a, b, c}
          // Following can satisfy the requirements: {a}, {b}, {c}, {a, b}, {b, c}, {a, c} or {a, b, c}

          // New: Use equals for subsumes check of hash distribution. If we uses subsumes,
          // a join may end up with hash-distributions using different keys. This would
          // cause incorrect query result.
          return this.equals(trait);
        }
        else if (requiredDist == DistributionType.RANDOM_DISTRIBUTED) {
          return true; // hash distribution subsumes random distribution and ANY distribution
        }
      }

      if(this.type == DistributionType.RANGE_DISTRIBUTED) {
        if (requiredDist == DistributionType.RANDOM_DISTRIBUTED) {
          return true; // RANGE_DISTRIBUTED distribution subsumes random distribution and ANY distribution
        }
      }
    }

    return this.equals(trait);
  }

  @Override
  public RelTraitDef<DrillDistributionTrait> getTraitDef() {
    return DrillDistributionTraitDef.INSTANCE;
  }

  public DistributionType getType() {
    return this.type;
  }

  public List<DistributionField> getFields() {
    return fields;
  }

  public PartitionFunction getPartitionFunction() {
    return partitionFunction;
  }

  private boolean arePartitionFunctionsSame(PartitionFunction f1, PartitionFunction f2) {
    return Objects.equals(f1, f2);
  }

  @Override
  public int hashCode() {
    return  fields == null? type.hashCode(): type.hashCode() | fields.hashCode() << 4;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof DrillDistributionTrait) {
      DrillDistributionTrait that = (DrillDistributionTrait) obj;
      return this.type == that.type && this.fields.equals(that.fields) &&
          arePartitionFunctionsSame(this.partitionFunction, that.partitionFunction);
    }
    return false;
  }

  @Override
  public String toString() {
    return fields == null ? this.type.toString() : this.type.toString() + "(" + fields + ")";
  }

  public enum DistributionType {
    SINGLETON,
    HASH_DISTRIBUTED,
    RANGE_DISTRIBUTED,
    RANDOM_DISTRIBUTED,
    ROUND_ROBIN_DISTRIBUTED,
    BROADCAST_DISTRIBUTED,
    ANY
  }

  public static class DistributionField {
    /**
     * 0-based index of field being DISTRIBUTED.
     */
    private final int fieldId;

    public DistributionField (int fieldId) {
      this.fieldId = fieldId;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof DistributionField)) {
        return false;
      }
      DistributionField other = (DistributionField) obj;
      return this.fieldId == other.fieldId;
    }

    @Override
    public int hashCode() {
      return this.fieldId;
    }

    public int getFieldId() {
      return this.fieldId;
    }

    @Override
    public String toString() {
      return String.format("[$%s]", this.fieldId);
    }
  }

  /**
   * Stores distribution field index and field name to be used in exchange operators.
   * Field name is required for the case of dynamic schema discovering
   * when field is not present in rel data type at planning time.
   */
  public static class NamedDistributionField extends DistributionField {
    /**
     * Name of the field being DISTRIBUTED.
     */
    private final String fieldName;

    public NamedDistributionField(int fieldId, String fieldName) {
      super(fieldId);
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }

      NamedDistributionField that = (NamedDistributionField) o;

      return Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), fieldName);
    }

    @Override
    public String toString() {
      return String.format("%s(%s)", fieldName, getFieldId());
    }
  }
}
