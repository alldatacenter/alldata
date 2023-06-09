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
package org.apache.drill.exec.planner.sql.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.parser.SqlParserUtil;

/**
 * Helper methods or constants used in parsing a SQL query.
 */
public class DrillParserUtil {

  private static final int CONDITION_LIST_CAPACITY = 3;

  /**
   * System-depended end of line character
   */
  public static final String EOL = System.lineSeparator();

  public static SqlNode createCondition(SqlNode left, SqlOperator op, SqlNode right) {

    // if one of the operands is null, return the other
    if (left == null) {
      return right;
    } else if (right == null) {
      return left;
    }

    List<Object> listCondition = new ArrayList<>(CONDITION_LIST_CAPACITY);
    listCondition.add(left);
    listCondition.add(new SqlParserUtil.ToTreeListItem(op, SqlParserPos.ZERO));
    listCondition.add(right);

    return SqlParserUtil.toTree(listCondition);
  }

}
