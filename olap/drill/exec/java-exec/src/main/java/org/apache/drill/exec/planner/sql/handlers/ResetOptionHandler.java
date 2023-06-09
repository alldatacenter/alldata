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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.DrillSqlResetOption;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.server.options.QueryOptionManager;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSetOption;

/**
 * Converts a {@link SqlNode} representing: "ALTER .. RESET option | ALL" statement to a {@link PhysicalPlan}.
 * See {@link DrillSqlResetOption}.
 * <p>
 * These statements have side effects i.e. the options within the system context or the session context are modified.
 * The resulting {@link DirectPlan} returns to the client a string that is the name of the option that was updated
 * or a value of the property
 */
public class ResetOptionHandler extends AbstractSqlSetHandler {

  /**
   * Class constructor.
   * @param context Context of the Query
   */
  public ResetOptionHandler(QueryContext context) {
    super(context);
  }

  /**
   * Handles {@link DrillSqlResetOption} query
   */
  @Override
  public final PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    QueryOptionManager options = context.getOptions();
    SqlSetOption statement = unwrap(sqlNode, SqlSetOption.class);
    OptionScope optionScope = getScope(statement, context.getOptions());

    if (optionScope == OptionValue.OptionScope.SYSTEM) {
      checkAdminPrivileges(options);
    }

    OptionManager optionManager = options.getOptionManager(optionScope);
    String optionName = statement.getName().toString();

    if ("ALL".equalsIgnoreCase(optionName)) {
      optionManager.deleteAllLocalOptions();
    } else {
      optionManager.deleteLocalOption(optionName);
    }
    return DirectPlan.createDirectPlan(context, true, String.format("%s updated.", optionName));
  }
}
