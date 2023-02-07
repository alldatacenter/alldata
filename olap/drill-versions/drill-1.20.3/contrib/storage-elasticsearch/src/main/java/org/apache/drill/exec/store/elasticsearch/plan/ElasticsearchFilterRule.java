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
package org.apache.drill.exec.store.elasticsearch.plan;

import org.apache.calcite.adapter.elasticsearch.CalciteUtils;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchFilter;
import org.apache.calcite.adapter.elasticsearch.ElasticsearchRel;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchFilterRule extends ConverterRule {
  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchFilterRule.class);

  public static final ElasticsearchFilterRule INSTANCE = new ElasticsearchFilterRule();

  private final Convention out;

  private ElasticsearchFilterRule() {
    super(Filter.class, Convention.NONE, ElasticsearchRel.CONVENTION,
        "DrillElasticsearchFilterRule");
    this.out = ElasticsearchRel.CONVENTION;
  }

  @Override
  public RelNode convert(RelNode relNode) {
    Filter filter = (Filter) relNode;
    NodeTypeFinder filterFinder = new NodeTypeFinder(ElasticsearchFilter.class);
    filter.getInput().accept(filterFinder);
    if (filterFinder.containsNode) {
      return null;
    }
    RelTraitSet traitSet = filter.getTraitSet().replace(out);

    try {
      CalciteUtils.analyzePredicate(filter.getCondition());
    } catch (Exception e) {
      logger.info("Unable to push filter into ElasticSearch :{}", e.getMessage(), e);
      return null;
    }

    return CalciteUtils.createFilter(traitSet,
        convert(filter.getInput(), out), filter.getCondition());
  }

}
