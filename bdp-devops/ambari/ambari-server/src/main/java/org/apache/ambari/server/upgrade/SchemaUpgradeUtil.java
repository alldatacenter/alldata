/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.upgrade;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class SchemaUpgradeUtil {

  private SchemaUpgradeUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Extracts a new property that was introduced to make a content type property more configurable by making a hard coded value
   * changeable. The function takes a pattern to identify the previous value of the property, and replaces the actual value with
   * a reference to the new property, and also adds the new property to a map.
   * 
   * Example:
   * content: ... some.variable=HARDCODED_VALUE ...
   * propertyName: some_variable
   * variableName: some_variable_processed
   * propertyPattern: some.variable=(\w)
   * 
   * the function returns: ... some.variable={{some_variable_processed}}
   * newProperties will contain: some_variable -> HARDCODED_VALUE
   * 
   * @param content the content type variable's content before upgrade
   * @param propertyName the name of the new property
   * @param variableName the name of the variable that will be used in the content
   * @param propertyPattern the regexp pattern to identify the property, must contain exactly one "(\w+)" for the actual value of
   *                        the new property
   * @param defaultValue the default value for the new property if the actual value can not be found in the content
   * @param newProperties map of new properties where the extracted property will be put
   * @return the updated content containing a reference to the new property
   */
  public static String extractProperty(String content, String propertyName, String variableName, String propertyPattern,
      String defaultValue, Map<String, String> newProperties) {
    if (StringUtils.countOccurrencesOf(propertyPattern, "(\\w+)") != 1) {
      throw new IllegalArgumentException("propertyPattern must contain exactly one '(\\w+)': " + propertyPattern);
    }
    
    Pattern p = Pattern.compile(propertyPattern);
    Matcher m = p.matcher(content);
    
    String propertyValue = defaultValue;
    if (m.find()) {
      propertyValue = m.group(1);
      
      String unescapedPattern = propertyPattern.replace("\\{", "{");
      String toReplace = unescapedPattern.replace("(\\w+)", propertyValue);
      String replaceWith = unescapedPattern.replace("(\\w+)", "{{" + variableName + "}}");
      content = content.replace(toReplace, replaceWith);
    }
    newProperties.put(propertyName, propertyValue);
    return content;
  }
}
