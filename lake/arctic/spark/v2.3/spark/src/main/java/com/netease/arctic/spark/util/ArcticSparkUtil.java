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

import com.netease.arctic.spark.distributions.Distribution;
import com.netease.arctic.spark.distributions.Distributions;
import com.netease.arctic.spark.distributions.Expressions;
import com.netease.arctic.spark.distributions.Transform;
import com.netease.arctic.spark.parquet.SparkParquetRowReaders;
import com.netease.arctic.spark.source.ArcticSparkTable;
import com.netease.arctic.table.DistributionHashMode;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.DistributionMode;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.transforms.PartitionSpecVisitor;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.util.ByteBuffers;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.RuntimeConfig;
import org.apache.spark.sql.types.BinaryType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE_DEFAULT;

public class ArcticSparkUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticSparkUtil.class);
  public static final String CATALOG_URL = "spark.sql.arctic.catalog.url";
  public static final String SQL_DELEGATE_HIVE_TABLE = "spark.sql.arctic.delegate.enabled";

  public static String catalogUrl(RuntimeConfig conf) {
    String catalogUrl = conf.get(CATALOG_URL, "");
    if (StringUtils.isBlank(catalogUrl)) {
      throw new IllegalArgumentException("spark.sql.arctic.catalog.url is blank");
    }
    return catalogUrl;
  }

  public static boolean delegateHiveTable(RuntimeConfig config) {
    String val = config.get(SQL_DELEGATE_HIVE_TABLE, "true");
    return Boolean.parseBoolean(val);
  }

  public static Object convertConstant(Type type, Object value) {
    if (value == null) {
      return null;
    }

    switch (type.typeId()) {
      case DECIMAL:
        return Decimal.apply((BigDecimal) value);
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

  public static Object[] convertRowObject(Object[] objects, Schema schema) {
    for (int i = 0; i < objects.length; i++) {
      Object object = objects[i];
      Type type = schema.columns().get(i).type();
      /*if (object instanceof UTF8String) {
        objects[i] = object.toString();
      } else*/
      if (object instanceof Double) {
        objects[i] = new BigDecimal((Double) object).doubleValue();
      } else if (object instanceof Float) {
        objects[i] = new BigDecimal((Float) object).floatValue();
      } else if (object instanceof Decimal) {
        objects[i] = ((Decimal) object).toJavaBigDecimal();
      } else if (object instanceof BinaryType) {
        objects[i] = ByteBuffer.wrap((byte[]) object);
      } else if (object instanceof SparkParquetRowReaders.ReusableMapData) {
        Object[] keyArray = ((SparkParquetRowReaders.ReusableMapData) object).keyArray().array();
        Object[] valueArray = ((SparkParquetRowReaders.ReusableMapData) object).valueArray().array();
        Map map = new HashMap();
        for (int j = 0; j < keyArray.length; j++) {
          if (keyArray[j] instanceof UTF8String) {
            keyArray[j] = keyArray[j].toString();
          }
          if (valueArray[j] instanceof UTF8String) {
            valueArray[j] = valueArray[j].toString();
          }
          map.put(keyArray[j], valueArray[j]);
        }
        scala.collection.Map scalaMap = (scala.collection.Map) JavaConverters.mapAsScalaMapConverter(map).asScala();
        objects[i] = scalaMap;
      } else if (object instanceof SparkParquetRowReaders.ReusableArrayData) {
        Object[] array = ((SparkParquetRowReaders.ReusableArrayData) object).array();
        for (int j = 0; j < array.length; j++) {
          if (array[j] instanceof UTF8String) {
            array[j] = array[j].toString();
          }
        }
        Seq seq = JavaConverters.asScalaIteratorConverter(Arrays.asList(array).iterator()).asScala().toSeq();
        objects[i] = seq;
      }
    }
    return objects;
  }

  public static Row convertInterRowToRow(Row row, Schema schema) {
    Seq<Object> objectSeq = row.toSeq();
    Object[] objects = JavaConverters.seqAsJavaListConverter(objectSeq).asJava().toArray();
    return RowFactory.create(convertRowObject(objects, schema));
  }

  public static Distribution buildRequiredDistribution(ArcticSparkTable arcticSparkTable) {
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

  public static Transform[] toTransforms(PartitionSpec spec) {
    List<Transform> transforms = PartitionSpecVisitor.visit(spec,
        new PartitionSpecVisitor<Transform>() {
          @Override
          public Transform identity(String sourceName, int sourceId) {
            return Expressions.identity(sourceName);
          }

          @Override
          public Transform bucket(String sourceName, int sourceId, int numBuckets) {
            return Expressions.bucket(numBuckets, sourceName);
          }

          @Override
          public Transform alwaysNull(int fieldId, String sourceName, int sourceId) {
            // do nothing for alwaysNull, it doesn't need to be converted to a transform
            return null;
          }

          @Override
          public Transform unknown(int fieldId, String sourceName, int sourceId, String transform) {
            return Expressions.apply(transform, Expressions.column(sourceName));
          }
        });

    return transforms.stream().filter(Objects::nonNull).toArray(Transform[]::new);
  }

}
