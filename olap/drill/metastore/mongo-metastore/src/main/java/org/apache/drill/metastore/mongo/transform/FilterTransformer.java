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
package org.apache.drill.metastore.mongo.transform;

import com.mongodb.client.model.Filters;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transforms given input into Mongo {@link Document} which is used as filter
 * to retrieve, overwrite or delete Metastore component data.
 */
public class FilterTransformer {

  public Bson transform(FilterExpression filter) {
    return filter == null ? new Document() : filter.accept(FilterExpressionVisitor.get());
  }

  public Bson transform(Set<MetadataType> metadataTypes) {
    if (metadataTypes.contains(MetadataType.ALL)) {
      return new BsonDocument();
    }

    Set<String> inConditionValues = metadataTypes.stream()
      .map(Enum::name)
      .collect(Collectors.toSet());

    if (inConditionValues.size() == 1) {
      return Filters.eq(MetastoreColumn.METADATA_TYPE.columnName(), inConditionValues.iterator().next());
    }
    return Filters.in(MetastoreColumn.METADATA_TYPE.columnName(), inConditionValues);
  }

  public Bson combine(Bson... expressions) {
    if (expressions.length == 0) {
      return new BsonDocument();
    }
    if (expressions.length == 1) {
      return expressions[0];
    }
    return Filters.and(expressions[0], expressions[1]);
  }
}
