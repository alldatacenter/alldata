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
package org.apache.drill.exec.server.options;

import java.util.Iterator;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;

/**
 * An {@link OptionManager} which allows for falling back onto another
 * {@link OptionManager} when retrieving options.
 * <p/>
 * {@link FragmentOptionManager} and {@link SessionOptionManager} use
 * {@link SystemOptionManager} as the fall back manager.
 * {@link QueryOptionManager} uses {@link SessionOptionManager} as the fall back
 * manager.
 */
public abstract class FallbackOptionManager extends BaseOptionManager {

  protected final OptionManager fallback;

  public FallbackOptionManager(OptionManager fallback) {
    /**
     * TODO(DRILL-2097): Add a Preconditions.checkNotNull(fallback, "A fallback manager must be provided.") and remove
     * the null check in {@link #getOption(String)}. This is not added currently only to avoid modifying the long list
     * of test files.
     */
    this.fallback = fallback;
  }

  @Override
  public Iterator<OptionValue> iterator() {
    return Iterables.concat(fallback, getLocalOptions()).iterator();
  }

  @Override
  public OptionValue getOption(final String name) {
    final OptionValue value = getLocalOption(name);
    if (value == null && fallback != null) {
      return fallback.getOption(name);
    } else {
      return value;
    }
  }

  /**
   * Gets the option values managed by this manager as an iterable.
   *
   * @return iterable of option values
   */
  abstract Iterable<OptionValue> getLocalOptions();

  /**
   * Gets the option value from this manager without falling back.
   *
   * @param name the option name
   * @return the option value, or null if the option does not exist locally
   */
  abstract OptionValue getLocalOption(String name);

  @Override
  public OptionDefinition getOptionDefinition(String name) {
    return fallback.getOptionDefinition(name);
  }

  @Override
  public OptionList getOptionList() {
    final OptionList list = new OptionList();
    for (final OptionValue value : getLocalOptions()) {
      list.add(value);
    }
    return list;
  }
}
