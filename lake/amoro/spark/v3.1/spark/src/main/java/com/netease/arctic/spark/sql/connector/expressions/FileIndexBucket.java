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

package com.netease.arctic.spark.sql.connector.expressions;

import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.spark.sql.connector.expressions.Expression;
import org.apache.spark.sql.connector.expressions.Expressions;
import org.apache.spark.sql.connector.expressions.NamedReference;
import org.apache.spark.sql.connector.expressions.Transform;

import java.util.Arrays;
import java.util.List;

public class FileIndexBucket implements Transform {

  private final Schema schema;
  private final PrimaryKeyData primaryKeyData;
  private final int mask;
  private final String[] primaryKeyColumns;


  public FileIndexBucket(Schema schema, PrimaryKeySpec keySpec, int mask) {
    this.primaryKeyData = new PrimaryKeyData(keySpec, schema);
    this.mask = mask;
    this.primaryKeyColumns = keySpec.fields().stream()
        .map(PrimaryKeySpec.PrimaryKeyField::fieldName)
        .toArray(String[]::new);
    this.schema = schema;
  }

  public Schema schema() {
    return this.schema;
  }

  public PrimaryKeyData primaryKeyData() {
    return this.primaryKeyData;
  }

  public int mask() {
    return this.mask;
  }

  @Override
  public String name() {
    return "FileIndexBucket";
  }

  @Override
  public NamedReference[] references() {
    return Arrays.stream(this.primaryKeyColumns).map(
        Expressions::column
    ).toArray(NamedReference[]::new);
  }

  @Override
  public Expression[] arguments() {
    List<Expression> arguments = Lists.newArrayList();
    arguments.add(Expressions.literal(mask));
    arguments.addAll(Arrays.asList(references()));
    return arguments.toArray(new Expression[0]);
  }

  @Override
  public String describe() {
    String columns = Joiner.on(", ").join(primaryKeyColumns);
    return "FileIndexBucket(" + mask + ", " + columns + ")";
  }
}
