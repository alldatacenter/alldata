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
package org.apache.drill.exec.store.sys;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValidator.OptionDescription;
import org.apache.drill.exec.server.options.OptionValue.AccessibleScopes;
import org.apache.drill.exec.server.options.OptionValue.Kind;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.store.pojo.NonNullable;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Extends the original Option iterator. The idea is to hide the implementation details and present the
 * user with the rows which have values set at the top level of hierarchy and exclude the values set
 * at lower levels. This is done by examining the scope and the precedence order of scope is session - system - default.
 * All the values are represented as String instead of having multiple
 * columns and the data type is provided as kind to preserve type information about the option.
 * The query output is as follows  -
 *               name,kind,type,val,optionScope
 *        planner.slice_target,BIGINT,SESSION,20,SESSION
 *        planner.width.max_per_node,BIGINT,SYSTEM,0,BOOT
 *        planner.affinity_factor,FLOAT,SYSTEM,1.7,SYSTEM
 *  In the above example, the query output contains single row for each option
 *  and we can infer that slice target is set at the session level and max width
 *  per node is set at the BOOT level and affinity factor is set at the SYSTEM level.
 *  The options set in the top level of hierarchy always takes precedence and they are returned
 *  in the query output. For example if the option is set at both SESSION level and
 *  SYSTEM level the value set at SESSION level takes precedence and query output has
 *  only the value set at SESSION level.
 */
public class ExtendedOptionIterator implements Iterator<Object> {

  private final OptionManager fragmentOptions;
  private final Iterator<OptionValue> mergedOptions;
  private final Map<OptionValue.Kind, String> typeMapping;
  private static final int SHORT_DESCRIP_MAX_SIZE = 110;

  public ExtendedOptionIterator(FragmentContext context, boolean internal) {
    fragmentOptions = context.getOptions();

    typeMapping = new HashMap<>();
    typeMapping.put(Kind.STRING, "VARCHAR");
    typeMapping.put(Kind.DOUBLE, "FLOAT");
    typeMapping.put(Kind.LONG, "BIGINT");
    typeMapping.put(Kind.BOOLEAN, "BIT");

    if (!internal) {
      mergedOptions = sortOptions(fragmentOptions.getPublicOptionList().iterator());
    } else {
      mergedOptions = sortOptions(fragmentOptions.getInternalOptionList().iterator());
    }
  }
   /* *
    * Remove the redundant rows for the same option based on the scope and return
    * the value that takes precedence over others. For example the option set in session
    * scope takes precedence over system and boot and etc.,
    */
  public Iterator<OptionValue> sortOptions(Iterator<OptionValue> options) {
    List<OptionValue> optionslist = Lists.newArrayList(options);
    HashMap<String, OptionValue> optionsmap = new HashMap<>();

    for (OptionValue option : optionslist) {
      if (option.scope == OptionScope.QUERY) {
        // Option set on query level should be ignored here as its value should not be shown to user
        continue;
      }

      if (optionsmap.containsKey(option.getName())) {

        if (option.scope.compareTo(optionsmap.get(option.getName()).scope) > 0) {
          optionsmap.put(option.getName(), option);
        }

      } else {
        optionsmap.put(option.getName(), option);
      }
    }
    optionslist.clear();
    for (String name : optionsmap.keySet()) {
      optionslist.add(optionsmap.get(name));
    }

    optionslist.sort(Comparator.comparing(OptionValue::getName));

    return optionslist.iterator();
  }

  @Override
  public boolean hasNext() {
    return mergedOptions.hasNext();
  }

  @Override
  public ExtendedOptionValueWrapper next() {
    final OptionValue value = mergedOptions.next();

    final Status status;
    if (value.accessibleScopes == AccessibleScopes.BOOT) {
      status = Status.BOOT;
    } else {
      final OptionValue def = fragmentOptions.getDefault(value.name);
      status = (value.equalsIgnoreType(def) ? Status.DEFAULT : Status.CHANGED);
    }

    return new ExtendedOptionValueWrapper(value.name, typeMapping.get(value.kind), value.accessibleScopes,value.getValue().toString(), status, value.scope,
        getShortDescription(value.name));
  }

  public enum Status {
    BOOT, DEFAULT, CHANGED
  }

  /**
   * Wrapper class for Extended Option Value
   */
  public static class ExtendedOptionValueWrapper {
    @NonNullable
    public final String name;
    @NonNullable
    public final String kind;
    @NonNullable
    public final OptionValue.AccessibleScopes accessibleScopes;
    public final String val;
    public final Status status;
    @NonNullable
    public final OptionScope optionScope;
    public final String description;

    public ExtendedOptionValueWrapper(final String name, final String kind, final OptionValue.AccessibleScopes type, final String value, final Status status, final OptionScope scope,
        final String description) {
      this.name = name;
      this.kind = kind;
      this.accessibleScopes = type;
      this.val = value;
      this.status = status;
      this.optionScope = scope;
      this.description = description;
    }
  }

  //Extract a limited length from the original description if not available
  private String getShortDescription(String name) {
    OptionDescription optionDescription = fragmentOptions.getOptionDefinition(name).getValidator().getOptionDescription();
    if (optionDescription == null) {
      return "";
    }
    String description;
    if (optionDescription.hasShortDescription()) {
      description = optionDescription.getShortDescription();
    } else {
      description = optionDescription.getDescription();
      if (description.length() > SHORT_DESCRIP_MAX_SIZE) {
        return description.substring(0, SHORT_DESCRIP_MAX_SIZE-3).concat("..."); //Append ellipsis (trailing dots)
      }
    }
    return description;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}


