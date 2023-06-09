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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.calcite.rel.RelNode;
import java.util.List;

public class InsertLocalExchangeVisitor extends BasePrelVisitor<Prel, Void, RuntimeException> {
  private final OptionManager options;

  private static boolean isMuxEnabled(OptionManager options) {
    return (options.getOption(PlannerSettings.MUX_EXCHANGE.getOptionName()).bool_val ||
            options.getOption(PlannerSettings.DEMUX_EXCHANGE.getOptionName()).bool_val ||
            options.getOption(PlannerSettings.ORDERED_MUX_EXCHANGE.getOptionName()).bool_val);
  }

  public static Prel insertLocalExchanges(Prel prel, OptionManager options) {

    if (isMuxEnabled(options)) {
      return prel.accept(new InsertLocalExchangeVisitor(options), null);
    }

    return prel;
  }

  public InsertLocalExchangeVisitor(OptionManager options) {
    this.options = options;
  }

  @Override
  public Prel visitExchange(ExchangePrel prel, Void value) throws RuntimeException {
    Prel child = ((Prel)prel.getInput()).accept(this, null);
    return prel.constructMuxPrel(child, options);
  }

  @Override
  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    for(Prel child : prel){
      children.add(child.accept(this, null));
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }
}
