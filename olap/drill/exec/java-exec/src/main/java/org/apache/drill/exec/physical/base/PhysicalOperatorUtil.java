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
package org.apache.drill.exec.physical.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.record.VectorAccessible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalOperatorUtil {
  private static final Logger logger = LoggerFactory.getLogger(PhysicalOperatorUtil.class);

  private PhysicalOperatorUtil() { }

  public static Set<Class<? extends PhysicalOperator>> getSubTypes(ScanResult classpathScan) {
    final Set<Class<? extends PhysicalOperator>> ops = classpathScan.getImplementations(PhysicalOperator.class);
    logger.debug("Found {} physical operator classes: {}.", ops.size(),
                 ops);
    return ops;
  }

  /**
   * Helper method to create a list of {@code MinorFragmentEndpoint} instances from a
   * given endpoint assignment list.
   *
   * @param endpoints
   *          Assigned endpoint list. Index of each endpoint in list indicates
   *          the MinorFragmentId of the fragment that is assigned to the
   *          endpoint.
   * @return a list of (minor fragment id, endpoint) pairs in which the
   * minor fragment ID is reified as a member. Items are indexed by minor fragment
   * ID.
   */
  public static List<MinorFragmentEndpoint> getIndexOrderedEndpoints(List<DrillbitEndpoint> endpoints) {
    List<MinorFragmentEndpoint> destinations = new ArrayList<>();
    int minorFragmentId = 0;
    for (DrillbitEndpoint endpoint : endpoints) {
      destinations.add(new MinorFragmentEndpoint(minorFragmentId, endpoint));
      minorFragmentId++;
    }

    return destinations;
  }

  /**
   * Helper method to materialize the given logical expression using the
   * {@code ExpressionTreeMaterializer}.
   * @param expr Logical expression to materialize
   * @param incoming Incoming record batch
   * @param context Fragment context
   */
  public static LogicalExpression materializeExpression(LogicalExpression expr,
      VectorAccessible incoming, FragmentContext context)  {
    ErrorCollector collector = new ErrorCollectorImpl();
    LogicalExpression mle = ExpressionTreeMaterializer.materialize(expr, incoming, collector,
            context.getFunctionRegistry());
    collector.reportErrors(logger);
    return mle;
  }
}
