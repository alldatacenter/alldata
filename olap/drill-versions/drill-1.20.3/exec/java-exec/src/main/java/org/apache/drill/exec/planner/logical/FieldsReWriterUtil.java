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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.SchemaPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FieldsReWriterUtil {

  /**
   * Checks if operator call is using item star field.
   * Will return field name if true. null otherwise.
   *
   * @param rexCall operator call
   * @param fieldNames list of field names
   * @return field name, null otherwise
   */
  public static String getFieldNameFromItemStarField(RexCall rexCall, List<String> fieldNames) {
    if (!SqlStdOperatorTable.ITEM.equals(rexCall.getOperator())) {
      return null;
    }

    if (rexCall.getOperands().size() != 2) {
      return null;
    }

    if (!(rexCall.getOperands().get(0) instanceof RexInputRef && rexCall.getOperands().get(1) instanceof RexLiteral)) {
      return null;
    }

    // get parent field reference from the first operand (ITEM($0, 'col_name' -> $0)
    // and check if it corresponds to the dynamic star
    RexInputRef rexInputRef = (RexInputRef) rexCall.getOperands().get(0);
    String parentFieldName = fieldNames.get(rexInputRef.getIndex());
    if (!SchemaPath.DYNAMIC_STAR.equals(parentFieldName)) {
      return null;
    }

    // get field name from the second operand (ITEM($0, 'col_name') -> col_name)
    RexLiteral rexLiteral = (RexLiteral) rexCall.getOperands().get(1);
    if (SqlTypeName.CHAR.equals(rexLiteral.getType().getSqlTypeName())) {
      return RexLiteral.stringValue(rexLiteral);
    }

    return null;
  }

  /**
   * Holder class to store field information (name and type) with the list of nodes this field is used in.
   * Primary used to hold information about new field during field re-write process.
   */
  public static class DesiredField {
    private final String name;
    private final RelDataType type;
    private final List<RexNode> nodes = new ArrayList<>();

    public DesiredField(String name, RelDataType type, RexNode node) {
      this.name = name;
      this.type = type;
      addNode(node);
    }

    public void addNode(RexNode originalNode) {
      nodes.add(originalNode);
    }

    public String getName() {
      return name;
    }

    public RelDataType getType() {
      return type;
    }

    public List<RexNode> getNodes() {
      return nodes;
    }
  }

  /**
   * Replaces original node with provided in mapper, otherwise returns original node.
   */
  public static class FieldsReWriter extends RexShuttle {

    private final Map<RexNode, Integer> mapper;

    public FieldsReWriter(Map<RexNode, Integer> mapper) {
      this.mapper = mapper;
    }

    @Override
    public RexNode visitCall(final RexCall call) {
      Integer index = mapper.get(call);
      if (index != null) {
        return new RexInputRef(index, call.getType());
      }
      return super.visitCall(call);
    }

    @Override
    public RexNode visitInputRef(RexInputRef inputRef) {
      Integer index = mapper.get(inputRef);
      if (index != null) {
        return new RexInputRef(index, inputRef.getType());
      }
      return super.visitInputRef(inputRef);
    }

  }


}
