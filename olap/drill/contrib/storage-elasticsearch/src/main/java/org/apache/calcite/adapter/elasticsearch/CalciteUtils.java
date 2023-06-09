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
package org.apache.calcite.adapter.elasticsearch;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.store.elasticsearch.ElasticsearchStorageConfig;
import org.apache.drill.exec.store.elasticsearch.plan.ElasticSearchEnumerablePrelContext;
import org.apache.drill.exec.store.elasticsearch.plan.ElasticsearchFilterRule;
import org.apache.drill.exec.store.elasticsearch.plan.ElasticsearchProjectRule;
import org.apache.drill.exec.store.enumerable.plan.EnumerableIntermediatePrelConverterRule;
import org.apache.drill.exec.store.enumerable.plan.VertexDrelConverterRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CalciteUtils {

  private static final List<String> BANNED_RULES =
      Arrays.asList("ElasticsearchProjectRule", "ElasticsearchFilterRule");

  public static final Predicate<RelOptRule> RULE_PREDICATE =
      relOptRule -> BANNED_RULES.stream()
          .noneMatch(banned -> relOptRule.toString().startsWith(banned));

  public static final VertexDrelConverterRule ELASTIC_DREL_CONVERTER_RULE =
      new VertexDrelConverterRule(ElasticsearchRel.CONVENTION);

  public static final RelOptRule ENUMERABLE_INTERMEDIATE_PREL_CONVERTER_RULE =
      new EnumerableIntermediatePrelConverterRule(
          new ElasticSearchEnumerablePrelContext(ElasticsearchStorageConfig.NAME), ElasticsearchRel.CONVENTION);

  public static Set<RelOptRule> elasticSearchRules() {
    // filters Calcite implementations of some rules and adds alternative versions specific for Drill
    Set<RelOptRule> rules = Arrays.stream(ElasticsearchRules.RULES)
        .filter(RULE_PREDICATE)
        .collect(Collectors.toSet());
    rules.add(ENUMERABLE_INTERMEDIATE_PREL_CONVERTER_RULE);
    rules.add(ELASTIC_DREL_CONVERTER_RULE);
    rules.add(ElasticsearchProjectRule.INSTANCE);
    rules.add(ElasticsearchFilterRule.INSTANCE);
    return rules;
  }

  public static ConverterRule getElasticsearchToEnumerableConverterRule() {
    return ElasticsearchToEnumerableConverterRule.INSTANCE;
  }

  public static ElasticsearchProject createProject(RelTraitSet traitSet, RelNode input,
      List<? extends RexNode> projects, RelDataType rowType) {
    return new ElasticsearchProject(input.getCluster(), traitSet, input, projects, rowType);
  }

  public static ElasticsearchFilter createFilter(RelTraitSet traitSet, RelNode input,
      RexNode condition) {
    return new ElasticsearchFilter(input.getCluster(), traitSet, input, condition);
  }

  public static void analyzePredicate(RexNode condition) throws PredicateAnalyzer.ExpressionNotAnalyzableException {
    PredicateAnalyzer.analyze(condition);
  }
}
