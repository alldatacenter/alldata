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
package org.apache.drill.exec.store.http;

import okhttp3.HttpUrl;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.Pair;
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
import org.apache.drill.exec.store.http.HttpPaginatorConfig.PaginatorMethod;
import org.apache.drill.exec.store.http.util.SimpleHttp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The HTTP storage plugin accepts filters which are:
 * <ul>
 * <li>A single column = value expression, where the column is
 * a filter column from the config, or</li>
 * <li>An AND'ed set of such expressions,</li>
 * <li>If the value is one with an unambiguous conversion to
 * a string. (That is, not dates, binary, maps, etc.)</li>
 * <li>  A field from the URL.  For instance in some APIs you have parameters that are part of the path.
 * For example, https://github.com/orgs/{org}/repos.  In this instance, the query must contain
 * a parameter called org.
 * </li>
 * </ul>
 */
public class HttpPushDownListener implements FilterPushDownListener {

  public static Set<StoragePluginOptimizerRule> rulesFor(
      OptimizerRulesContext optimizerRulesContext) {
    return FilterPushDownStrategy.rulesFor(
        new HttpPushDownListener());
  }

  @Override
  public String prefix() {
    return "Http";
  }

  @Override
  public boolean isTargetScan(GroupScan groupScan) {
    return groupScan instanceof HttpGroupScan;
  }

  @Override
  public ScanPushDownListener builderFor(GroupScan groupScan) {
    HttpGroupScan httpScan = (HttpGroupScan) groupScan;
    if (httpScan.hasFilters() || !httpScan.allowsFilters()) {
      return null;
    } else {
      return new HttpScanPushDownListener(httpScan);
    }
  }

  private static class HttpScanPushDownListener implements ScanPushDownListener {

    private final HttpGroupScan groupScan;
    private final Map<String, String> filterParams = CaseInsensitiveMap.newHashMap();

    HttpScanPushDownListener(HttpGroupScan groupScan) {
      this.groupScan = groupScan;

      // Add fields from config
      if (groupScan.getHttpConfig().params() != null) {
        for (String field : groupScan.getHttpConfig().params()) {
          filterParams.put(field, field);
        }
      }

      // Add fields from paginator, if present
      HttpPaginatorConfig paginator = groupScan.getHttpConfig().paginator();
      if (paginator != null) {
        if (paginator.getMethodType() == PaginatorMethod.OFFSET) {
          filterParams.put(paginator.limitParam(), paginator.limitParam());
          filterParams.put(paginator.offsetParam(), paginator.offsetParam());
        } else if (paginator.getMethodType() == PaginatorMethod.PAGE) {
          filterParams.put(paginator.pageParam(), paginator.pageParam());
          filterParams.put(paginator.pageSizeParam(), paginator.pageSizeParam());
        }
      }

      // Add fields from the URL path as denoted by {}
      HttpUrl url = HttpUrl.parse(groupScan.getHttpConfig().url());
      if (url != null) {
        List<String> urlParams = SimpleHttp.getURLParameters(url);
        for (String urlField : urlParams) {
          filterParams.put(urlField, urlField);
        }
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

    /**
     * Only accept equality conditions.
     */
    private ColRelOpConstNode acceptRelOp(ColRelOpConstNode relOp) {
      switch (relOp.op) {
      case EQ:
        return acceptColumn(relOp.colName) &&
               acceptType(relOp.value.type) ? relOp : null;
      default:
        return null;
      }
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
     * Convert the equality nodes to a map of param/string pairs using
     * the case specified in the storage plugin config.
     */
    @Override
    public Pair<GroupScan, List<RexNode>> transform(AndNode andNode) {
      Map<String, String> filters;
      if (groupScan.getHttpConfig().caseSensitiveFilters()) {
        filters = new HashMap<>();
      } else {
        filters = CaseInsensitiveMap.newHashMap();
      }

      double selectivity = 1;
      for (ExprNode expr : andNode.children) {
        ColRelOpConstNode relOp = (ColRelOpConstNode) expr;
        filters.put(filterParams.get(relOp.colName), relOp.value.value.toString());
        selectivity *= relOp.op.selectivity();
      }
      HttpGroupScan newScan = new HttpGroupScan(groupScan, filters, selectivity);
      return Pair.of(newScan, Collections.emptyList());
    }
  }
}
