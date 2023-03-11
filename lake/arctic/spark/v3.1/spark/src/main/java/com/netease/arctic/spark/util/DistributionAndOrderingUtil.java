/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.util;

import com.clearspring.analytics.util.Lists;
import com.netease.arctic.spark.SparkAdapterLoader;
import com.netease.arctic.spark.sql.connector.expressions.FileIndexBucket;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.DistributionHashMode;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.relocated.com.google.common.collect.ObjectArrays;
import org.apache.iceberg.transforms.SortOrderVisitor;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.spark.sql.catalyst.plans.logical.RepartitionByExpression;
import org.apache.spark.sql.connector.expressions.Expression;
import org.apache.spark.sql.connector.expressions.Expressions;
import org.apache.spark.sql.connector.expressions.NamedReference;
import org.apache.spark.sql.connector.expressions.Transform;

import java.util.Arrays;
import java.util.List;

import static org.apache.iceberg.spark.Spark3Util.toTransforms;

public class DistributionAndOrderingUtil {

  private static final ExpressionHelper expressionHelper = SparkAdapterLoader.getOrLoad().expressions();

  private static final NamedReference SPEC_ID = Expressions.column(MetadataColumns.SPEC_ID.name());
  private static final NamedReference PARTITION = Expressions.column(MetadataColumns.PARTITION_COLUMN_NAME);
  private static final NamedReference FILE_PATH = Expressions.column(MetadataColumns.FILE_PATH.name());
  private static final NamedReference ROW_POSITION = Expressions.column(MetadataColumns.ROW_POSITION.name());

  private static final Expression SPEC_ID_ORDER = expressionHelper.sort(SPEC_ID, true);
  private static final Expression PARTITION_ORDER = expressionHelper.sort(PARTITION, true);
  private static final Expression FILE_PATH_ORDER = expressionHelper.sort(FILE_PATH, true);
  private static final Expression ROW_POSITION_ORDER = expressionHelper.sort(ROW_POSITION, true);
  private static final Expression[] METADATA_ORDERS = new Expression[] {
      PARTITION_ORDER, FILE_PATH_ORDER, ROW_POSITION_ORDER
  };

  /**
   * Build a list of {@link Expression} indicate how to shuffle incoming data before writing.
   * The result of this method will convert to a list of {@link org.apache.spark.sql.catalyst.expressions.Expression}
   * which will be used by a {@link RepartitionByExpression} operator.
   *
   * @param table the arctic table to write
   * @param writeBase write to base store
   * @return array of expressions indicate how to shuffle incoming data.
   */
  public static Expression[] buildTableRequiredDistribution(ArcticTable table, boolean writeBase) {
    DistributionHashMode distributionHashMode = DistributionHashMode.autoSelect(
        table.isKeyedTable(),
        !table.spec().isUnpartitioned());

    List<Expression> distributionExpressions = Lists.newArrayList();

    if (distributionHashMode.isSupportPartition()) {
      distributionExpressions.addAll(Arrays.asList(toTransforms(table.spec())));
    }

    if (distributionHashMode.isSupportPrimaryKey()) {
      Transform transform = toTransformsFromPrimary(table, table.asKeyedTable().primaryKeySpec(), writeBase);
      distributionExpressions.add(transform);
    }

    return distributionExpressions.toArray(new Expression[0]);
  }

  private static Transform toTransformsFromPrimary(
      ArcticTable table,
      PrimaryKeySpec primaryKeySpec,
      boolean writeBase) {
    int numBucket = PropertyUtil.propertyAsInt(table.properties(),
        TableProperties.BASE_FILE_INDEX_HASH_BUCKET,
        TableProperties.BASE_FILE_INDEX_HASH_BUCKET_DEFAULT);

    if (!writeBase) {
      numBucket = PropertyUtil.propertyAsInt(table.properties(),
          TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET,
          TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET_DEFAULT);
    }
    return new FileIndexBucket(table.schema(), primaryKeySpec, numBucket - 1);
  }

  /**
   * Build a list of {@link Expression} to indicate how the incoming data will be sorted before write.
   * The result of this method will covert to {@link org.apache.spark.sql.catalyst.expressions.Expression} list and
   * be used for a local sort by add an {@link org.apache.spark.sql.catalyst.plans.logical.Sort} operator for
   * in-coming data.
   *
   * @param table the arctic table to write to
   * @param rowLevelOperation is this writing is an row-level-operation or a batch overwrite.
   * @param writeBase is this writing happened in base store.
   * @return array of expression to indicate how incoming data will be sorted.
   */
  public static Expression[] buildTableRequiredSortOrder(
      ArcticTable table,
      boolean rowLevelOperation,
      boolean writeBase) {
    Schema schema = table.schema();
    PartitionSpec partitionSpec = table.spec();
    PrimaryKeySpec keySpec = PrimaryKeySpec.noPrimaryKey();
    if (table.isKeyedTable()) {
      keySpec = table.asKeyedTable().primaryKeySpec();
    }
    boolean withMetaColumn = table.isUnkeyedTable() && rowLevelOperation;

    if (partitionSpec.isUnpartitioned() && !keySpec.primaryKeyExisted() && !withMetaColumn) {
      return new Expression[0];
    }

    SortOrder.Builder builder = SortOrder.builderFor(schema);
    if (partitionSpec.isPartitioned()) {
      for (PartitionField field: partitionSpec.fields()) {
        String sourceName = schema.findColumnName(field.sourceId());
        builder.asc(org.apache.iceberg.expressions.Expressions.transform(sourceName, field.transform()));
      }
    }
    SortOrder sortOrder = builder.build();
    List<Expression> converted = SortOrderVisitor.visit(sortOrder, new SortOrderToSpark(expressionHelper));

    if (keySpec.primaryKeyExisted()) {
      Transform fileIndexBucket = toTransformsFromPrimary(table, keySpec, writeBase);
      converted.add(expressionHelper.sort(fileIndexBucket, true));
    }

    Expression[] orders = converted.toArray(new Expression[0]);

    if (withMetaColumn) {
      orders = ObjectArrays.concat(orders, METADATA_ORDERS, Expression.class);
    }
    return orders;
  }

}
