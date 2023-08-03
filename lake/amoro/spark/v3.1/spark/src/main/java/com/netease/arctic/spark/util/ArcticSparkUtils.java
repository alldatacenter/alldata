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

import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.util.ByteBuffers;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;

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
