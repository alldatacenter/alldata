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

import java.math.BigDecimal;

import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.NlsString;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.parser.DrillSqlSetOption;
import org.apache.drill.exec.server.options.OptionDefinition;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSetOption;

/**
 * Converts a {@link SqlNode} representing: "ALTER .. SET option = value" or "ALTER ... SET option"
 * statement to a {@link PhysicalPlan}. See {@link DrillSqlSetOption}
 * <p>
 * These statements have side effects i.e. the options within the system context or the session context are modified.
 * The resulting {@link DirectPlan} returns to the client a string that is the name of the option that was updated
 * or a value of the property
 */
public class SetOptionHandler extends AbstractSqlSetHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SetOptionHandler.class);

  public SetOptionHandler(QueryContext context) {
    super(context);
  }

  /**
   * Handles {@link DrillSqlSetOption} query
   */
  @Override
  public final PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    // sqlNode could contain DrillSqlResetOption or DrillSqlSetOption, depends on parsed statement
    SqlSetOption statement = unwrap(sqlNode, SqlSetOption.class);
    OptionScope optionScope = getScope(statement, context.getOptions());
    OptionManager optionManager = context.getOptions().getOptionManager(optionScope);

    String optionName = statement.getName().toString();
    SqlNode optionValue = statement.getValue();

    if (optionValue == null) {
      // OptionManager.getOptionDefinition() call ensures that the specified option name is valid
      OptionDefinition optionDefinition = optionManager.getOptionDefinition(optionName);
      String value = String.valueOf(optionManager.getOption(optionName).getValue());

      // obtains option name from OptionDefinition to use the name as defined in the option, rather than what the user provided
      return DirectPlan.createDirectPlan(context, new SetOptionViewResult(optionDefinition.getValidator().getOptionName(), value));
    } else {
      if (optionScope == OptionValue.OptionScope.SYSTEM) {
        checkAdminPrivileges(context.getOptions());
      }
      if (!(optionValue instanceof SqlLiteral)) {
        throw UserException.validationError()
            .message("Drill does not support assigning non-literal values in SET statements.")
            .build(logger);
      }
      optionManager.setLocalOption(optionName, sqlLiteralToObject((SqlLiteral) optionValue));

      return DirectPlan.createDirectPlan(context, true, String.format("%s updated.", optionName));
    }
  }

  private static Object sqlLiteralToObject(SqlLiteral literal) {
    final Object object = literal.getValue();
    final SqlTypeName typeName = literal.getTypeName();
    switch (typeName) {
    case DECIMAL: {
      final BigDecimal bigDecimal = (BigDecimal) object;
      if (bigDecimal.scale() == 0) {
        return bigDecimal.longValue();
      } else {
        return bigDecimal.doubleValue();
      }
    }

    case DOUBLE:
    case FLOAT:
      return ((BigDecimal) object).doubleValue();

    case SMALLINT:
    case TINYINT:
    case BIGINT:
    case INTEGER:
      return ((BigDecimal) object).longValue();

    case VARBINARY:
    case VARCHAR:
    case CHAR:
      return ((NlsString) object).getValue();

    case BOOLEAN:
      return object;

    default:
      throw UserException.validationError()
        .message("Drill doesn't support assigning literals of type %s in SET statements.", typeName)
        .build(logger);
    }
  }

  /**
   * Representation of "SET property.name" query result.
   */
  public static class SetOptionViewResult {
    public String name;
    public String value;

    SetOptionViewResult(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }
}
