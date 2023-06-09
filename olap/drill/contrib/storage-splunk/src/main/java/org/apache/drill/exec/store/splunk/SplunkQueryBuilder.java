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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.store.base.filter.ExprNode;
import org.apache.drill.exec.store.base.filter.RelOp;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import java.util.Map;

public class SplunkQueryBuilder {
  public final static String EQUAL_OPERATOR = "=";
  public final static String NOT_EQUAL_OPERATOR = "!=";
  public final static String GREATER_THAN = ">";
  public final static String GREATER_THAN_EQ = ">=";
  public final static String LESS_THAN = "<";
  public final static String LESS_THAN_EQ = "<=";

  private String query;
  private String sourceTypes;
  private String concatenatedFields;
  private String filters;
  private int sourcetypeCount;
  private int limit;

  public SplunkQueryBuilder (String index) {
    this.filters = "";
    sourcetypeCount = 0;
    query = "search index=" + index;
  }

  /**
   * Adds a sourcetype to the Splunk query.  Splunk indexes its data by indexes, then within the index, organizes
   * the data by sourcetype, which could be a reference to the underlying source system.  For instance, sourcetype
   * might be csv files, log files, Azure storage or whatever.  Since this is a sort of special metadata case,
   * it is better to apply this separately than a regular filter.  Sourcetypes can accept wildcards, but cannot accept
   * any other operator other than = or !=.
   * @param sourceType The Splunk Sourcetype to be added to the Splunk query.
   */
  public void addSourceType(String sourceType) {
    if (this.sourceTypes == null) {
      this.sourceTypes = " sourcetype=\"" + sourceType + "\"";
    } else if (! sourceTypes.contains("sourcetype=\"" + sourceType + "\"")) {
      this.sourceTypes += " OR sourcetype=\"" + sourceType + "\"";
    }
    sourcetypeCount++;
  }

  /**
   * Adds a field name to a Splunk query.  To push down the projection into Splunk,
   * Splunk accepts arguments in the format | fields foo, bar, car.  This function adds these fields to the query.
   * As an error preventative measure, this function will ignore ** from Drill.
   * @param field The field to be added to the query
   * @return true if the field was added, false if it was skipped
   */
  public boolean addField(String field) {
    // Double Star fields cause errors and we will not add to the field list
    if (SchemaPath.DYNAMIC_STAR.equals(field) || SplunkUtils.SPECIAL_FIELDS.includes(field)) {
      return false;
    }

    // Case for first field
    if (concatenatedFields == null) {
      this.concatenatedFields = field;
    } else {
      this.concatenatedFields += "," + field;
    }
    return true;
  }

  /**
   * Adds a row limit to the query. Ignores anything <= zero.
   * This method should only be called once, but if is called more than once,
   * it will set the limit to the most recent value.
   * @param limit Positive, non-zero integer of number of desired rows.
   */
  public void addLimit(int limit) {
    if (limit > 0) {
      this.limit = limit;
    }
  }

  /**
   * Adds a filter to the Splunk query.  Splunk treats all filters as
   * AND filters, without explicitly noting that.  The operator should be the actual operator
   * @param left The field to be filtered
   * @param right The value of that field
   * @param operator The actual operator to go in the SPL query
   */
  public void addFilter(String left, String right, String operator) {
    filters = filters + " " + left + operator + quoteString(right);
  }

  /**
   * Adds an isnotnull() filter to the Splunk query
   * @param fieldName The field name which should be null
   */
  public void addNotNullFilter(String fieldName) {
    filters = filters + " isnotnull(" + fieldName + ") ";
  }

  /**
   * Adds an isnull() filter to the Splunk query
   * @param fieldName The field name which should be null
   */
  public void addNullFilter(String fieldName) {
    filters = filters + " isnull(" + fieldName + ") ";
  }

  /**
   * Processes the filters for a Splunk query
   * @param filters A HashMap of filters
   */
  public void addFilters(Map<String, ExprNode.ColRelOpConstNode> filters) {
    if (filters == null) {
      return;
    }
    for ( Map.Entry filter : filters.entrySet()) {
      String fieldName = ((ExprNode.ColRelOpConstNode)filter.getValue()).colName;
      RelOp operation = ((ExprNode.ColRelOpConstNode)filter.getValue()).op;
      String value = ((ExprNode.ColRelOpConstNode)filter.getValue()).value.value.toString();

      // Ignore special cases
      if (SplunkUtils.SPECIAL_FIELDS.includes(fieldName)) {
        // Sourcetypes are a special case and can be added via filter pushdown
        if (fieldName.equalsIgnoreCase("sourcetype")) {
          addSourceType(value);
        }
        continue;
      }

      switch (operation) {
        case EQ:
          // Sourcetypes are a special case and can be added via filter pushdown
          if (fieldName.equalsIgnoreCase("sourcetype")) {
            addSourceType(value);
          } else {
            addFilter(fieldName, value, EQUAL_OPERATOR);
          }
          break;
        case NE:
          addFilter(fieldName, value, NOT_EQUAL_OPERATOR);
          break;
        case GE:
          addFilter(fieldName, value, GREATER_THAN_EQ);
          break;
        case GT:
          addFilter(fieldName, value, GREATER_THAN);
          break;
        case LE:
          addFilter(fieldName, value, LESS_THAN_EQ);
          break;
        case LT:
          addFilter(fieldName, value, LESS_THAN);
          break;
        case IS_NULL:
          addNullFilter(fieldName);
          break;
        case IS_NOT_NULL:
          addNotNullFilter(fieldName);
          break;
      }
    }
  }

  /**
   * Adds quotes around text for use in SPL queries. Ignores numbers
   * @param word The input word to be quoted.
   * @return The text with quotes
   */
  public String quoteString(String word) {

    if (! isNumeric(word)) {
      return "\"" + word + "\"";
    } else {
      return word;
    }
  }

  public String build() {
    // Add the sourcetype
    if (! Strings.isNullOrEmpty(sourceTypes)) {

      // Add parens
      if (sourcetypeCount > 1) {
        sourceTypes = " (" + sourceTypes.trim() + ")";
      }
      query += sourceTypes;
    }

    // Add filters
    if (! Strings.isNullOrEmpty(filters)) {
      query += filters;
    }

    // Add fields
    if (! Strings.isNullOrEmpty(concatenatedFields)) {
      query += " | fields " + concatenatedFields;
    }

    // Add limit
    if (this.limit > 0) {
      query += " | head " + this.limit;
    }

    // Add table logic. This tells Splunk to return the data in tabular form rather than the mess that it usually generates
    if ( Strings.isNullOrEmpty(concatenatedFields)) {
      concatenatedFields = "*";
    }
    query += " | table " + concatenatedFields;

    return query;
  }

  /**
   * Returns true if the given string is numeric, false if not
   * @param str The string to test for numeric
   * @return True if the string is numeric, false if not.
   */
  public static boolean isNumeric(String str)
  {
    return str.matches("-?\\d+(\\.\\d+)?");
  }
}
