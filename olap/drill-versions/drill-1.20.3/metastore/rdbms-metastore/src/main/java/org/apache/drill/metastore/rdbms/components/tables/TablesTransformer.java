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
package org.apache.drill.metastore.rdbms.components.tables;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.operate.RdbmsOperation;
import org.apache.drill.metastore.rdbms.transform.AbstractTransformer;
import org.apache.drill.metastore.rdbms.transform.MetadataMapper;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.jooq.Record;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transformer implementation for RDBMS Metastore tables component.
 */
public class TablesTransformer extends AbstractTransformer<TableMetadataUnit> {

  private static final TablesTransformer INSTANCE = new TablesTransformer();

  private final Map<MetadataType, MetadataMapper<TableMetadataUnit, ? extends Record>> MAPPERS = ImmutableMap.of(
    MetadataType.TABLE, TablesMetadataMapper.TableMapper.get(),
    MetadataType.SEGMENT, TablesMetadataMapper.SegmentMapper.get(),
    MetadataType.FILE, TablesMetadataMapper.FileMapper.get(),
    MetadataType.ROW_GROUP, TablesMetadataMapper.RowGroupMapper.get(),
    MetadataType.PARTITION, TablesMetadataMapper.PartitionMapper.get()
  );

  public static TablesTransformer get() {
    return INSTANCE;
  }

  @Override
  public Set<MetadataMapper<TableMetadataUnit, ? extends Record>> toMappers(Set<MetadataType> metadataTypes) {
    if (metadataTypes.contains(MetadataType.ALL)) {
      return Sets.newHashSet(MAPPERS.values());
    }

    return metadataTypes.stream()
      .map(this::toMapper)
      .collect(Collectors.toSet());
  }

  @Override
  public MetadataMapper<TableMetadataUnit, ? extends Record> toMapper(MetadataType metadataType) {
    MetadataMapper<TableMetadataUnit, ? extends Record> mapper = MAPPERS.get(metadataType);
    if (mapper == null) {
      throw new RdbmsMetastoreException("Metadata mapper is absent for type: " + metadataType);
    }
    return mapper;
  }

  @Override
  public List<RdbmsOperation.Overwrite> toOverwrite(List<TableMetadataUnit> units) {
    Map<String, List<TableMetadataUnit>> unitsByTables = units.stream()
      .collect(Collectors.groupingBy(TableMetadataUnit::metadataType));

    return unitsByTables.entrySet().stream()
      .map(entry -> toOverwrite(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList());
  }
}
