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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.server.options.OptionValue.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * This {@link OptionManager} implements some the basic methods and should be
 * extended by concrete implementations.
 */

public abstract class BaseOptionManager implements OptionManager {
  private static final Logger logger = LoggerFactory.getLogger(BaseOptionManager.class);

  /**
   * Gets the current option value given a validator.
   *
   * @param validator the validator
   * @return option value
   * @throws IllegalArgumentException - if the validator is not found
   */
  private OptionValue getOptionSafe(OptionValidator validator) {
    final String optionName = validator.getOptionName();
    OptionValue value = getOption(optionName);
    return value == null ? getDefault(optionName) : value;
  }

  @Override
  public boolean getOption(TypeValidators.BooleanValidator validator) {
    return getOptionSafe(validator).bool_val;
  }

  @Override
  public double getOption(TypeValidators.DoubleValidator validator) {
    return getOptionSafe(validator).float_val;
  }

  @Override
  public long getOption(TypeValidators.LongValidator validator) {
    return getOptionSafe(validator).num_val;
  }

  @Override
  public String getOption(TypeValidators.StringValidator validator) {
    return getOptionSafe(validator).string_val;
  }

  @Override
  public boolean getBoolean(String name) {
    return getByType(name, Kind.BOOLEAN).bool_val;
  }

  @Override
  public int getInt(String name) {
    return (int) getLong(name);
  }

  @Override
  public long getLong(String name) {
    return getByType(name, Kind.LONG).num_val;
  }

  @Override
  public double getDouble(String name) {
    return getByType(name, Kind.DOUBLE).float_val;
  }

  @Override
  public String getString(String name) {
    return getByType(name, Kind.STRING).string_val;
  }

  private OptionValue getByType(String name, Kind dataType) {
    OptionValue value = getOption(name);
    if (value == null) {
      throw UserException.systemError(null)
        .addContext("Undefined option: " + name)
        .build(logger);
    }
    if (value.kind != dataType) {
      throw UserException.systemError(null)
          .addContext("Option " + name + " is of data type " +
              value.kind + " but was requested as " + dataType)
          .build(logger);
    }
    return value;
  }

  @Override
  public OptionList getInternalOptionList() {
    return getAllOptionList(true);
  }

  @Override
  public OptionList getPublicOptionList() {
    return getAllOptionList(false);
  }

  @Override
  public void setLocalOption(final String name, final boolean value) {
    setLocalOption(name, Boolean.valueOf(value));
  }

  @Override
  public void setLocalOption(final String name, final long value) {
    setLocalOption(name, Long.valueOf(value));
  }

  @Override
  public void setLocalOption(final String name, final double value) {
    setLocalOption(name, Double.valueOf(value));
  }

  @Override
  public void setLocalOption(final String name, final String value) {
    setLocalOption(name, (Object) value);
  }

  @Override
  public void setLocalOption(final String name, final Object value) {
    final OptionDefinition definition = getOptionDefinition(name);
    final OptionValidator validator = definition.getValidator();
    final OptionMetaData metaData = definition.getMetaData();
    final OptionValue.AccessibleScopes type = definition.getMetaData().getAccessibleScopes();
    final OptionValue.OptionScope scope = getScope();
    checkOptionPermissions(name, type, scope);
    final OptionValue optionValue = OptionValue.create(type, name, value, scope, validator.getKind());
    validator.validate(optionValue, metaData, this);
    setLocalOptionHelper(optionValue);
  }

  @Override
  public void setLocalOption(final OptionValue.Kind kind, final String name, final String valueStr) {
    setLocalOption(name, valueStr);
  }

  private static void checkOptionPermissions(String name, OptionValue.AccessibleScopes type, OptionValue.OptionScope scope) {
    if (!type.inScopeOf(scope)) {
      throw UserException.permissionError()
        .message(String.format("Cannot change option %s in scope %s", name, scope))
        .build(logger);
    }
  }

  abstract protected void setLocalOptionHelper(OptionValue optionValue);

  abstract protected OptionValue.OptionScope getScope();

  private OptionList getAllOptionList(boolean internal)
  {
    Iterator<OptionValue> values = this.iterator();
    OptionList optionList = new OptionList();

    while (values.hasNext()) {
      OptionValue value = values.next();

      if (getOptionDefinition(value.getName()).getMetaData().isInternal() == internal) {
        optionList.add(value);
      }
    }

    return optionList;
  }
}
