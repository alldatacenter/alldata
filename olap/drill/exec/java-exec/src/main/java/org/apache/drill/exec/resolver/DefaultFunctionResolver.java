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
package org.apache.drill.exec.resolver;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.util.AssertionUtil;

public class DefaultFunctionResolver implements FunctionResolver {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultFunctionResolver.class);

  @Override
  public DrillFuncHolder getBestMatch(List<DrillFuncHolder> methods, FunctionCall call) {

    int bestcost = Integer.MAX_VALUE;
    int currcost = Integer.MAX_VALUE;
    DrillFuncHolder bestmatch = null;
    final List<DrillFuncHolder> bestMatchAlternatives = new LinkedList<>();
    List<TypeProtos.MajorType> argumentTypes = call.args().stream()
            .map(LogicalExpression::getMajorType)
            .collect(Collectors.toList());
    for (DrillFuncHolder h : methods) {
      currcost = TypeCastRules.getCost(argumentTypes, h);

      // if cost is lower than 0, func implementation is not matched, either w/ or w/o implicit casts
      if (currcost  < 0 ) {
        continue;
      }

      if (currcost < bestcost) {
        bestcost = currcost;
        bestmatch = h;
        bestMatchAlternatives.clear();
      } else if (currcost == bestcost) {
        // keep log of different function implementations that have the same best cost
        bestMatchAlternatives.add(h);
      }
    }

    if (bestcost < 0) {
      //did not find a matched func implementation, either w/ or w/o implicit casts
      //TODO: raise exception here?
      return null;
    } else {
      if (AssertionUtil.isAssertionsEnabled() && bestMatchAlternatives.size() > 0) {
        /*
         * There are other alternatives to the best match function which could have been selected
         * Log the possible functions and the chose implementation and raise an exception
         */
        logger.error("Chosen function impl: " + bestmatch.toString());

        // printing the possible matches
        logger.error("Printing all the possible functions that could have matched: ");
        for (DrillFuncHolder holder: bestMatchAlternatives) {
          logger.error(holder.toString());
        }

        throw new AssertionError("Multiple functions with best cost found");
      }
      return bestmatch;
    }
  }
}
