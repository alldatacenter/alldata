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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.server.options.DrillConfigIterator;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValue.AccessibleScopes;
import org.apache.drill.exec.server.options.OptionValue.Kind;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.store.pojo.NonNullable;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class OptionIterator implements Iterator<Object> {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OptionIterator.class);

  enum Mode {
    BOOT,
    // These represent System/Session options that are public. These are available for everyone to change and are considered safe to change.
    SYS_SESS_PUBLIC,
    // These represent System/Session options that are internal. These are segregated from the public options and are not visible in the same tables and rest endpoints as public
    // options. These are technically available for anyone to change, but users are discouraged from doing so unless they absolutely know what they are doing. The general purpose
    // for internal options is to enable support to easily debug.
    SYS_SESS_INTERNAL
  };

  private final OptionManager fragmentOptions;
  private final Iterator<OptionValue> mergedOptions;

  public OptionIterator(FragmentContext context, Mode mode){
    final DrillConfigIterator configOptions = new DrillConfigIterator(context.getConfig());
    fragmentOptions = context.getOptions();
    final Iterator<OptionValue> optionList;
    switch(mode){
    case BOOT:
      optionList = configOptions.iterator();
      break;
    case SYS_SESS_PUBLIC:
      optionList = fragmentOptions.getPublicOptionList().iterator();
      break;
    case SYS_SESS_INTERNAL:
      optionList = fragmentOptions.getInternalOptionList().iterator();
      break;
    default:
      optionList = Iterators.concat(configOptions.iterator(), fragmentOptions.iterator());
    }

    List<OptionValue> values = Lists.newArrayList(optionList);
    Collections.sort(values);
    mergedOptions = values.iterator();

  }

  @Override
  public boolean hasNext() {
    return mergedOptions.hasNext();
  }

  @Override
  public OptionValueWrapper next() {
    final OptionValue value = mergedOptions.next();
    final Status status;

    if (value.accessibleScopes == AccessibleScopes.BOOT) {
      status = Status.BOOT;
    } else {
      final OptionValue def = fragmentOptions.getDefault(value.name);
      if (value.equalsIgnoreType(def)) {
        status = Status.DEFAULT;
        } else {
        status = Status.CHANGED;
        }
      }
    return new OptionValueWrapper(value.name, value.kind, value.accessibleScopes, value.scope, value.num_val, value.string_val,
        value.bool_val, value.float_val, status);
  }

  public enum Status {
    BOOT, DEFAULT, CHANGED
  }

  /**
   * Wrapper class for OptionValue to add Status
   */
  public static class OptionValueWrapper {

    @NonNullable
    public final String name;
    @NonNullable
    public final Kind kind;
    @NonNullable
    public final AccessibleScopes accessibleScopes;
    @NonNullable
    public final OptionScope optionScope;
    @NonNullable
    public final Status status;
    public final Long num_val;
    public final String string_val;
    public final Boolean bool_val;
    public final Double float_val;

    public OptionValueWrapper(final String name, final Kind kind, final OptionValue.AccessibleScopes type, final OptionScope scope, final Long num_val,
                              final String string_val, final Boolean bool_val, final Double float_val,
                              final Status status) {
      this.name = name;
      this.kind = kind;
      this.accessibleScopes = type;
      this.optionScope = scope;
      this.num_val = num_val;
      this.string_val = string_val;
      this.bool_val = bool_val;
      this.float_val = float_val;
      this.status = status;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
