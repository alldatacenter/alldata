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
package org.apache.drill.exec.store.mongo;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.drill.exec.store.mongo.common.MongoOp;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MongoAggregateUtils {

  public static List<String> mongoFieldNames(RelDataType rowType) {
    List<String> renamed = rowType.getFieldNames().stream()
        .map(name -> name.startsWith("$") ? "_" + name.substring(2) : name)
        .collect(Collectors.toList());
    return SqlValidatorUtil.uniquify(renamed, true);
  }

  public static String maybeQuote(String s) {
    if (!needsQuote(s)) {
      return s;
    }
    return quote(s);
  }

  public static String quote(String s) {
    return "'" + s + "'";
  }

  private static boolean needsQuote(String s) {
    for (int i = 0, n = s.length(); i < n; i++) {
      char c = s.charAt(i);
      if (!Character.isJavaIdentifierPart(c)
          || c == '$') {
        return true;
      }
    }
    return false;
  }

  public static List<Bson> getAggregateOperations(Aggregate aggregate, RelDataType rowType) {
    List<String> inNames = mongoFieldNames(rowType);
    List<String> outNames = mongoFieldNames(aggregate.getRowType());
    Object id;
    if (aggregate.getGroupSet().cardinality() == 1) {
      String inName = inNames.get(aggregate.getGroupSet().nth(0));
      id = "$" + inName;
    } else {
      List<BsonElement> elements =
          StreamSupport.stream(aggregate.getGroupSet().spliterator(), false)
              .map(inNames::get)
              .map(inName -> new BsonElement(inName, new BsonString("$" + inName)))
              .collect(Collectors.toList());
      id = new BsonDocument(elements);
    }
    int outNameIndex = aggregate.getGroupSet().cardinality();
    List<BsonField> accumList = new ArrayList<>();
    for (AggregateCall aggCall : aggregate.getAggCallList()) {
      accumList.add(bsonAggregate(inNames, outNames.get(outNameIndex++), aggCall));
    }
    List<Bson> operationsList = new ArrayList<>();
    operationsList.add(Aggregates.group(id, accumList).toBsonDocument());
    List<BsonElement> projectFields = new ArrayList<>();
    if (aggregate.getGroupSet().cardinality() == 1) {
      for (int index = 0; index < outNames.size(); index++) {
        String outName = outNames.get(index);
        projectFields.add(new BsonElement(maybeQuote(outName),
            new BsonString("$" + (index == 0 ? "_id" : outName))));
      }
    } else {
      projectFields.add(new BsonElement("_id", new BsonInt32(0)));
      for (int group : aggregate.getGroupSet()) {
        projectFields.add(
            new BsonElement(maybeQuote(outNames.get(group)),
                new BsonString("$_id." + outNames.get(group))));
      }
      outNameIndex = aggregate.getGroupSet().cardinality();
      for (AggregateCall ignored : aggregate.getAggCallList()) {
        String outName = outNames.get(outNameIndex++);
        projectFields.add(new BsonElement(maybeQuote(outName), new BsonString("$" + outName)));
      }
    }
    if (!aggregate.getGroupSet().isEmpty()) {
      operationsList.add(Aggregates.project(new BsonDocument(projectFields)).toBsonDocument());
    }

    return operationsList;
  }

  private static BsonField bsonAggregate(List<String> inNames, String outName, AggregateCall aggCall) {
    String aggregationName = aggCall.getAggregation().getName();
    List<Integer> args = aggCall.getArgList();
    if (aggregationName.equals(SqlStdOperatorTable.COUNT.getName())) {
      Object expr;
      if (args.size() == 0) {
        // count(*) case
        expr = 1;
      } else {
        assert args.size() == 1;
        String inName = inNames.get(args.get(0));
        expr = new BsonDocument(MongoOp.COND.getCompareOp(),
            new BsonArray(Arrays.asList(
                new Document(MongoOp.EQUAL.getCompareOp(),
                    new BsonArray(Arrays.asList(
                        new BsonString(quote(inName)),
                        BsonNull.VALUE))).toBsonDocument(),
                new BsonInt32(0),
                new BsonInt32(1)
            ))
        );
      }
      return Accumulators.sum(maybeQuote(outName), expr);
    } else {
      BiFunction<String, Object, BsonField> mongoAccumulator = mongoAccumulator(aggregationName);
      if (mongoAccumulator != null) {
        return mongoAccumulator.apply(maybeQuote(outName), "$" + inNames.get(args.get(0)));
      }
    }
    return null;
  }

  private static <T> BiFunction<String, T, BsonField> mongoAccumulator(String aggregationName) {
    if (aggregationName.equals(SqlStdOperatorTable.SUM.getName())
        || aggregationName.equals(SqlStdOperatorTable.SUM0.getName())) {
      return Accumulators::sum;
    } else if (aggregationName.equals(SqlStdOperatorTable.MIN.getName())) {
      return Accumulators::min;
    } else if (aggregationName.equals(SqlStdOperatorTable.MAX.getName())) {
      return Accumulators::max;
    } else if (aggregationName.equals(SqlStdOperatorTable.AVG.getName())) {
      return Accumulators::avg;
    } else if (aggregationName.equals(SqlStdOperatorTable.FIRST.getName())) {
      return Accumulators::first;
    } else if (aggregationName.equals(SqlStdOperatorTable.LAST.getName())) {
      return Accumulators::last;
    } else if (aggregationName.equals(SqlStdOperatorTable.STDDEV.getName())
      || aggregationName.equals(SqlStdOperatorTable.STDDEV_SAMP.getName())) {
      return Accumulators::stdDevSamp;
    } else if (aggregationName.equals(SqlStdOperatorTable.STDDEV_POP.getName())) {
      return Accumulators::stdDevPop;
    }
    return null;
  }

  public static boolean supportsAggregation(AggregateCall aggregateCall) {
    String name = aggregateCall.getAggregation().getName();
    return name.equals(SqlStdOperatorTable.COUNT.getName())
      || name.equals(SqlStdOperatorTable.SUM.getName())
      || name.equals(SqlStdOperatorTable.SUM0.getName())
      || name.equals(SqlStdOperatorTable.MIN.getName())
      || name.equals(SqlStdOperatorTable.MAX.getName())
      || name.equals(SqlStdOperatorTable.AVG.getName())
      || name.equals(SqlStdOperatorTable.FIRST.getName())
      || name.equals(SqlStdOperatorTable.LAST.getName())
      || name.equals(SqlStdOperatorTable.STDDEV.getName())
      || name.equals(SqlStdOperatorTable.STDDEV_SAMP.getName())
      || name.equals(SqlStdOperatorTable.STDDEV_POP.getName());
  }
}
