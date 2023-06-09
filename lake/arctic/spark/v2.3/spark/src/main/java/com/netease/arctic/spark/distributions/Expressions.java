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

package com.netease.arctic.spark.distributions;

import com.netease.arctic.spark.distibutions.LogicalExpressions;
import org.apache.spark.annotation.InterfaceStability;
import scala.collection.JavaConverters;

import java.util.Arrays;

@InterfaceStability.Evolving
public class Expressions {
  private Expressions() {
  }

  public static Transform apply(String name, Expression... args) {
    return LogicalExpressions.apply(
        name,
        JavaConverters.asScalaBufferConverter(Arrays.asList(args)).asScala().toSeq());
  }

  public static NamedReference column(String name) {
    return LogicalExpressions.parseReference(name);
  }

  public static Transform bucket(int numBuckets, String... columns) {
    NamedReference[] references = Arrays.stream(columns)
        .map(Expressions::column)
        .toArray(NamedReference[]::new);
    return LogicalExpressions.bucket(numBuckets, references);
  }

  public static Transform identity(String column) {
    return LogicalExpressions.identity(Expressions.column(column));
  }
}
