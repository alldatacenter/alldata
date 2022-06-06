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

package org.apache.ambari.server.state.kerberos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Helper class to provide variable replacement services
 */
@Singleton
public class VariableReplacementHelper {

  private static final Logger LOG = LoggerFactory.getLogger(VariableReplacementHelper.class);

  /**
   * a regular expression Pattern used to find "variable" placeholders in strings
   */
  private static final Pattern PATTERN_VARIABLE = Pattern.compile("\\$\\{(?:([\\w\\-\\.]+)/)?([\\w\\-\\./]+)(?:\\s*\\|\\s*(.+?))?\\}");

  /**
   * a regular expression Pattern used to parse "function" declarations: name(arg1, arg2, ...)
   */
  private static final Pattern PATTERN_FUNCTION = Pattern.compile("(\\w+)\\((.*?)\\)");

  /**
   * A map of "registered" functions
   */
  private static final Map<String, Function> FUNCTIONS = new HashMap<String, Function>() {
    {
      put("each", new EachFunction());
      put("toLower", new ToLowerFunction());
      put("replace", new ReplaceValue());
      put("append", new AppendFunction());
      put("principalPrimary", new PrincipalPrimary());
      put("stripPort", new StripPort());
    }
  };

  /**
   * Performs variable replacement on the supplied String value using values from the replacementsMap.
   * <p/>
   * The value is a String containing one or more "variables" in the form of ${variable_name}, such
   * that "variable_name" may indicate a group identifier; else "" is used as the group.
   * For example:
   * <p/>
   * variable_name:  group: ""; property: "variable_name"
   * group1/variable_name:  group: "group1"; property: "variable_name"
   * root/group1/variable_name:  Not Supported
   * <p/>
   * The replacementsMap is a Map of Maps creating a (small) hierarchy of data to traverse in order
   * to resolve the variable.
   * <p/>
   * If a variable resolves to one or more variables, that new variable(s) will be processed and replaced.
   * If variable exists after a set number of iterations it is assumed that a cycle has been created
   * and the process will abort returning a String in a possibly unexpected state.
   *
   * @param value           a String containing zero or more variables to be replaced
   * @param replacementsMap a Map of data used to perform the variable replacements
   * @return a new String
   */
  public String replaceVariables(String value, Map<String, Map<String, String>> replacementsMap) throws AmbariException {
    if ((value != null) && (replacementsMap != null) && !replacementsMap.isEmpty()) {
      int count = 0; // Used to help prevent an infinite loop...
      boolean replacementPerformed;

      do {
        if (++count > 1000) {
          throw new AmbariException(String.format("Circular reference found while replacing variables in %s", value));
        }

        Matcher matcher = PATTERN_VARIABLE.matcher(value);
        StringBuffer sb = new StringBuffer();

        replacementPerformed = false;

        while (matcher.find()) {
          String type = matcher.group(1);
          String name = matcher.group(2);
          String function = matcher.group(3);

          Map<String, String> replacements;

          if ((name != null) && !name.isEmpty()) {
            if (type == null) {
              replacements = replacementsMap.get("");
            } else {
              replacements = replacementsMap.get(type);
            }

            if (replacements != null) {
              String replacement = replacements.get(name);

              if (replacement != null) {
                if (function != null) {
                  replacement = applyReplacementFunction(function, replacement, replacementsMap);
                }

                // Escape '$' and '\' so they don't cause any issues.
                matcher.appendReplacement(sb, replacement.replace("\\", "\\\\").replace("$", "\\$"));
                replacementPerformed = true;
              }
            }
          }
        }

        matcher.appendTail(sb);
        value = sb.toString();
      }
      while (replacementPerformed); // Process the string again to make sure new variables were not introduced
    }

    return value;
  }

  /**
   * Applies the specified replacement function to the supplied data.
   * <p/>
   * The function must be in the following format:
   * <code>
   * name(arg1,arg2,arg3...)
   * </code>
   * <p/>
   * Commas in arguments should be escaped with a '/'.
   *
   * @param function        the name and arguments of the function
   * @param replacement     the data to use in the function
   * @param replacementsMap a Map of data used to perform variable replacements, if needed
   * @return a new string generated by applying the function
   */
  private String applyReplacementFunction(String function, String replacement, Map<String, Map<String, String>> replacementsMap) {
    if (function != null) {
      Matcher matcher = PATTERN_FUNCTION.matcher(function);

      if (matcher.matches()) {
        String name = matcher.group(1);

        if (name != null) {
          Function f = FUNCTIONS.get(name);

          if (f != null) {
            String args = matcher.group(2);
            String[] argsList = args.split("(?<!\\\\),");

            // Remove escape character from '\,'
            for (int i = 0; i < argsList.length; i++) {
              argsList[i] = argsList[i].trim().replace("\\,", ",");
            }

            return f.perform(argsList, replacement, replacementsMap);
          }
        }
      }
    }

    return replacement;
  }

  /**
   * Function is the interface to be implemented by replacement functions.
   */
  private interface Function {
    /**
     * Perform the function to generate a new string by applying the logic of this function to the
     * supplied data.
     *
     * @param args            an array of arguments, specific to the function
     * @param data            the data to apply the function logic to
     * @param replacementsMap a Map of data used to perform variable replacements, if needed
     * @return the resulting string
     */
    String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap);
  }

  /**
   * EachFunction is a Function implementation that iterates over a list of values pulled from a
   * delimited string to yield a new string.
   * <p/>
   * This function expects the following arguments (in order) within the args array:
   * <ol>
   * <li>pattern to use for each item, see {@link String#format(String, Object...)}</li>
   * <li>delimiter to use when concatenating the resolved pattern per item</li>
   * <li>regular expression used to split the original value</li>
   * </ol>
   */
  private static class EachFunction implements Function {
    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if ((args == null) || (args.length != 3)) {
        throw new IllegalArgumentException("Invalid number of arguments encountered");
      }

      if (data != null) {
        StringBuilder builder = new StringBuilder();

        String pattern = args[0];
        String concatDelimiter = args[1];
        String dataDelimiter = args[2];

        String[] items = data.split(dataDelimiter);

        for (String item : items) {
          if (builder.length() > 0) {
            builder.append(concatDelimiter);
          }

          builder.append(String.format(pattern, item));
        }

        return builder.toString();
      }

      return "";
    }
  }

  /**
   * ReplaceValue is a Function implementation that replaces the value in the string
   * <p/>
   * This function expects the following arguments (in order) within the args array:
   * <ol>
   * <li>regular expression that should be replaced</li>
   * <li>replacement value for the string</li>
   * </ol>
   */
  private static class ReplaceValue implements Function {

    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if ((args == null) || (args.length != 2)) {
        throw new IllegalArgumentException("Invalid number of arguments encountered");
      }
      if (data != null) {
        StringBuffer builder = new StringBuffer();
        String regex = args[0];
        String replacement = args[1];
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
          matcher.appendReplacement(builder, replacement);
        }
        matcher.appendTail(builder);
        return builder.toString();
      }
      return "";
    }
  }

  /**
   * ToLowerFunction is a Function implementation that converts a String to lowercase
   */
  private static class ToLowerFunction implements Function {
    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if (data != null) {
        return data.toLowerCase();
      }

      return "";
    }
  }

  /**
   * AppendFunction is a Function implementation that appends the current value to the value of some
   * configuration property using a specified delimited value.
   * <p/>
   * This function expects the following arguments (in order) within the args array:
   * <ol>
   * <li>configuration specification to use to find the value to append</li>
   * <li>delimiter to use when concatenating the value</li>
   * <li>boolean value used to determine of the value should be appended only it is unique (true) or unconditionally appended (false)</li>
   * </ol>
   */
  private static class AppendFunction implements Function {
    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if ((args == null) || (args.length != 3)) {
        String message = "Invalid number of arguments encountered while processing the 'append' variable replacement function.  The following arguments are expected:" +
            "\n\t- Configuration specification used to get the initial value" +
            "\n\t- Delimiter used for parsing the initial value and appending new values" +
            "\n\t- A flag to indicate whether values should be unique ('true') or not ('false')";

        LOG.error(message);
        throw new IllegalArgumentException(message);
      }

      String configurationSpec = args[0];
      String concatDelimiter = args[1];
      boolean uniqueOnly = Boolean.parseBoolean(args[2]);
      String sourceData = getSourceData(replacementsMap, configurationSpec);

      Collection<String> sourceItems = parseItems(sourceData, concatDelimiter);
      Collection<String> dataItems = parseItems(data, concatDelimiter);
      Collection<String> items = new ArrayList<>();

      if (uniqueOnly) {
        for (String item : sourceItems) {
          if (!items.contains(item)) {
            items.add(item);
          }
        }

        for (String item : dataItems) {
          if (!items.contains(item)) {
            items.add(item);
          }
        }
      } else {
        items.addAll(sourceItems);
        items.addAll(dataItems);
      }

      return StringUtils.join(items, concatDelimiter);
    }

    /**
     * Parse the string using the specified delimiter to create a collection of parsed (and trimmed) Strings.
     *
     * @param delimitedString the String to parse
     * @param concatDelimiter the delimiter used to split the String
     * @return a Collection of Strings split from the original string
     */
    private Collection<String> parseItems(String delimitedString, String concatDelimiter) {
      Collection<String> items = new ArrayList<>();

      if (!StringUtils.isEmpty(delimitedString)) {
        for (String item : delimitedString.split(concatDelimiter)) {
          item = item.trim();
          if (!item.isEmpty()) {
            items.add(item);
          }
        }
      }

      return items;
    }

    /**
     * Retrieves the source data given a configuration specification and a Map of configurations, grouped by configuration types.
     * <p>
     * The configuration specification is expected to be in one of the following forms:
     * <ul>
     * <li>config-type/property-name</li>
     * <li>property-name</li>
     * </ul>
     * <p>
     * The replacementsMap is expected to be a Map of config-types to their name/value pairs.
     *
     * @param replacementsMap   a Map of data used to perform variable replacements, if needed
     * @param configurationSpec a configuration specification declaring the config-type (optional) and the relevant
     *                          property name
     * @return the found value
     */
    private String getSourceData(Map<String, Map<String, String>> replacementsMap, String configurationSpec) {
      String sourceData = null;
      if ((replacementsMap != null) && !replacementsMap.isEmpty() && !StringUtils.isEmpty(configurationSpec)) {
        // Parse the configuration specification to get the config-type and property-name.
        // If only one "part" is found when splitting the String, assume that is it the property name
        // where the config-type will be assumed to be "", which contains properties not set in service
        // configurations
        String[] parts = configurationSpec.split("/");
        String type = null;
        String name = null;

        if (parts.length == 2) {
          type = parts[0];
          name = parts[1];
        } else if (parts.length == 1) {
          name = parts[0];
        }

        if (!StringUtils.isEmpty(name)) {
          Map<String, String> replacements;
          if (type == null) {
            replacements = replacementsMap.get("");
          } else {
            replacements = replacementsMap.get(type);
          }

          if (replacements != null) {
            sourceData = replacements.get(name);
          }
        }
      }

      return sourceData;
    }
  }

  /**
   * Get the primary part of a Kerberos principal.
   * The format of a typical Kerberos principal is primary/instance@REALM.
   */
  private static class PrincipalPrimary implements Function {
    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if (data == null) {
        return null;
      }
      if (data.contains("/")) {
        return data.split("/")[0];
      } else if (data.contains("@")) {
        return data.split("@")[0];
      } else {
        return data;
      }
    }
  }

  /**
   * Strips out the port (if any) from a URL assuming the following input data layout
   * <code>host[:port]</code>
   */
  private static class StripPort implements Function {
    @Override
    public String perform(String[] args, String data, Map<String, Map<String, String>> replacementsMap) {
      if (data == null) {
        return null;
      }
      final int semicolonIndex = data.indexOf(":");
      return semicolonIndex == -1 ? data : data.substring(0, semicolonIndex);
    }
  }
}
