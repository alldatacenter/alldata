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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.UnionVector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Utility class for dealing with changing schemas
 */
public class SchemaUtil {

  /**
   * Returns the merger of schemas. The merged schema will include the union all columns. If there is a type conflict
   * between columns with the same schemapath but different types, the merged schema will contain a Union type.
   * @param schemas
   * @return
   */
  public static BatchSchema mergeSchemas(BatchSchema... schemas) {
    Map<SchemaPath,Set<MinorType>> typeSetMap = Maps.newLinkedHashMap();

    for (BatchSchema s : schemas) {
      for (MaterializedField field : s) {
        SchemaPath path = SchemaPath.getSimplePath(field.getName());
        Set<MinorType> currentTypes = typeSetMap.get(path);
        if (currentTypes == null) {
          currentTypes = Sets.newHashSet();
          typeSetMap.put(path, currentTypes);
        }
        MinorType newType = field.getType().getMinorType();
        if (newType == MinorType.MAP || newType == MinorType.LIST) {
          throw new RuntimeException("Schema change not currently supported for schemas with complex types");
        }
        if (newType == MinorType.UNION) {
          currentTypes.addAll(field.getType().getSubTypeList());
        } else {
          currentTypes.add(newType);
        }
      }
    }

    List<MaterializedField> fields = Lists.newArrayList();

    for (SchemaPath path : typeSetMap.keySet()) {
      Set<MinorType> types = typeSetMap.get(path);
      if (types.size() > 1) {
        MajorType.Builder builder = MajorType.newBuilder().setMinorType(MinorType.UNION).setMode(DataMode.OPTIONAL);
        for (MinorType t : types) {
          builder.addSubType(t);
        }
        MaterializedField field = MaterializedField.create(path.getLastSegment().getNameSegment().getPath(), builder.build());
        fields.add(field);
      } else {
        MaterializedField field = MaterializedField.create(path.getLastSegment().getNameSegment().getPath(),
                                                            Types.optional(types.iterator().next()));
        fields.add(field);
      }
    }

    SchemaBuilder schemaBuilder = new SchemaBuilder();
    BatchSchema s = schemaBuilder.addFields(fields).setSelectionVectorMode(schemas[0].getSelectionVectorMode()).build();
    return s;
  }

  private static  ValueVector coerceVector(ValueVector v, VectorContainer c, MaterializedField field,
                                           int recordCount, BufferAllocator allocator) {
    if (v != null) {
      int valueCount = v.getAccessor().getValueCount();
      TransferPair tp = v.getTransferPair(allocator);
      tp.transfer();
      if (v.getField().getType().getMinorType().equals(field.getType().getMinorType())) {
        if (field.getType().getMinorType() == MinorType.UNION) {
          UnionVector u = (UnionVector) tp.getTo();
          for (MinorType t : field.getType().getSubTypeList()) {
            u.addSubType(t);
          }
        }
        return tp.getTo();
      } else {
        ValueVector newVector = TypeHelper.getNewVector(field, allocator);
        Preconditions.checkState(field.getType().getMinorType() == MinorType.UNION, "Can only convert vector to Union vector");
        UnionVector u = (UnionVector) newVector;
        u.setFirstType(tp.getTo(), valueCount);
        return u;
      }
    } else {
      v = TypeHelper.getNewVector(field, allocator);
      v.allocateNew();
      v.getMutator().setValueCount(recordCount);
      return v;
    }
  }

  /**
   * Creates a copy a record batch, converting any fields as necessary to coerce it into the provided schema
   * @param in
   * @param toSchema
   * @param context
   * @return
   */
  public static VectorContainer coerceContainer(VectorAccessible in, BatchSchema toSchema, OperatorContext context) {
    return coerceContainer(in, toSchema, context.getAllocator());
  }

  public static VectorContainer coerceContainer(VectorAccessible in, BatchSchema toSchema, BufferAllocator allocator) {
    int recordCount = in.getRecordCount();
    boolean isHyper = false;
    Map<String, Object> vectorMap = Maps.newHashMap();
    for (VectorWrapper<?> w : in) {
      if (w.isHyper()) {
        isHyper = true;
        final ValueVector[] vvs = w.getValueVectors();
        vectorMap.put(vvs[0].getField().getName(), vvs);
      } else {
        assert !isHyper;
        final ValueVector v = w.getValueVector();
        vectorMap.put(v.getField().getName(), v);
      }
    }

    VectorContainer c = new VectorContainer(allocator);

    for (MaterializedField field : toSchema) {
      if (isHyper) {
        final ValueVector[] vvs = (ValueVector[]) vectorMap.remove(field.getName());
        final ValueVector[] vvsOut;
        if (vvs == null) {
          vvsOut = new ValueVector[1];
          vvsOut[0] = coerceVector(null, c, field, recordCount, allocator);
        } else {
          vvsOut = new ValueVector[vvs.length];
          for (int i = 0; i < vvs.length; ++i) {
            vvsOut[i] = coerceVector(vvs[i], c, field, recordCount, allocator);
          }
        }
        c.add(vvsOut);
      } else {
        final ValueVector v = (ValueVector) vectorMap.remove(field.getName());
        c.add(coerceVector(v, c, field, recordCount, allocator));
      }
    }
    c.buildSchema(in.getSchema().getSelectionVectorMode());
    c.setRecordCount(recordCount);
    Preconditions.checkState(vectorMap.size() == 0, "Leftover vector from incoming batch");
    return c;
  }

  public static TupleMetadata fromBatchSchema(BatchSchema batchSchema) {
    TupleSchema tuple = new TupleSchema();
    for (MaterializedField field : batchSchema) {
      tuple.add(MetadataUtils.fromView(field));
    }
    return tuple;
  }

  /**
   * Returns list of {@link SchemaPath} for fields taken from specified schema.
   *
   * @param schema the source of fields to return
   * @return list of {@link SchemaPath}
   */
  public static List<SchemaPath> getSchemaPaths(TupleMetadata schema) {
    return SchemaUtil.getColumnPaths(schema, null).stream()
        .map(stringList -> SchemaPath.getCompoundPath(stringList.toArray(new String[0])))
        .collect(Collectors.toList());
  }

  private static List<List<String>> getColumnPaths(TupleMetadata schema, List<String> parentNames) {
    List<List<String>> result = new ArrayList<>();
    for (ColumnMetadata columnMetadata : schema) {
      if (columnMetadata.isMap()) {
        List<String> currentNames = parentNames == null
            ? new ArrayList<>()
            : new ArrayList<>(parentNames);
        currentNames.add(columnMetadata.name());
        result.addAll(getColumnPaths(columnMetadata.tupleSchema(), currentNames));
      } else {
        if (parentNames != null) {
          List<String> combinedList = new ArrayList<>(parentNames);
          combinedList.add(columnMetadata.name());
          result.add(combinedList);
        } else {
          result.add(Collections.singletonList(columnMetadata.name()));
        }
      }
    }
    return result;
  }
}
