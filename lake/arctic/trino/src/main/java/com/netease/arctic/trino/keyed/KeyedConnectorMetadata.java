
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino.keyed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.trino.util.ObjectSerializerUtil;
import io.trino.plugin.hive.HiveApplyProjectionUtil;
import io.trino.plugin.iceberg.ColumnIdentity;
import io.trino.plugin.iceberg.IcebergColumnHandle;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.plugin.iceberg.TableType;
import io.trino.spi.connector.Assignment;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.ConstraintApplicationResult;
import io.trino.spi.connector.ProjectionApplicationResult;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;
import io.trino.spi.connector.TableNotFoundException;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.expression.Variable;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.type.TypeManager;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SchemaParser;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.exceptions.NotFoundException;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.trino.plugin.hive.HiveApplyProjectionUtil.extractSupportedProjectedColumns;
import static io.trino.plugin.hive.HiveApplyProjectionUtil.replaceWithNewVariables;
import static io.trino.plugin.hive.util.HiveUtil.isHiveSystemSchema;
import static io.trino.plugin.hive.util.HiveUtil.isStructuralType;
import static io.trino.plugin.iceberg.IcebergUtil.getColumns;
import static io.trino.plugin.iceberg.TypeConverter.toTrinoType;
import static io.trino.spi.connector.RetryMode.NO_RETRIES;

/**
 * Metadata for Keyed Table
 */
public class KeyedConnectorMetadata implements ConnectorMetadata {

  private static final Logger log = LoggerFactory.getLogger(KeyedConnectorMetadata.class);

  private ArcticCatalog arcticCatalog;

  private TypeManager typeManager;

  private ConcurrentHashMap<SchemaTableName, ArcticTable> concurrentHashMap = new ConcurrentHashMap<>();

  public KeyedConnectorMetadata(ArcticCatalog arcticCatalog, TypeManager typeManager) {
    this.arcticCatalog = arcticCatalog;
    this.typeManager = typeManager;
  }

  @Override
  public List<String> listSchemaNames(ConnectorSession session) {
    return arcticCatalog.listDatabases().stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
  }

  @Override
  public ConnectorTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName) {

    KeyedTable arcticTable = getArcticTable(tableName).asKeyedTable();
    if (arcticTable == null) {
      return null;
    }
    TableIdentifier tableIdentifier = arcticTable.id();
    Map<String, String> tableProperties = arcticTable.properties();
    String nameMappingJson = tableProperties.get(TableProperties.DEFAULT_NAME_MAPPING);
    IcebergTableHandle icebergTableHandle = new IcebergTableHandle(
        tableName.getSchemaName(),
        tableIdentifier.getTableName(),
        TableType.DATA,
        Optional.empty(),
        SchemaParser.toJson(arcticTable.schema()),
        2,
        TupleDomain.all(),
        TupleDomain.all(),
        ImmutableSet.of(),
        Optional.ofNullable(nameMappingJson),
        arcticTable.location(),
        tableProperties,
        NO_RETRIES);

    return new KeyedTableHandle(icebergTableHandle, ObjectSerializerUtil.write(arcticTable.primaryKeySpec()));
  }

  @Override
  public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table) {
    KeyedTableHandle keyedTableHandle = (KeyedTableHandle) table;
    IcebergTableHandle icebergTableHandle = keyedTableHandle.getIcebergTableHandle();
    SchemaTableName schemaTableName =
        new SchemaTableName(icebergTableHandle.getSchemaName(), icebergTableHandle.getTableName());
    ArcticTable arcticTable = getArcticTable(new SchemaTableName(
        icebergTableHandle.getSchemaName(),
        icebergTableHandle.getTableName()));
    if (arcticTable == null) {
      throw new TableNotFoundException(schemaTableName);
    }
    List<ColumnMetadata> columnMetadata = getColumnMetadata(arcticTable);
    return new ConnectorTableMetadata(schemaTableName, columnMetadata);
  }

  @Override
  public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle) {
    KeyedTableHandle keyedTableHandle = (KeyedTableHandle) tableHandle;
    IcebergTableHandle icebergTableHandle = keyedTableHandle.getIcebergTableHandle();
    SchemaTableName schemaTableName =
        new SchemaTableName(icebergTableHandle.getSchemaName(), icebergTableHandle.getTableName());
    ArcticTable arcticTable = getArcticTable(new SchemaTableName(
        icebergTableHandle.getSchemaName(),
        icebergTableHandle.getTableName()));
    if (arcticTable == null) {
      throw new TableNotFoundException(schemaTableName);
    }

    ImmutableMap.Builder<String, ColumnHandle> columnHandles = ImmutableMap.builder();
    for (IcebergColumnHandle columnHandle : getColumns(arcticTable.schema(), typeManager)) {
      columnHandles.put(columnHandle.getName(), columnHandle);
    }
    // columnHandles.put(FILE_PATH.getColumnName(), pathColumnHandle());
    return columnHandles.buildOrThrow();
  }

  @Override
  public ColumnMetadata getColumnMetadata(
      ConnectorSession session,
      ConnectorTableHandle tableHandle,
      ColumnHandle columnHandle) {
    IcebergColumnHandle column = (IcebergColumnHandle) columnHandle;
    return ColumnMetadata.builder()
        .setName(column.getName())
        .setType(column.getType())
        .build();
  }

  private List<ColumnMetadata> getColumnMetadata(ArcticTable arcticTable) {
    ImmutableList.Builder<ColumnMetadata> columnsMetadata = ImmutableList.builder();
    Schema schema = arcticTable.schema();
    List<Types.NestedField> schemaWithPartition = schema.columns();
    for (Types.NestedField column : schemaWithPartition) {
      columnsMetadata.add(
          ColumnMetadata.builder()
              .setName(column.name())
              .setComment(Optional.ofNullable(column.doc()))
              .setType(toTrinoType(column.type(), typeManager))
              .setNullable(column.isOptional())
              .setExtraInfo(Optional.of(column.fieldId() + ""))
              .build()
      );
    }
    return columnsMetadata.build();
  }

  @Override
  public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(
      ConnectorSession session,
      SchemaTablePrefix prefix) {
    List<SchemaTableName> schemaTableNames = !prefix.getTable().isPresent() ?
        listTables(session, prefix.getSchema()) : Lists.newArrayList(prefix.toSchemaTableName());
    ImmutableMap.Builder<SchemaTableName, List<ColumnMetadata>> columns = ImmutableMap.builder();
    for (SchemaTableName schemaTableName : schemaTableNames) {
      try {
        ArcticTable arcticTable = getArcticTable(schemaTableName);
        List<ColumnMetadata> columnMetadata = getColumnMetadata(arcticTable);
        columns.put(schemaTableName, columnMetadata);
      } catch (TableNotFoundException | NotFoundException e) {
        return Collections.EMPTY_MAP;
      } catch (Exception e) {
        return Collections.EMPTY_MAP;
      }
    }
    return columns.build();
  }

  @Override
  public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName) {
    return listNamespaces(session, schemaName)
        .stream()
        .flatMap(s -> arcticCatalog.listTables(s).stream())
        .map(s -> new SchemaTableName(s.getDatabase(), s.getTableName()))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(
      ConnectorSession session,
      ConnectorTableHandle handle,
      Constraint constraint) {
    KeyedTableHandle table = (KeyedTableHandle) handle;
    IcebergTableHandle icebergTableHandle = table.getIcebergTableHandle();
    ArcticTable arcticTable = getArcticTable(new SchemaTableName(
        icebergTableHandle.getSchemaName(),
        icebergTableHandle.getTableName()));

    Set<Integer> partitionSourceIds = identityPartitionColumnsInAllSpecs(arcticTable);
    BiPredicate<IcebergColumnHandle, Domain> isIdentityPartition =
        (column, domain) -> partitionSourceIds.contains(column.getId());

    TupleDomain<IcebergColumnHandle> newEnforcedConstraint = constraint.getSummary()
        .transformKeys(IcebergColumnHandle.class::cast)
        .filter(isIdentityPartition)
        .intersect(icebergTableHandle.getEnforcedPredicate());

    TupleDomain<IcebergColumnHandle> remainingConstraint = constraint.getSummary()
        .transformKeys(IcebergColumnHandle.class::cast)
        .filter(isIdentityPartition.negate());

    TupleDomain<IcebergColumnHandle> newUnenforcedConstraint = remainingConstraint
        // Only applies to the unenforced constraint because structural types cannot be partition keys
        .filter((columnHandle, predicate) -> !isStructuralType(columnHandle.getType()))
        .intersect(icebergTableHandle.getUnenforcedPredicate());

    if (newEnforcedConstraint.equals(icebergTableHandle.getEnforcedPredicate()) &&
        newUnenforcedConstraint.equals(icebergTableHandle.getUnenforcedPredicate())) {
      return Optional.empty();
    }

    IcebergTableHandle newIcebergTableHandle = new IcebergTableHandle(
        icebergTableHandle.getSchemaName(),
        icebergTableHandle.getTableName(),
        icebergTableHandle.getTableType(),
        icebergTableHandle.getSnapshotId(),
        icebergTableHandle.getTableSchemaJson(),
        2,
        newUnenforcedConstraint,
        newEnforcedConstraint,
        icebergTableHandle.getProjectedColumns(),
        icebergTableHandle.getNameMappingJson(),
        icebergTableHandle.getTableLocation(),
        icebergTableHandle.getStorageProperties(),
        icebergTableHandle.getRetryMode(),
        icebergTableHandle.isRecordScannedFiles(),
        icebergTableHandle.getMaxScannedFileSize()
    );
    return Optional.of(new ConstraintApplicationResult<>(
        new KeyedTableHandle(newIcebergTableHandle, table.getPrimaryKeySpecBytes()),
        remainingConstraint.transformKeys(ColumnHandle.class::cast),
        false));
  }

  @Override
  public Optional<ProjectionApplicationResult<ConnectorTableHandle>> applyProjection(
      ConnectorSession session,
      ConnectorTableHandle handle,
      List<ConnectorExpression> projections,
      Map<String, ColumnHandle> assignments) {
    // Create projected column representations for supported sub expressions. Simple column references and chain of
    // dereferences on a variable are supported right now.
    Set<ConnectorExpression> projectedExpressions = projections.stream()
        .flatMap(expression -> extractSupportedProjectedColumns(expression).stream())
        .collect(toImmutableSet());

    Map<ConnectorExpression, HiveApplyProjectionUtil.ProjectedColumnRepresentation> columnProjections =
        projectedExpressions.stream()
            .collect(toImmutableMap(Function.identity(), HiveApplyProjectionUtil::createProjectedColumnRepresentation));

    KeyedTableHandle keyedTableHandle = (KeyedTableHandle) handle;
    IcebergTableHandle icebergTableHandle = keyedTableHandle.getIcebergTableHandle();

    // all references are simple variables
    if (columnProjections.values()
        .stream()
        .allMatch(HiveApplyProjectionUtil.ProjectedColumnRepresentation::isVariable)) {
      Set<IcebergColumnHandle> projectedColumns = assignments.values().stream()
          .map(IcebergColumnHandle.class::cast)
          .collect(toImmutableSet());
      if (icebergTableHandle.getProjectedColumns().equals(projectedColumns)) {
        return Optional.empty();
      }
      List<Assignment> assignmentsList = assignments.entrySet().stream()
          .map(assignment -> new Assignment(
              assignment.getKey(),
              assignment.getValue(),
              ((IcebergColumnHandle) assignment.getValue()).getType()))
          .collect(toImmutableList());

      return Optional.of(new ProjectionApplicationResult<>(
          keyedTableHandle.withProjectedColumns(projectedColumns),
          projections,
          assignmentsList,
          false));
    }

    Map<String, Assignment> newAssignments = new HashMap<>();
    ImmutableMap.Builder<ConnectorExpression, Variable> newVariablesBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<IcebergColumnHandle> projectedColumnsBuilder = ImmutableSet.builder();

    for (Map.Entry<ConnectorExpression, HiveApplyProjectionUtil.ProjectedColumnRepresentation> entry :
        columnProjections.entrySet()) {
      ConnectorExpression expression = entry.getKey();
      HiveApplyProjectionUtil.ProjectedColumnRepresentation projectedColumn = entry.getValue();

      IcebergColumnHandle baseColumnHandle =
          (IcebergColumnHandle) assignments.get(projectedColumn.getVariable().getName());
      IcebergColumnHandle projectedColumnHandle =
          createProjectedColumnHandle(baseColumnHandle, projectedColumn.getDereferenceIndices(), expression.getType());
      String projectedColumnName = projectedColumnHandle.getQualifiedName();

      Variable projectedColumnVariable = new Variable(projectedColumnName, expression.getType());
      Assignment newAssignment = new Assignment(projectedColumnName, projectedColumnHandle, expression.getType());
      newAssignments.putIfAbsent(projectedColumnName, newAssignment);

      newVariablesBuilder.put(expression, projectedColumnVariable);
      projectedColumnsBuilder.add(projectedColumnHandle);
    }

    // Modify projections to refer to new variables
    Map<ConnectorExpression, Variable> newVariables = newVariablesBuilder.buildOrThrow();
    List<ConnectorExpression> newProjections = projections.stream()
        .map(expression -> replaceWithNewVariables(expression, newVariables))
        .collect(toImmutableList());

    List<Assignment> outputAssignments = newAssignments.values().stream().collect(toImmutableList());
    return Optional.of(new ProjectionApplicationResult<>(
        keyedTableHandle.withProjectedColumns(projectedColumnsBuilder.build()),
        newProjections,
        outputAssignments,
        false));
  }

  private static Set<Integer> identityPartitionColumnsInAllSpecs(ArcticTable table) {
    // Extract identity partition column source ids common to ALL specs
    return table.spec().partitionType().fields()
        .stream().map(s -> s.fieldId()).collect(Collectors.toUnmodifiableSet());
  }

  private static IcebergColumnHandle createProjectedColumnHandle(
      IcebergColumnHandle column, List<Integer> indices,
      io.trino.spi.type.Type projectedColumnType) {
    if (indices.isEmpty()) {
      return column;
    }
    ImmutableList.Builder<Integer> fullPath = ImmutableList.builder();
    fullPath.addAll(column.getPath());

    ColumnIdentity projectedColumnIdentity = column.getColumnIdentity();
    for (int index : indices) {
      // Position based lookup, not FieldId based
      projectedColumnIdentity = projectedColumnIdentity.getChildren().get(index);
      fullPath.add(projectedColumnIdentity.getId());
    }

    return new IcebergColumnHandle(
        column.getBaseColumnIdentity(),
        column.getBaseType(),
        fullPath.build(),
        projectedColumnType,
        Optional.empty());
  }

  public ArcticTable getArcticTable(SchemaTableName schemaTableName) {
    concurrentHashMap.computeIfAbsent(
        schemaTableName,
        ignore -> arcticCatalog.loadTable(TableIdentifier.of(arcticCatalog.name(),
            schemaTableName.getSchemaName(), schemaTableName.getTableName()))
    );
    return concurrentHashMap.get(schemaTableName);
  }

  private List<String> listNamespaces(ConnectorSession session, Optional<String> namespace) {
    if (namespace.isPresent()) {
      if (isHiveSystemSchema(namespace.get())) {
        return ImmutableList.of();
      }
      return ImmutableList.of(namespace.get());
    }
    return listSchemaNames(session);
  }
}
