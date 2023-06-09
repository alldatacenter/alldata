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

package com.netease.arctic.spark.sql.catalyst.parser;

import com.netease.arctic.spark.sql.catalyst.plans.MigrateToArcticStatement;
import com.netease.arctic.spark.sql.parser.ArcticSqlCommandBaseVisitor;
import com.netease.arctic.spark.sql.parser.ArcticSqlCommandParser;
import org.antlr.v4.runtime.RuleContext;
import org.apache.spark.sql.catalyst.SQLConfHelper;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.List;
import java.util.stream.Collectors;

public class ArcticCommandAstParser extends ArcticSqlCommandBaseVisitor<Object>
    implements SQLConfHelper {

  @Override
  public LogicalPlan visitArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx) {
    return (LogicalPlan) ArcticParserUtils.withOrigin(ctx, () -> visit(ctx.arcticStatement()));
  }

  @Override
  public LogicalPlan visitMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx) {
    // return super.visitMigrateStatement(ctx);
    Seq<String> source = multipartIdentifier(ctx.source);
    Seq<String> target = multipartIdentifier(ctx.target);
    return new MigrateToArcticStatement(
        source, target
    );
  }

  private Seq<String> multipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx) {
    List<String> identifier = ctx.parts.stream().map(RuleContext::getText).collect(Collectors.toList());
    return JavaConverters.asScalaBuffer(identifier);
  }
}
