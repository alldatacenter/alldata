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
package org.apache.drill.exec.store.mongo.plan;

import com.mongodb.client.model.Aggregates;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.core.Union;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.mongo.MongoAggregateUtils;
import org.apache.drill.exec.store.mongo.MongoFilterBuilder;
import org.apache.drill.exec.store.mongo.MongoGroupScan;
import org.apache.drill.exec.store.mongo.MongoScanSpec;
import org.apache.drill.exec.store.mongo.MongoStoragePlugin;
import org.apache.drill.exec.store.plan.AbstractPluginImplementor;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginAggregateRel;
import org.apache.drill.exec.store.plan.rel.PluginFilterRel;
import org.apache.drill.exec.store.plan.rel.PluginLimitRel;
import org.apache.drill.exec.store.plan.rel.PluginProjectRel;
import org.apache.drill.exec.store.plan.rel.PluginSortRel;
import org.apache.drill.exec.store.plan.rel.PluginUnionRel;
import org.apache.drill.exec.store.plan.rel.StoragePluginTableScan;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PluginImplementor} for Mongo.
 * This class tries to convert operators to use {@link com.mongodb.client.MongoCollection#find}
 * if only simple project and filter expressions are present,
 * otherwise {@link com.mongodb.client.MongoCollection#aggregate} is used to obtain data from Mongo.
 */
public class MongoPluginImplementor extends AbstractPluginImplementor {

  private MongoGroupScan groupScan;

  private List<Bson> operations;

  private Document filters;

  private List<SchemaPath> columns;

  private boolean runAggregate;

  @Override
  public void implement(PluginAggregateRel aggregate) throws IOException {
    runAggregate = true;
    visitChild(aggregate.getInput());

    operations.addAll(
      MongoAggregateUtils.getAggregateOperations(aggregate, aggregate.getInput().getRowType()));
    List<String> outNames = MongoAggregateUtils.mongoFieldNames(aggregate.getRowType());
    columns = outNames.stream()
      .map(SchemaPath::getSimplePath)
      .collect(Collectors.toList());
  }

  @Override
  public void implement(PluginFilterRel filter) throws IOException {
    visitChild(filter.getInput());

    LogicalExpression conditionExp = DrillOptiq.toDrill(
      new DrillParseContext(PrelUtil.getPlannerSettings(filter.getCluster().getPlanner())),
      filter.getInput(),
      filter.getCondition());
    MongoFilterBuilder mongoFilterBuilder = new MongoFilterBuilder(conditionExp);
    if (runAggregate) {
      Bson convertedFilterExpression = Aggregates.match(mongoFilterBuilder.parseTree()).toBsonDocument();
      operations.add(convertedFilterExpression);
    } else {
      filters = mongoFilterBuilder.parseTree();
    }
  }

  @Override
  public void implement(PluginLimitRel limit) throws IOException {
    runAggregate = true;
    visitChild(limit.getInput());

    if (limit.getOffset() != null) {
      operations.add(
        Aggregates.skip(rexLiteralIntValue((RexLiteral) limit.getOffset())).toBsonDocument());
    }
    if (limit.getFetch() != null) {
      operations.add(
        Aggregates.limit(rexLiteralIntValue((RexLiteral) limit.getFetch())).toBsonDocument());
    }
  }

  @Override
  public void implement(PluginProjectRel project) throws IOException {
    runAggregate = runAggregate || project.getProjects().stream()
      .anyMatch(expression -> !expression.isA(SqlKind.INPUT_REF));

    visitChild(project.getInput());

    if (runAggregate) {
      RexToMongoTranslator translator =
        new RexToMongoTranslator(
          (JavaTypeFactory) project.getCluster().getTypeFactory(),
          MongoAggregateUtils.mongoFieldNames(project.getInput().getRowType()));
      List<BsonElement> items = new ArrayList<>();
      for (Pair<RexNode, String> pair : project.getNamedProjects()) {
        String name = pair.right;
        BsonValue expr = pair.left.accept(translator);
        items.add(expr.equals(new BsonString("$" + name))
          ? new BsonElement(MongoAggregateUtils.maybeQuote(name), new BsonInt32(1))
          : new BsonElement(MongoAggregateUtils.maybeQuote(name), expr));
      }
      BsonDocument projection = Aggregates.project(new BsonDocument(items)).toBsonDocument();

      operations.add(projection);
      List<String> outNames = MongoAggregateUtils.mongoFieldNames(project.getRowType());
      this.columns = outNames.stream()
        .map(SchemaPath::getSimplePath)
        .collect(Collectors.toList());
    } else {
      List<String> outNames = MongoAggregateUtils.mongoFieldNames(project.getRowType());
      this.columns = outNames.stream()
        .map(SchemaPath::getSimplePath)
        .collect(Collectors.toList());
    }
  }

  @Override
  public void implement(PluginSortRel sort) throws IOException {
    runAggregate = true;
    visitChild(sort.getInput());

    if (!sort.collation.getFieldCollations().isEmpty()) {
      BsonDocument sortKeys = new BsonDocument();
      List<RelDataTypeField> fields = sort.getRowType().getFieldList();
      for (RelFieldCollation fieldCollation : sort.collation.getFieldCollations()) {
        String name = fields.get(fieldCollation.getFieldIndex()).getName();
        sortKeys.put(name, new BsonInt32(direction(fieldCollation)));
      }

      operations.add(Aggregates.sort(sortKeys).toBsonDocument());
    }
    if (sort.offset != null) {
      operations.add(
        Aggregates.skip(rexLiteralIntValue((RexLiteral) sort.offset)).toBsonDocument());
    }
    if (sort.fetch != null) {
      operations.add(
        Aggregates.limit(rexLiteralIntValue((RexLiteral) sort.fetch)).toBsonDocument());
    }
  }

  private int rexLiteralIntValue(RexLiteral offset) {
    return ((BigDecimal) offset.getValue()).intValue();
  }

  @Override
  public void implement(PluginUnionRel union) throws IOException {
    runAggregate = true;

    MongoPluginImplementor childImplementor = new MongoPluginImplementor();
    childImplementor.runAggregate = true;

    boolean firstProcessed = false;
    for (RelNode input : union.getInputs()) {
      if (!firstProcessed) {
        this.visitChild(input);
        firstProcessed = true;
      } else {
        childImplementor.visitChild(input);
        operations.add(
          Aggregates.unionWith(childImplementor.groupScan.getScanSpec().getCollectionName(), childImplementor.operations).toBsonDocument()
        );
      }
    }
  }

  @Override
  public void implement(StoragePluginTableScan scan) {
    groupScan = (MongoGroupScan) scan.getGroupScan();
    operations = this.groupScan.getScanSpec().getOperations().stream()
      .map(BsonDocument::parse)
      .collect(Collectors.toList());
    filters = Optional.ofNullable(groupScan.getScanSpec().getFilters())
      .map(Document::parse)
      .orElse(null);
    columns = groupScan.getColumns();
  }

  @Override
  public boolean canImplement(Aggregate aggregate) {
    return hasPluginGroupScan(aggregate)
      && aggregate.getGroupType() == Aggregate.Group.SIMPLE
      && aggregate.getAggCallList().stream()
      .noneMatch(AggregateCall::isDistinct)
      && aggregate.getAggCallList().stream()
      .allMatch(MongoAggregateUtils::supportsAggregation);
  }

  @Override
  public boolean canImplement(Filter filter) {
    if (hasPluginGroupScan(filter)) {
      LogicalExpression conditionExp = DrillOptiq.toDrill(
        new DrillParseContext(PrelUtil.getPlannerSettings(filter.getCluster().getPlanner())),
        filter.getInput(),
        filter.getCondition());
      MongoFilterBuilder filterBuilder = new MongoFilterBuilder(conditionExp);
      filterBuilder.parseTree();
      return filterBuilder.isAllExpressionsConverted();
    }
    return false;
  }

  @Override
  public boolean canImplement(DrillLimitRelBase limit) {
    return hasPluginGroupScan(limit);
  }

  @Override
  public boolean canImplement(Project project) {
    return hasPluginGroupScan(project) &&
      project.getProjects().stream()
        .allMatch(RexToMongoTranslator::supportsExpression);
  }

  @Override
  public boolean canImplement(Sort sort) {
    return hasPluginGroupScan(sort);
  }

  @Override
  public boolean canImplement(Union union) {
    // allow converting for union all only, since Drill adds extra aggregation for union distinct,
    // so we will convert both union all and aggregation later
    return union.all && hasPluginGroupScan(union);
  }

  @Override
  public boolean canImplement(TableScan scan) {
    return hasPluginGroupScan(scan);
  }

  @Override
  protected Class<? extends StoragePlugin> supportedPlugin() {
    return MongoStoragePlugin.class;
  }

  @Override
  public GroupScan getPhysicalOperator() {
    MongoScanSpec scanSpec = groupScan.getScanSpec();
    List<String> operations = this.operations.stream()
      .map(op -> op.toBsonDocument().toJson())
      .collect(Collectors.toList());
    String filters = Optional.ofNullable(this.filters)
      .map(Document::toJson)
      .orElse(null);
    MongoScanSpec newSpec = new MongoScanSpec(scanSpec.getDbName(), scanSpec.getCollectionName(),
      filters, operations);
    return new MongoGroupScan(groupScan.getUserName(), groupScan.getStoragePlugin(),
      newSpec, columns, runAggregate);
  }

  @Override
  protected boolean hasPluginGroupScan(RelNode node) {
    return findGroupScan(node) instanceof MongoGroupScan;
  }

  private static int direction(RelFieldCollation fieldCollation) {
    switch (fieldCollation.getDirection()) {
      case DESCENDING:
      case STRICTLY_DESCENDING:
        return -1;
      case ASCENDING:
      case STRICTLY_ASCENDING:
      default:
        return 1;
    }
  }
}
