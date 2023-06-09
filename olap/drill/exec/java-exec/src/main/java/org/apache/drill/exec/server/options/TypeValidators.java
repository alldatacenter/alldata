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

import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionValue.Kind;
import org.apache.drill.exec.util.ImpersonationUtil;

public class TypeValidators {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TypeValidators.class);
  public static class NonNegativeLongValidator extends LongValidator {
    private final long max;

    public NonNegativeLongValidator(String name, long max, OptionDescription description) {
      super(name, description);
      this.max = max;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (v.num_val > max || v.num_val < 0) {
        throw UserException.validationError()
            .message(String.format("Option %s must be between %d and %d.", getOptionName(), 0, max))
            .build(logger);
      }
    }
  }

  public static class PositiveLongValidator extends LongValidator {
    protected final long max;

    public PositiveLongValidator(String name, long max, OptionDescription description) {
      super(name, description);
      this.max = max;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (v.num_val > max || v.num_val < 1) {
        throw UserException.validationError()
            .message(String.format("Option %s must be between %d and %d.", getOptionName(), 1, max))
            .build(logger);
      }
    }
  }

  public static class PowerOfTwoLongValidator extends PositiveLongValidator {

    public PowerOfTwoLongValidator(String name, long max, OptionDescription description) {
      super(name, max, description);
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (!isPowerOfTwo(v.num_val)) {
        throw UserException.validationError()
            .message(String.format("Option %s must be a power of two.", getOptionName()))
            .build(logger);
      }
    }

    private static boolean isPowerOfTwo(long num) {
      return (num & (num - 1)) == 0;
    }
  }

  public static class RangeDoubleValidator extends DoubleValidator {
    protected final double min;
    protected final double max;

    public RangeDoubleValidator(String name, double min, double max, OptionDescription description) {
      super(name, description);
      this.min = min;
      this.max = max;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (v.float_val > max || v.float_val < min) {
        throw UserException.validationError()
            .message(String.format("Option %s must be between %f and %f.", getOptionName(), min, max))
            .build(logger);
      }
    }
  }

  public static class MinRangeDoubleValidator extends RangeDoubleValidator {
    private final String maxValidatorName;

    public MinRangeDoubleValidator(String name, double min, double max, String maxValidatorName, OptionDescription description) {
      super(name, min, max, description);
      this.maxValidatorName = maxValidatorName;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      OptionValue maxValue = manager.getOption(maxValidatorName);
      if (v.float_val > maxValue.float_val) {
        throw UserException.validationError()
                .message(String.format("Option %s must be less than or equal to Option %s",
                        getOptionName(), maxValidatorName))
                .build(logger);
      }
    }
  }

  public static class MaxRangeDoubleValidator extends RangeDoubleValidator {
    private final String minValidatorName;

    public MaxRangeDoubleValidator(String name, double min, double max, String minValidatorName, OptionDescription description) {
      super(name, min, max, description);
      this.minValidatorName = minValidatorName;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      OptionValue minValue = manager.getOption(minValidatorName);
      if (v.float_val < minValue.float_val) {
        throw UserException.validationError()
                .message(String.format("Option %s must be greater than or equal to Option %s",
                        getOptionName(), minValidatorName))
                .build(logger);
      }
    }
  }

  public static class BooleanValidator extends TypeValidator {
    public BooleanValidator(String name, OptionDescription description) {
      super(name, Kind.BOOLEAN, description);
    }
  }

  public static class StringValidator extends TypeValidator {
    public StringValidator(String name, OptionDescription description) {
      super(name, Kind.STRING, description);
    }
  }

  public static class LongValidator extends TypeValidator {
    public LongValidator(String name, OptionDescription description) {
      super(name, Kind.LONG, description);
    }
  }

  public static class DoubleValidator extends TypeValidator {
    public DoubleValidator(String name, OptionDescription description) {
      super(name, Kind.DOUBLE, description);
    }
  }

  public static class IntegerValidator extends LongValidator {
    public IntegerValidator(String name, OptionDescription description) {
      super(name, description);
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (v.num_val > Integer.MAX_VALUE || v.num_val < Integer.MIN_VALUE) {
        throw UserException.validationError()
          .message(String.format("Option %s does not have a valid integer value", getOptionName()))
          .build(logger);
      }
    }
  }

  public static class RangeLongValidator extends LongValidator {
    private final long min;
    private final long max;

    public RangeLongValidator(String name, long min, long max, OptionDescription description) {
      super(name, description);
      this.min = min;
      this.max = max;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (v.num_val > max || v.num_val < min) {
        throw UserException.validationError()
            .message(String.format("Option %s must be between %d and %d.", getOptionName(), min, max))
            .build(logger);
      }
    }
  }

  /**
   * Validator that checks if the given value is included in a list of acceptable values. Case insensitive.
   */
  public static class EnumeratedStringValidator extends StringValidator {
    private final Set<String> valuesSet = Sets.newLinkedHashSet();

    public EnumeratedStringValidator(String name, OptionDescription description, String... values) {
      super(name, description);
      for (String value : values) {
        valuesSet.add(value.toLowerCase());
      }
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      super.validate(v, metaData, manager);
      if (!valuesSet.contains(v.string_val.toLowerCase())) {
        throw UserException.validationError()
            .message(String.format("Option %s must be one of: %s.", getOptionName(), valuesSet))
            .build(logger);
      }
    }
  }

  /**
   * Unless explicitly changed by the user previously, the admin user
   * can only be determined at runtime
   */
  public static class AdminUsersValidator extends StringValidator {

    public final String DEFAULT_ADMIN_USERS = "%drill_process_user%";

    public AdminUsersValidator(String name, OptionDescription description) {
      super(name, description);
    }

    public String getAdminUsers(OptionSet optionManager) {
      String adminUsers = optionManager.getOption(ExecConstants.ADMIN_USERS_VALIDATOR);
      // if this option has not been changed by the user then return the
      // process user
      if (adminUsers.equals(DEFAULT_ADMIN_USERS)) {
        adminUsers = ImpersonationUtil.getProcessUserName();
      }
      adminUsers = DrillStringUtils.sanitizeCSV(adminUsers);
      return adminUsers;
    }
  }

  /**
   * Unless explicitly changed by the user previously, the admin user
   * groups can only be determined at runtime
   */
  public static class AdminUserGroupsValidator extends StringValidator {

    public final String DEFAULT_ADMIN_USER_GROUPS = "%drill_process_user_groups%";

    public AdminUserGroupsValidator(String name, OptionDescription description) {
      super(name, description);
    }

    public String getAdminUserGroups(OptionSet optionManager) {
      String adminUserGroups = optionManager.getOption(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR);
      // if this option has not been changed by the user then return the
      // process user groups
      if (adminUserGroups.equals(DEFAULT_ADMIN_USER_GROUPS)) {
        adminUserGroups = Joiner.on(",").join(ImpersonationUtil.getProcessUserGroupNames());
      }
      adminUserGroups = DrillStringUtils.sanitizeCSV(adminUserGroups);
      return adminUserGroups;
    }
  }

  /** Max width is a special validator which computes and validates
   *  the maxwidth. If the maxwidth is already set in system/session
   * the value is returned or else it is computed dynamically based on
   * the available number of processors and cpu load average
   */
  public static class MaxWidthValidator extends LongValidator{
    public MaxWidthValidator(String name, OptionDescription description) {
      super(name, description);
    }

    public int computeMaxWidth(double cpuLoadAverage, long maxWidth) {
      // if maxwidth is already set return it
      if (maxWidth != 0) {
        return (int) maxWidth;
      }
      // else compute the value and return
      else {
        int availProc = Runtime.getRuntime().availableProcessors();
        long maxWidthPerNode = Math.max(1, Math.min(availProc, Math.round(availProc * cpuLoadAverage)));
        return (int) maxWidthPerNode;
      }
    }
  }

  /**
   * Validator that checks if the given DateTime format template is valid.
   * See {@link DateTimeFormatter} for the acceptable values.
   */
  public static class DateTimeFormatValidator extends StringValidator {

    public DateTimeFormatValidator(String name, OptionDescription description) {
      super(name, description);
    }

    @Override
    public void validate(OptionValue v, OptionMetaData metaData, OptionSet manager) {
      super.validate(v, metaData, manager);
      if (!v.string_val.isEmpty()) {
        try {
          DateTimeFormatter.ofPattern(v.string_val);
        } catch (IllegalArgumentException e) {
          throw UserException.validationError()
              .message("'%s' is not a valid DateTime format pattern: %s", v.string_val, e.getMessage())
              .build(logger);
        }
      }
    }
  }

  public static abstract class TypeValidator extends OptionValidator {
    private final Kind kind;

    public TypeValidator(final String name, final Kind kind, final OptionDescription description) {
      super(name, description);
      this.kind = kind;
    }

    @Override
    public void validate(final OptionValue v, final OptionMetaData metaData, final OptionSet manager) {
      if (v.kind != kind) {
        throw UserException.validationError()
            .message(String.format("Option %s must be of type %s but you tried to set to %s.", getOptionName(),
              kind.name(), v.kind.name()))
            .build(logger);
      }
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public String getConfigProperty() {
      return ExecConstants.bootDefaultFor(getOptionName());
    }
  }
}
