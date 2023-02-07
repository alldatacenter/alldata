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
package org.apache.drill.exec.store.splunk;

import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.base.filter.ExprNode;
import org.apache.drill.exec.store.base.filter.ExprNode.AndNode;
import org.apache.drill.exec.store.base.filter.ExprNode.ColRelOpConstNode;
import org.apache.drill.exec.store.base.filter.ExprNode.OrNode;
import org.apache.drill.exec.store.base.filter.FilterPushDownListener;
import org.apache.drill.exec.store.base.filter.FilterPushDownStrategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Splunk storage plugin accepts filters which are:
 * <ul>
 * <li>A single column = value expression </li>
 * <li>An AND'ed set of such expressions,</li>
 * <li>If the value is one with an unambiguous conversion to
 * a string. (That is, not dates, binary, maps, etc.)</li>
 * </ul>
 */
public class SplunkPushDownListener implements FilterPushDownListener {

  public static Set<StoragePluginOptimizerRule> rulesFor(OptimizerRulesContext optimizerRulesContext) {
    return FilterPushDownStrategy.rulesFor(new SplunkPushDownListener());
  }

  @Override
  public String prefix() {
    return "Splunk";
  }

  @Override
  public boolean isTargetScan(GroupScan groupScan) {
    return groupScan instanceof SplunkGroupScan;
  }

  @Override
  public ScanPushDownListener builderFor(GroupScan groupScan) {
    SplunkGroupScan splunkScan = (SplunkGroupScan) groupScan;
    if (splunkScan.hasFilters() || !splunkScan.allowsFilters()) {
      return null;
    } else {
      return new SplunkScanPushDownListener(splunkScan);
    }
  }

  private static class SplunkScanPushDownListener implements ScanPushDownListener {

    private final SplunkGroupScan groupScan;
    private final Map<String, String> filterParams = CaseInsensitiveMap.newHashMap();

    SplunkScanPushDownListener(SplunkGroupScan groupScan) {
      this.groupScan = groupScan;
      for (SchemaPath field : groupScan.columns()) {
        filterParams.put(field.getAsUnescapedPath(), field.getAsUnescapedPath());
      }
    }

    @Override
    public ExprNode accept(ExprNode node) {
      if (node instanceof OrNode) {
        return null;
      } else if (node instanceof ColRelOpConstNode) {
        return acceptRelOp((ColRelOpConstNode) node);
      } else {
        return null;
      }
    }

    private ColRelOpConstNode acceptRelOp(ColRelOpConstNode relOp) {
      return acceptColumn(relOp.colName) && acceptType(relOp.value.type) ? relOp : null;
    }

    /**
     * Only accept columns in the filter params list.
     */
    private boolean acceptColumn(String colName) {
      return filterParams.containsKey(colName);
    }

    /**
     * Only accept types which have an unambiguous mapping to
     * String.
     */
    private boolean acceptType(MinorType type) {
      switch (type) {
        case BIGINT:
        case BIT:
        case FLOAT4:
        case FLOAT8:
        case INT:
        case SMALLINT:
        case VARCHAR:
        case VARDECIMAL:
          return true;
        default:
          return false;
      }
    }

    /**
     * Convert the nodes to a map of param/string pairs using
     * the case specified in the storage plugin config.
     */
    @Override
    public Pair<GroupScan, List<RexNode>> transform(AndNode andNode) {
      Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
      double selectivity = 1;
      for (ExprNode expr : andNode.children) {
        ColRelOpConstNode relOp = (ColRelOpConstNode) expr;
        filters.put(filterParams.get(relOp.colName), relOp);
        selectivity *= relOp.op.selectivity();
      }
      SplunkGroupScan newScan = new SplunkGroupScan(groupScan, filters, selectivity);
      return Pair.of(newScan, Collections.emptyList());
    }
  }
}
