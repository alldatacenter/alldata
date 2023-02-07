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

package org.apache.drill.exec.store.httpd;

public class HttpdUtils {

  public static final String PARSER_WILDCARD = ".*";
  public static final String SAFE_WILDCARD = "_$";
  public static final String SAFE_SEPARATOR = "_";

  /**
   * Drill cannot deal with fields with dots in them like request.referer. For the sake of simplicity we are going
   * ensure the field name is cleansed. The resultant output field will look like: request_referer.<br>
   * Additionally, wild cards will get replaced with _$
   *
   * @param parserFieldName name to be cleansed.
   * @return The field name formatted for Drill
   */
  public static String drillFormattedFieldName(String parserFieldName) {
    if (parserFieldName.contains(":")) {
      String[] fieldPart = parserFieldName.split(":");
      return fieldPart[1].replaceAll("_", "__").replace(PARSER_WILDCARD, SAFE_WILDCARD).replaceAll("\\.", SAFE_SEPARATOR);
    } else {
      return parserFieldName.replaceAll("_", "__").replace(PARSER_WILDCARD, SAFE_WILDCARD).replaceAll("\\.", SAFE_SEPARATOR);
    }
  }

  /**
   * Returns true if the field is a wildcard AKA map field, false if not.
   * @param fieldName The target field name
   * @return True if the field is a wildcard, false if not
   */
  public static boolean isWildcard(String fieldName) {
    return fieldName.endsWith(PARSER_WILDCARD);
  }

  /**
   * The HTTPD parser formats fields using the format HTTP.URI:request.firstline.uri.query.
   * For maps, we only want the last part of this, so this function returns the last bit of the
   * field name.
   * @param mapField The unformatted field name
   * @return The last part of the field name
   */
  public static String getFieldNameFromMap(String mapField) {
    return mapField.substring(mapField.lastIndexOf('.') + 1);
  }

}
