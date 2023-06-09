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
package org.apache.drill.exec.planner.common;

import static org.apache.drill.exec.planner.logical.DrillOptiq.isLiteralNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.core.Values;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.util.NlsString;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.util.GuavaUtils;
import org.apache.drill.exec.vector.complex.fn.ExtendedJsonOutput;
import org.apache.drill.exec.vector.complex.fn.JsonOutput;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Base class for logical and physical Values implemented in Drill.
 */
public abstract class DrillValuesRelBase extends Values implements DrillRelNode {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  protected final JSONOptions content;

  public DrillValuesRelBase(RelOptCluster cluster, RelDataType rowType, List<? extends List<RexLiteral>> tuples, RelTraitSet traits) {
    this(cluster, rowType, tuples, traits, convertToJsonOptions(rowType, tuples));
  }

  /**
   * This constructor helps to avoid unnecessary tuples parsing into json options
   * during copying or logical to physical values conversion.
   */
  public DrillValuesRelBase(RelOptCluster cluster,
                            RelDataType rowType,
                            List<? extends List<RexLiteral>> tuples,
                            RelTraitSet traits,
                            JSONOptions content) {
    super(cluster, rowType, GuavaUtils.convertToNestedUnshadedImmutableList(tuples), traits);
    this.content = content;
  }

  /**
   * @return values content represented as json
   */
  public JSONOptions getContent() {
    return content;
  }

  /**
   * Converts tuples into json representation taking into account row type.
   * Example: [['A']] -> [{"EXPR$0":"A"}], [[1]] -> [{"EXPR$0":{"$numberLong":1}}]
   *
   * @param rowType row type
   * @param tuples list of constant values in a row-expression
   * @return json representation of tuples
   */
  private static JSONOptions convertToJsonOptions(RelDataType rowType, List<? extends List<RexLiteral>> tuples) {
    try {
      return new JSONOptions(convertToJsonNode(rowType, tuples), JsonLocation.NA);
    } catch (IOException e) {
      throw new DrillRuntimeException("Failure while attempting to encode Values in JSON.", e);
    }
  }

  private static JsonNode convertToJsonNode(RelDataType rowType, List<? extends List<RexLiteral>> tuples) throws IOException {
    TokenBuffer out = new TokenBuffer(MAPPER.getFactory().getCodec(), false);
    JsonOutput json = new ExtendedJsonOutput(out);
    json.writeStartArray();
    String[] fields = rowType.getFieldNames().toArray(new String[rowType.getFieldCount()]);

    for (List<RexLiteral> row : tuples) {
      json.writeStartObject();
      int i = 0;
      for (RexLiteral field : row) {
        json.writeFieldName(fields[i]);
        writeLiteral(field, json);
        i++;
      }
      json.writeEndObject();
    }
    json.writeEndArray();
    json.flush();
    return out.asParser().readValueAsTree();
  }

  private static void writeLiteral(RexLiteral literal, JsonOutput out) throws IOException {
    switch (literal.getType().getSqlTypeName()) {
      case BIGINT:
        if (isLiteralNull(literal)) {
          out.writeBigIntNull();
        } else {
          out.writeBigInt((((BigDecimal) literal.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP)).longValue());
        }
        return;

      case BOOLEAN:
        if (isLiteralNull(literal)) {
          out.writeBooleanNull();
        } else {
          out.writeBoolean((Boolean) literal.getValue());
        }
        return;

      case CHAR:
        if (isLiteralNull(literal)) {
          out.writeVarcharNull();
        } else {
          // Since Calcite treats string literals as fixed char and adds trailing spaces to the strings to make them the
          // same length, here we do an rtrim() to get the string without the trailing spaces. If we don't rtrim, the comparison
          // with Drill's varchar column values would not return a match.
          // TODO: However, note that if the user had explicitly added spaces in the string literals then even those would get
          // trimmed, so this exposes another issue that needs to be resolved.
          out.writeVarChar(((NlsString) literal.getValue()).rtrim().getValue());
        }
        return;

      case DOUBLE:
        if (isLiteralNull(literal)) {
          out.writeDoubleNull();
        } else {
          out.writeDouble(((BigDecimal) literal.getValue()).doubleValue());
        }
        return;

      case FLOAT:
        if (isLiteralNull(literal)) {
          out.writeFloatNull();
        } else {
          out.writeFloat(((BigDecimal) literal.getValue()).floatValue());
        }
        return;

      case INTEGER:
        if (isLiteralNull(literal)) {
          out.writeIntNull();
        } else {
          out.writeInt((((BigDecimal) literal.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP)).intValue());
        }
        return;

      case DECIMAL:
        // Converting exact decimal into double since values in the list may have different scales
        // so the resulting scale wouldn't be calculated correctly
        if (isLiteralNull(literal)) {
          out.writeDoubleNull();
        } else {
          out.writeDouble(((BigDecimal) literal.getValue()).doubleValue());
        }
        return;

      case VARCHAR:
        if (isLiteralNull(literal)) {
          out.writeVarcharNull();
        } else {
          out.writeVarChar(((NlsString) literal.getValue()).getValue());
        }
        return;

      case SYMBOL:
        if (isLiteralNull(literal)) {
          out.writeVarcharNull();
        } else {
          out.writeVarChar(literal.getValue().toString());
        }
        return;

      case DATE:
        if (isLiteralNull(literal)) {
          out.writeDateNull();
        } else {
          out.writeDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(new DateTime(literal.getValue()).getMillis()), ZoneOffset.UTC).toLocalDate());
        }
        return;

      case TIME:
        if (isLiteralNull(literal)) {
          out.writeTimeNull();
        } else {
          out.writeTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(new DateTime(literal.getValue()).getMillis()), ZoneOffset.UTC).toLocalTime());
        }
        return;

      case TIMESTAMP:
        if (isLiteralNull(literal)) {
          out.writeTimestampNull();
        } else {
          out.writeTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(new DateTime(literal.getValue()).getMillis()), ZoneOffset.UTC));
        }
        return;

      case INTERVAL_YEAR:
      case INTERVAL_YEAR_MONTH:
      case INTERVAL_MONTH:
        if (isLiteralNull(literal)) {
          out.writeIntervalNull();
        } else {
          int months = ((BigDecimal) (literal.getValue())).intValue();
          out.writeInterval(new Period().plusMonths(months));
        }
        return;

      case INTERVAL_DAY:
      case INTERVAL_DAY_HOUR:
      case INTERVAL_DAY_MINUTE:
      case INTERVAL_DAY_SECOND:
      case INTERVAL_HOUR:
      case INTERVAL_HOUR_MINUTE:
      case INTERVAL_HOUR_SECOND:
      case INTERVAL_MINUTE:
      case INTERVAL_MINUTE_SECOND:
      case INTERVAL_SECOND:
        if (isLiteralNull(literal)) {
          out.writeIntervalNull();
        } else {
          long millis = ((BigDecimal) (literal.getValue())).longValue();
          int days = (int) (millis / DateTimeConstants.MILLIS_PER_DAY);
          millis = millis - (days * DateTimeConstants.MILLIS_PER_DAY);
          out.writeInterval(new Period().plusDays(days).plusMillis((int) millis));
        }
        return;

      case NULL:
        out.writeUntypedNull();
        return;

      case ANY:
      default:
        throw new UnsupportedOperationException(
            String.format("Unable to convert the value of %s and type %s to a Drill constant expression.",
                literal, literal.getType().getSqlTypeName()));
    }
  }

}
