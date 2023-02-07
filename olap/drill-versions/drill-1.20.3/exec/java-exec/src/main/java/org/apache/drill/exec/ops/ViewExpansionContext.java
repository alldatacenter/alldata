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
package org.apache.drill.exec.ops;

import static org.apache.drill.exec.ExecConstants.IMPERSONATION_MAX_CHAINED_USER_HOPS;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.SchemaConfig.SchemaConfigInfoProvider;

import com.carrotsearch.hppc.ObjectIntHashMap;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Contains context information about view expansion(s) in a query. Part of {@link org.apache.drill.exec.ops
 * .QueryContext}. Before expanding a view into its definition, as part of the
 * {@link org.apache.drill.exec.planner.logical.DrillViewTable#toRel(org.apache.calcite.plan.RelOptTable.ToRelContext,
 * org.apache.calcite.plan.RelOptTable)}, first a {@link ViewExpansionToken} is requested from ViewExpansionContext
 * through {@link #reserveViewExpansionToken(String)}.
 * Once view expansion is complete, a token is released through {@link ViewExpansionToken#release()}. A view definition
 * itself may contain zero or more views for expanding those nested views also a token is obtained.
 *
 * Ex:
 *   Following are the available view tables: { "view_1", "view_2", "view_3", "view_4" }. Corresponding owners are
 *   {"view1Owner", "view2Owner", "view3Owner", "view4Owner"}.
 *   Definition of "view4" : "SELECT field4 FROM view3"
 *   Definition of "view3" : "SELECT field4, field3 FROM view2"
 *   Definition of "view2" : "SELECT field4, field3, field2 FROM view1"
 *   Definition of "view1" : "SELECT field4, field3, field2, field1 FROM someTable"
 *
 *   Query is: "SELECT * FROM view4".
 *   Steps:
 *     1. "view4" comes for expanding it into its definition
 *     2. A token "view4Token" is requested through {@link #reserveViewExpansionToken(String view4Owner)}
 *     3. "view4" is called for expansion. As part of it
 *       3.1 "view3" comes for expansion
 *       3.2 A token "view3Token" is requested through {@link #reserveViewExpansionToken(String view3Owner)}
 *       3.3 "view3" is called for expansion. As part of it
 *           3.3.1 "view2" comes for expansion
 *           3.3.2 A token "view2Token" is requested through {@link #reserveViewExpansionToken(String view2Owner)}
 *           3.3.3 "view2" is called for expansion. As part of it
 *                 3.3.3.1 "view1" comes for expansion
 *                 3.3.3.2 A token "view1Token" is requested through {@link #reserveViewExpansionToken(String view1Owner)}
 *                 3.3.3.3 "view1" is called for expansion
 *                 3.3.3.4 "view1" expansion is complete
 *                 3.3.3.5 Token "view1Token" is released
 *           3.3.4 "view2" expansion is complete
 *           3.3.5 Token "view2Token" is released
 *       3.4 "view3" expansion is complete
 *       3.5 Token "view3Token" is released
 *    4. "view4" expansion is complete
 *    5. Token "view4Token" is released.
 *
 */
public class ViewExpansionContext {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ViewExpansionContext.class);

  private final SchemaConfigInfoProvider schemaConfigInfoProvider;
  private final int maxChainedUserHops;
  private final String queryUser;
  private final ObjectIntHashMap<String> userTokens = new ObjectIntHashMap<>();
  private final boolean impersonationEnabled;

  public ViewExpansionContext(QueryContext queryContext) {
    this(queryContext.getConfig(), queryContext);
  }

  public ViewExpansionContext(DrillConfig config, SchemaConfigInfoProvider schemaConfigInfoProvider) {
    this.schemaConfigInfoProvider = schemaConfigInfoProvider;
    this.maxChainedUserHops = config.getInt(IMPERSONATION_MAX_CHAINED_USER_HOPS);
    this.queryUser = schemaConfigInfoProvider.getQueryUserName();
    this.impersonationEnabled = config.getBoolean(ExecConstants.IMPERSONATION_ENABLED);
  }

  public boolean isImpersonationEnabled() {
    return impersonationEnabled;
  }

  /**
   * Reserve a token for expansion of view owned by given user name. If it can't issue any more tokens,
   * throws {@link UserException}.
   *
   * @param viewOwner Name of the user who owns the view.
   * @return An instance of {@link org.apache.drill.exec.ops.ViewExpansionContext.ViewExpansionToken} which must be
   *         released when done using the token.
   */
  public ViewExpansionToken reserveViewExpansionToken(String viewOwner) {
    int totalTokens = 1;
    if (!viewOwner.equals(queryUser)) {
      // We want to track the tokens only if the "viewOwner" is not same as the "queryUser".
      if (userTokens.containsKey(viewOwner)) {
        // If the user already exists, we don't need to validate the limit on maximum user hops in chained impersonation
        // as the limit is for number of unique users.
        totalTokens += userTokens.get(viewOwner);
      } else {
        // Make sure we are not exceeding the limit of maximum number impersonation user hops in chained impersonation.
        if (userTokens.size() == maxChainedUserHops) {
          final String errMsg =
              String.format("Cannot issue token for view expansion as issuing the token exceeds the " +
                  "maximum allowed number of user hops (%d) in chained impersonation.", maxChainedUserHops);
          logger.error(errMsg);
          throw UserException.permissionError().message(errMsg).build(logger);
        }
      }

      userTokens.put(viewOwner, totalTokens);

      logger.debug("Issued view expansion token for user '{}'", viewOwner);
    }

    return new ViewExpansionToken(viewOwner);
  }

  private void releaseViewExpansionToken(ViewExpansionToken token) {
    final String viewOwner = token.viewOwner;

    if (viewOwner.equals(queryUser)) {
      // If the token owner and queryUser are same, no need to track the token release.
      return;
    }

    Preconditions.checkState(userTokens.containsKey(token.viewOwner),
        "Given user doesn't exist in User Token store. Make sure token for this user is obtained first.");

    final int userTokenCount = userTokens.get(viewOwner);
    if (userTokenCount == 1) {
      // Remove the user from collection, when there are no more tokens issued to the user.
      userTokens.remove(viewOwner);
    } else {
      userTokens.put(viewOwner, userTokenCount - 1);
    }
    logger.debug("Released view expansion token issued for user '{}'", viewOwner);
  }

  /**
   * Represents token issued to a view owner for expanding the view.
   */
  public class ViewExpansionToken {
    private final String viewOwner;

    private boolean released;

    ViewExpansionToken(String viewOwner) {
      this.viewOwner = viewOwner;
    }

    /**
     * Get schema tree for view owner who owns this token.
     * @return Root of schema tree.
     */
    public SchemaPlus getSchemaTree() {
      Preconditions.checkState(!released, "Trying to use released token.");
      return schemaConfigInfoProvider.getRootSchema(viewOwner);
    }

    /**
     * Release the token. Once released all method calls (except release) cause {@link java.lang.IllegalStateException}.
     */
    public void release() {
      if (!released) {
        released = true;
        releaseViewExpansionToken(this);
      }
    }
  }
}
