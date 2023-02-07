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
package org.apache.drill.exec.planner.sql.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScreenRel;
import org.apache.drill.exec.planner.logical.DrillWriterRel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectAllowDupPrel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.planner.physical.visitor.BasePrelVisitor;
import org.apache.drill.exec.planner.sql.DrillSqlOperator;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlCreateTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class CreateTableHandler extends DefaultSqlHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CreateTableHandler.class);

  public CreateTableHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    super(config, textPlan);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ValidationException, RelConversionException, IOException, ForemanSetupException {
    final SqlCreateTable sqlCreateTable = unwrap(sqlNode, SqlCreateTable.class);
    final String originalTableName = DrillStringUtils.removeLeadingSlash(sqlCreateTable.getName());

    final ConvertedRelNode convertedRelNode = validateAndConvert(sqlCreateTable.getQuery());
    final RelDataType validatedRowType = convertedRelNode.getValidatedRowType();
    final RelNode queryRelNode = convertedRelNode.getConvertedNode();

    final RelNode newTblRelNode =
        SqlHandlerUtil.resolveNewTableRel(false, sqlCreateTable.getFieldNames(), validatedRowType, queryRelNode);

    final DrillConfig drillConfig = context.getConfig();
    final AbstractSchema drillSchema = resolveSchema(sqlCreateTable, config.getConverter().getDefaultSchema(), drillConfig);
    final boolean checkTableNonExistence = sqlCreateTable.checkTableNonExistence();
    final String schemaPath = drillSchema.getFullSchemaName();

    // Check table creation possibility
    if(!checkTableCreationPossibility(drillSchema, originalTableName, drillConfig, context.getSession(), schemaPath, checkTableNonExistence)) {
      return DirectPlan.createDirectPlan(context, false,
        String.format("A table or view with given name [%s] already exists in schema [%s]", originalTableName, schemaPath));
    }

    final RelNode newTblRelNodeWithPCol = SqlHandlerUtil.qualifyPartitionCol(newTblRelNode,
        sqlCreateTable.getPartitionColumns());

    log("Calcite", newTblRelNodeWithPCol, logger, null);
    // Convert the query to Drill Logical plan and insert a writer operator on top.
    StorageStrategy storageStrategy = sqlCreateTable.isTemporary() ?
        StorageStrategy.TEMPORARY :
        new StorageStrategy(context.getOption(ExecConstants.PERSISTENT_TABLE_UMASK).string_val, false);

    // If we are creating temporary table, initial table name will be replaced with generated table name.
    // Generated table name is unique, UUID.randomUUID() is used for its generation.
    // Original table name is stored in temporary tables cache, so it can be substituted to generated one during querying.
    String newTableName = sqlCreateTable.isTemporary() ?
        context.getSession().registerTemporaryTable(drillSchema, originalTableName, drillConfig) : originalTableName;

    DrillRel drel = convertToDrel(newTblRelNodeWithPCol, drillSchema, newTableName,
        sqlCreateTable.getPartitionColumns(), newTblRelNode.getRowType(), storageStrategy);
    Prel prel = convertToPrel(drel, newTblRelNode.getRowType(), sqlCreateTable.getPartitionColumns());
    logAndSetTextPlan("Drill Physical", prel, logger);
    PhysicalOperator pop = convertToPop(prel);
    PhysicalPlan plan = convertToPlan(pop);
    log("Drill Plan", plan, logger);

    String message = String.format("Creating %s table [%s].",
        sqlCreateTable.isTemporary()  ? "temporary" : "persistent", originalTableName);
    logger.info(message);
    return plan;
  }

  private DrillRel convertToDrel(RelNode relNode,
                                 AbstractSchema schema,
                                 String tableName,
                                 List<String> partitionColumns,
                                 RelDataType queryRowType,
                                 StorageStrategy storageStrategy)
      throws RelConversionException, SqlUnsupportedException {
    final DrillRel convertedRelNode = convertToRawDrel(relNode);

    // Put a non-trivial topProject to ensure the final output field name is preserved, when necessary.
    // Only insert project when the field count from the child is same as that of the queryRowType.
    final DrillRel topPreservedNameProj = queryRowType.getFieldCount() == convertedRelNode.getRowType().getFieldCount() ?
        addRenamedProject(convertedRelNode, queryRowType) : convertedRelNode;

    final RelTraitSet traits = convertedRelNode.getCluster().traitSet().plus(DrillRel.DRILL_LOGICAL);
    final DrillWriterRel writerRel = new DrillWriterRel(convertedRelNode.getCluster(),
        traits, topPreservedNameProj, schema.createNewTable(tableName, partitionColumns, storageStrategy));
    return new DrillScreenRel(writerRel.getCluster(), writerRel.getTraitSet(), writerRel);
  }

  private Prel convertToPrel(RelNode drel, RelDataType inputRowType, List<String> partitionColumns)
      throws RelConversionException, SqlUnsupportedException {
    Prel prel = convertToPrel(drel, inputRowType);

    prel = prel.accept(new ProjectForWriterVisitor(inputRowType, partitionColumns), null);

    return prel;
  }

  /**
   * A PrelVisitor which will insert a project under Writer.
   *
   * For CTAS : create table t1 partition by (con_A) select * from T1;
   *   A Project with Item expr will be inserted, in addition to *.  We need insert another Project to remove
   *   this additional expression.
   *
   * In addition, to make execution's implementation easier,  a special field is added to Project :
   *     PARTITION_COLUMN_IDENTIFIER = newPartitionValue(Partition_colA)
   *                                    || newPartitionValue(Partition_colB)
   *                                    || ...
   *                                    || newPartitionValue(Partition_colN).
   */
  private class ProjectForWriterVisitor extends BasePrelVisitor<Prel, Void, RuntimeException> {

    private final RelDataType queryRowType;
    private final List<String> partitionColumns;

    ProjectForWriterVisitor(RelDataType queryRowType, List<String> partitionColumns) {
      this.queryRowType = queryRowType;
      this.partitionColumns = partitionColumns;
    }

    @Override
    public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
      List<RelNode> children = Lists.newArrayList();
      for(Prel child : prel){
        child = child.accept(this, null);
        children.add(child);
      }

      return (Prel) prel.copy(prel.getTraitSet(), children);

    }

    @Override
    public Prel visitWriter(WriterPrel prel, Void value) throws RuntimeException {

      final Prel child = ((Prel) prel.getInput()).accept(this, null);

      final RelDataType childRowType = child.getRowType();

      final RelOptCluster cluster = prel.getCluster();

      final List<RexNode> exprs = Lists.newArrayListWithExpectedSize(queryRowType.getFieldCount() + 1);
      final List<String> fieldNames = new ArrayList<>(queryRowType.getFieldNames());

      for (final RelDataTypeField field : queryRowType.getFieldList()) {
        exprs.add(RexInputRef.of(field.getIndex(), queryRowType));
      }

      // No partition columns.
      if (partitionColumns.size() == 0) {
        final ProjectPrel projectUnderWriter = new ProjectAllowDupPrel(cluster,
            cluster.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL), child, exprs, queryRowType);

        return prel.copy(projectUnderWriter.getTraitSet(),
            Collections.singletonList( (RelNode) projectUnderWriter));
      } else {
        // find list of partition columns.
        final List<RexNode> partitionColumnExprs = Lists.newArrayListWithExpectedSize(partitionColumns.size());
        for (final String colName : partitionColumns) {
          final RelDataTypeField field = childRowType.getField(colName, false, false);

          if (field == null) {
            throw UserException.validationError()
                .message("Partition column %s is not in the SELECT list of CTAS!", colName)
                .build(logger);
          }

          partitionColumnExprs.add(RexInputRef.of(field.getIndex(), childRowType));
        }

        // Add partition column comparator to Project's field name list.
        fieldNames.add(WriterPrel.PARTITION_COMPARATOR_FIELD);

        // Add partition column comparator to Project's expression list.
        final RexNode partionColComp = createPartitionColComparator(prel.getCluster().getRexBuilder(), partitionColumnExprs);
        exprs.add(partionColComp);


        final RelDataType rowTypeWithPCComp = RexUtil.createStructType(cluster.getTypeFactory(), exprs, fieldNames, null);

        final ProjectPrel projectUnderWriter = new ProjectAllowDupPrel(cluster,
            cluster.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL), child, exprs, rowTypeWithPCComp);

        return prel.copy(projectUnderWriter.getTraitSet(),
            Collections.singletonList( (RelNode) projectUnderWriter));
      }
    }

  }

  private RexNode createPartitionColComparator(final RexBuilder rexBuilder, List<RexNode> inputs) {
    final DrillSqlOperator op = new DrillSqlOperator(WriterPrel.PARTITION_COMPARATOR_FUNC, 1, true, false);

    final List<RexNode> compFuncs = Lists.newArrayListWithExpectedSize(inputs.size());

    for (final RexNode input : inputs) {
      compFuncs.add(rexBuilder.makeCall(op, ImmutableList.of(input)));
    }

    return composeDisjunction(rexBuilder, compFuncs);
  }

  private RexNode composeDisjunction(final RexBuilder rexBuilder, List<RexNode> compFuncs) {
    final DrillSqlOperator booleanOrFunc
             = new DrillSqlOperator("orNoShortCircuit", 2, true, false);
    RexNode node = compFuncs.remove(0);
    while (!compFuncs.isEmpty()) {
      node = rexBuilder.makeCall(booleanOrFunc, node, compFuncs.remove(0));
    }
    return node;
  }

  /**
   * Resolves schema taking into account type of table being created.
   * If schema path wasn't indicated in sql call and table type to be created is temporary
   * returns temporary workspace.
   *
   * If schema path is indicated, resolves to mutable drill schema.
   * Though if table to be created is temporary table, checks if resolved schema is valid default temporary workspace.
   *
   * @param sqlCreateTable create table call
   * @param defaultSchema default schema
   * @param config drill config
   * @return resolved schema
   * @throws UserException if attempted to create temporary table outside of temporary workspace
   */
  private AbstractSchema resolveSchema(SqlCreateTable sqlCreateTable, SchemaPlus defaultSchema, DrillConfig config) {
    AbstractSchema resolvedSchema;
    if (sqlCreateTable.isTemporary() && sqlCreateTable.getSchemaPath().size() == 0) {
      resolvedSchema = SchemaUtilites.getTemporaryWorkspace(defaultSchema, config);
    } else {
      resolvedSchema = SchemaUtilites.resolveToMutableDrillSchema(
          defaultSchema, sqlCreateTable.getSchemaPath());
    }

    if (sqlCreateTable.isTemporary()) {
      return SchemaUtilites.resolveToValidTemporaryWorkspace(resolvedSchema, config);
    }

    return resolvedSchema;
  }

  /**
   * Validates if table can be created in indicated schema
   * Checks if any object (persistent table / temporary table / view) with the same name exists
   * or if object with the same name exists but if not exists flag is set.
   *
   * @param drillSchema schema where table will be created
   * @param tableName table name
   * @param config drill config
   * @param userSession current user session
   * @param schemaPath schema path
   * @param checkTableNonExistence whether duplicate check is requested
   * @return if duplicate found in indicated schema
   * @throws UserException if duplicate found in indicated schema and no duplicate check requested
   */
  private boolean checkTableCreationPossibility(AbstractSchema drillSchema,
                                              String tableName,
                                              DrillConfig config,
                                              UserSession userSession,
                                              String schemaPath,
                                              boolean checkTableNonExistence) {
    boolean isTemporaryTable = userSession.isTemporaryTable(drillSchema, config, tableName);

    if (isTemporaryTable || SqlHandlerUtil.getTableFromSchema(drillSchema, tableName) != null) {
      if (checkTableNonExistence) {
        return false;
      } else {
        throw UserException.validationError()
          .message("A table or view with given name [%s] already exists in schema [%s]", tableName, schemaPath)
          .build(logger);
      }
    }
    return true;
  }
}
