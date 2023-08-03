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

package com.netease.arctic.spark.util;

import com.netease.arctic.spark.table.ArcticSparkTable;
import com.netease.arctic.table.DistributionHashMode;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;
import org.apache.iceberg.DistributionMode;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.util.ByteBuffers;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.connector.distributions.ClusteredDistribution;
import org.apache.spark.sql.connector.distributions.Distributions;
import org.apache.spark.sql.connector.expressions.Expressions;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE_DEFAULT;
import static org.apache.iceberg.spark.Spark3Util.toTransforms;

public class ArcticSparkUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticSparkUtils.class);

  public static TableCatalogAndIdentifier tableCatalogAndIdentifier(SparkSession spark, List<String> nameParts) {
    Spark3Util.CatalogAndIdentifier catalogAndIdentifier = Spark3Util.catalogAndIdentifier(
        spark, nameParts, spark.sessionState().catalogManager().currentCatalog());
    CatalogPlugin catalog = catalogAndIdentifier.catalog();
    Preconditions.checkArgument(catalog instanceof TableCatalog,
        "Cannot resolver name-parts %s to catalog and identifier, %s is not a table catalog",
        Joiner.on(',').join(nameParts), catalog.name());
    return new TableCatalogAndIdentifier((TableCatalog) catalog, catalogAndIdentifier.identifier());
  }

  public static class TableCatalogAndIdentifier {
    TableCatalog tableCatalog;
    Identifier identifier;

    public TableCatalogAndIdentifier(TableCatalog tableCatalog, Identifier identifier) {
      this.tableCatalog = tableCatalog;
      this.identifier = identifier;
    }

    public TableCatalog catalog() {
      return this.tableCatalog;
    }

    public Identifier identifier() {
      return this.identifier;
    }
  }

  public static ClusteredDistribution buildRequiredDistribution(ArcticSparkTable arcticSparkTable) {
    // Fallback to use distribution mode parsed from table properties .
    String modeName = PropertyUtil.propertyAsString(
        arcticSparkTable.properties(),
        WRITE_DISTRIBUTION_MODE,
        WRITE_DISTRIBUTION_MODE_DEFAULT);
    DistributionMode writeMode = DistributionMode.fromName(modeName);
    switch (writeMode) {
      case NONE:
        return null;

      case HASH:
        DistributionHashMode distributionHashMode = DistributionHashMode.valueOfDesc(
            arcticSparkTable.properties().getOrDefault(
                TableProperties.WRITE_DISTRIBUTION_HASH_MODE,
                TableProperties.WRITE_DISTRIBUTION_HASH_MODE_DEFAULT));
        List<Transform> transforms = new ArrayList<>();
        if (DistributionHashMode.AUTO.equals(distributionHashMode)) {
          distributionHashMode = DistributionHashMode.autoSelect(
              arcticSparkTable.table().isKeyedTable(),
              !arcticSparkTable.table().spec().isUnpartitioned());
        }
        if (distributionHashMode.isSupportPrimaryKey()) {
          Transform transform = toTransformsFromPrimary(
              arcticSparkTable,
              arcticSparkTable.table().asKeyedTable().primaryKeySpec());
          transforms.add(transform);
          if (distributionHashMode.isSupportPartition()) {
            transforms.addAll(Arrays.asList(toTransforms(arcticSparkTable.table().spec())));
          }
          return Distributions.clustered(transforms.stream().filter(Objects::nonNull).toArray(Transform[]::new));
        } else {
          if (distributionHashMode.isSupportPartition()) {
            return Distributions.clustered(toTransforms(arcticSparkTable.table().spec()));
          } else {
            return null;
          }
        }

      case RANGE:
        LOG.warn("Fallback to use 'none' distribution mode, because {}={} is not supported in spark now",
            WRITE_DISTRIBUTION_MODE, DistributionMode.RANGE.modeName());
        return null;

      default:
        throw new RuntimeException("Unrecognized write.distribution-mode: " + writeMode);
    }
  }

  private static Transform toTransformsFromPrimary(ArcticSparkTable arcticSparkTable, PrimaryKeySpec primaryKeySpec) {
    int numBucket = PropertyUtil.propertyAsInt(arcticSparkTable.properties(),
        TableProperties.BASE_FILE_INDEX_HASH_BUCKET, TableProperties.BASE_FILE_INDEX_HASH_BUCKET_DEFAULT);
    return Expressions.bucket(numBucket, primaryKeySpec.fieldNames().get(0));
  }

  public static Object convertConstant(Type type, Object value) {
    if (value == null) {
      return null;
    }

    switch (type.typeId()) {
      case DECIMAL:
        return Decimal.apply((BigDecimal) value);
      case STRING:
        if (value instanceof Utf8) {
          Utf8 utf8 = (Utf8) value;
          return UTF8String.fromBytes(utf8.getBytes(), 0, utf8.getByteLength());
        }
        return UTF8String.fromString(value.toString());
      case FIXED:
        if (value instanceof byte[]) {
          return value;
        } else if (value instanceof GenericData.Fixed) {
          return ((GenericData.Fixed) value).bytes();
        }
        return ByteBuffers.toByteArray((ByteBuffer) value);
      case BINARY:
        return ByteBuffers.toByteArray((ByteBuffer) value);
      default:
    }
    return value;
  }
}
