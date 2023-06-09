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
package org.apache.drill.exec.vector.complex;

import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.vector.ValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FieldIdUtil {
  private static final Logger logger = LoggerFactory.getLogger(FieldIdUtil.class);

  public static TypedFieldId getFieldIdIfMatchesUnion(UnionVector unionVector, TypedFieldId.Builder builder, boolean addToBreadCrumb, PathSegment seg) {
    if (seg != null) {
      if (seg.isNamed()) {
        ValueVector v = unionVector.getMap();
        if (v != null) {
          return getFieldIdIfMatches(v, builder, addToBreadCrumb, seg);
        } else {
          return null;
        }
      } else if (seg.isArray()) {
        ValueVector v = unionVector.getList();
        if (v != null) {
          return getFieldIdIfMatches(v, builder, addToBreadCrumb, seg);
        } else {
          return null;
        }
      }
    } else {
      if (addToBreadCrumb) {
        builder.intermediateType(unionVector.getField().getType());
      }
      return builder.finalType(unionVector.getField().getType()).build();
    }

    return null;
  }

  /**
   * Utility method to obtain {@link TypedFieldId}, providing metadata
   * for specified field given by value vector used in code generation.
   *
   * @param vector a value vector the metadata is obtained for
   * @param builder a builder instance gathering metadata
   * @param addToBreadCrumb flag to indicate whether to include intermediate type
   * @param seg path segment corresponding to the vector
   * @return type metadata for given vector
   */
  public static TypedFieldId getFieldIdIfMatches(ValueVector vector, TypedFieldId.Builder builder,
                                                 boolean addToBreadCrumb, PathSegment seg) {
    return getFieldIdIfMatches(vector, builder, addToBreadCrumb, seg, 0);
  }

  private static TypedFieldId getFieldIdIfMatches(ValueVector vector, TypedFieldId.Builder builder,
                                                  boolean addToBreadCrumb, PathSegment seg, int depth) {
    if (vector instanceof DictVector) {
      builder.setDict(depth);
    } else if (vector instanceof RepeatedMapVector && seg != null && seg.isArray() && !seg.isLastPath()) {
      if (addToBreadCrumb) {
        addToBreadCrumb = false;
        builder.remainder(seg);
      }
      // skip the first array segment as there is no corresponding child vector.
      seg = seg.getChild();
      depth++;

      // multi-level numbered access to a repeated map is not possible so return if the next part is also an array
      // segment.
      if (seg.isArray()) {
        return null;
      }
    }

    if (seg == null) {
      if (addToBreadCrumb) {
        builder.intermediateType(vector.getField().getType());
      }
      return builder.finalType(vector.getField().getType()).build();
    }

    if (seg.isArray()) {
      if (seg.isLastPath()) {
        MajorType type;
        if (vector instanceof AbstractContainerVector) {
          type = ((AbstractContainerVector) vector).getLastPathType();
        } else if (vector instanceof RepeatedValueVector) {
          type = ((RepeatedValueVector) vector).getDataVector().getField().getType();
          builder.listVector(vector.getField().getType().getMinorType() == MinorType.LIST);
        } else {
          throw new UnsupportedOperationException("FieldIdUtil does not support vector of type " + vector.getField().getType());
        }
        builder //
                .withIndex() //
                .finalType(type);

        // remainder starts with the 1st array segment in SchemaPath.
        // only set remainder when it's the only array segment.
        if (addToBreadCrumb) {
          addToBreadCrumb = false;
          builder.remainder(seg);
        }
        return builder.build();
      } else {
        if (addToBreadCrumb) {
          addToBreadCrumb = false;
          builder.remainder(seg);
        }
      }
    } else {
      if (vector instanceof ListVector) {
        return null;
      }
    }

    ValueVector v;
    MajorType finalType = null;
    if (vector instanceof DictVector) {
      v = ((DictVector) vector).getValues();
      if (addToBreadCrumb) {
        builder.remainder(seg);
        builder.intermediateType(vector.getField().getType());
        addToBreadCrumb = false;
        // reset bit set and depth as this Dict vector will be the first one in the schema
        builder.resetDictBitSet();
        depth = 0;
        builder.setDict(depth);
      }
      finalType = ((DictVector) vector).getLastPathType();
    } else if (vector instanceof AbstractContainerVector) {
      String fieldName = null;
      if (seg.isNamed()) {
        fieldName = seg.getNameSegment().getPath();
      }
      VectorWithOrdinal vord = ((AbstractContainerVector) vector).getChildVectorWithOrdinal(fieldName);
      if (vord == null) {
        return null;
      }
      v = vord.vector;
      if (addToBreadCrumb) {
        builder.intermediateType(v.getField().getType());
        builder.addId(vord.ordinal);
      }
    } else if (vector instanceof ListVector || vector instanceof RepeatedDictVector) {
      v = ((RepeatedValueVector) vector).getDataVector();
    } else {
      throw new UnsupportedOperationException("FieldIdUtil does not support vector of type " + vector.getField().getType());
    }

    if (v instanceof AbstractContainerVector || v instanceof ListVector || v instanceof RepeatedDictVector) {
      return getFieldIdIfMatches(v, builder, addToBreadCrumb, seg.getChild(), depth + 1);
    } else if (v instanceof UnionVector) {
      return getFieldIdIfMatchesUnion((UnionVector) v, builder, addToBreadCrumb, seg.getChild());
    } else {
      if (seg.isNamed()) {
        if(addToBreadCrumb) {
          builder.intermediateType(v.getField().getType());
        }
        builder.finalType(finalType != null ? finalType : v.getField().getType());
      } else {
        builder.finalType(v.getField().getType().toBuilder().setMode(DataMode.OPTIONAL).build());
      }

      if (seg.isLastPath()) {
        return builder.build();
      } else {
        PathSegment child = seg.getChild();
        if (child.isLastPath() && child.isArray()) {
          if (addToBreadCrumb) {
            builder.remainder(child);
          }
          builder.withIndex();
          builder.finalType(v.getField().getType().toBuilder().setMode(DataMode.OPTIONAL).build());
          return builder.build();
        } else {
          logger.warn("You tried to request a complex type inside a scalar object or path or type is wrong.");
          return null;
        }
      }
    }
  }

  public static TypedFieldId getFieldId(ValueVector vector, int id, SchemaPath expectedPath, boolean hyper) {
    if (!expectedPath.getRootSegment().getPath().equalsIgnoreCase(vector.getField().getName())) {
      return null;
    }
    PathSegment seg = expectedPath.getRootSegment();

    TypedFieldId.Builder builder = TypedFieldId.newBuilder().hyper(hyper);
    if (vector instanceof UnionVector) {
      builder.addId(id).remainder(expectedPath.getRootSegment().getChild());
      List<MinorType> minorTypes = ((UnionVector) vector).getSubTypes();
      MajorType.Builder majorTypeBuilder = MajorType.newBuilder().setMinorType(MinorType.UNION);
      for (MinorType type : minorTypes) {
        majorTypeBuilder.addSubType(type);
      }
      MajorType majorType = majorTypeBuilder.build();
      builder.intermediateType(majorType);
      if (seg.isLastPath()) {
        builder.finalType(majorType);
        return builder.build();
      } else {
        return getFieldIdIfMatchesUnion((UnionVector) vector, builder, false, seg.getChild());
      }
    } else if (vector instanceof ListVector) {
      builder.intermediateType(vector.getField().getType());
      builder.addId(id);
      return getFieldIdIfMatches(vector, builder, true, expectedPath.getRootSegment().getChild());
    } else if (vector instanceof DictVector) {
      MajorType vectorType = vector.getField().getType();
      builder.intermediateType(vectorType);
      builder.addId(id);
      if (seg.isLastPath()) {
        builder.finalType(vectorType);
        return builder.build();
      } else {
        PathSegment child = seg.getChild();
        builder.remainder(child);
        return getFieldIdIfMatches(vector, builder, false, expectedPath.getRootSegment().getChild());
      }
    } else if (vector instanceof AbstractContainerVector) {
      // we're looking for a multi path.
      builder.intermediateType(vector.getField().getType());
      builder.addId(id);
      return getFieldIdIfMatches(vector, builder, true, expectedPath.getRootSegment().getChild());
    } else if (vector instanceof RepeatedDictVector) {
      MajorType vectorType = vector.getField().getType();
      builder.intermediateType(vectorType);
      builder.addId(id);
      if (seg.isLastPath()) {
        builder.finalType(vectorType);
        return builder.build();
      } else {
        PathSegment child = seg.getChild();
        if (!child.isArray()) {
          return null;
        } else {
          builder.remainder(child);
          builder.withIndex();
          if (child.isLastPath()) {
            return builder.finalType(DictVector.TYPE).build();
          } else {
            return getFieldIdIfMatches(vector, builder, true, expectedPath.getRootSegment().getChild());
          }
        }
      }
    } else {
      builder.intermediateType(vector.getField().getType());
      builder.addId(id);
      builder.finalType(vector.getField().getType());
      if (seg.isLastPath()) {
        return builder.build();
      } else {
        PathSegment child = seg.getChild();
        if (child.isArray() && child.isLastPath()) {
          builder.remainder(child);
          builder.withIndex();
          builder.finalType(vector.getField().getType().toBuilder().setMode(DataMode.OPTIONAL).build());
          return builder.build();
        } else {
          return null;
        }
      }
    }
  }
}
