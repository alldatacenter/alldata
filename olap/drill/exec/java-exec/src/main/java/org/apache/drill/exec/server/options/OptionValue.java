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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * <p>
 * An {@link OptionValue option value} is used internally by an
 * {@link OptionManager} to store a run-time setting. This setting, for example,
 * could affect a query in an execution stage. Instances of this class are JSON
 * serializable and can be stored in a {@link PersistentStore persistent store}
 * (see {@link SystemOptionManager#options}), or in memory (see
 * {@link InMemoryOptionManager#options}).
 * </p>
 * <p>
 * {@link AccessibleScopes} defines the scopes at which the option can be set.
 * If it can be set at System level or Session level or so on. Whereas
 * {@link OptionScope} defines the scope at which the option is being set. If
 * the option is being set at the BOOT time the scope of the option is BOOT. If
 * it is set at SYSTEM level the scope is SYSTEM. Although they look similar
 * there is a fine level which differentiates both of them which is at which
 * level of hierarchy they can be set and at what at level of hierarchy they
 * were actually set.
 * </p>
 */
@JsonInclude(Include.NON_NULL)
public class OptionValue implements Comparable<OptionValue> {
  private static final Logger logger = LoggerFactory.getLogger(OptionValue.class);

  public static final String JSON_KIND = "kind";
  public static final String JSON_ACCESSIBLE_SCOPES = "accessibleScopes";
  public static final String JSON_NAME = "name";
  public static final String JSON_NUM_VAL = "num_val";
  public static final String JSON_STRING_VAL = "string_val";
  public static final String JSON_BOOL_VAL = "bool_val";
  public static final String JSON_FLOAT_VAL = "float_val";
  public static final String JSON_INTEGER_VAL = "int_val";
  public static final String JSON_SCOPE = "scope";

  /**
   * Defines where an option can be configured.
   */
  public enum AccessibleScopes {
    BOOT(EnumSet.of(OptionScope.BOOT)), // Can only be configured at boot time
    SYSTEM(EnumSet.of(OptionScope.BOOT, OptionScope.SYSTEM)), // Can only be configured at the system level
    SESSION(EnumSet.of(OptionScope.BOOT, OptionScope.SESSION)), // Can only be configured at the session level
    QUERY(EnumSet.of(OptionScope.BOOT, OptionScope.QUERY)), // Can only be configured at the query level
    SYSTEM_AND_SESSION(EnumSet.of(OptionScope.BOOT, OptionScope.SYSTEM, OptionScope.SESSION)), // Can only be configured at the system or session level
    SESSION_AND_QUERY(EnumSet.of(OptionScope.BOOT, OptionScope.SESSION, OptionScope.QUERY)), // Can only be configured at the session or query level
    ALL(EnumSet.of(OptionScope.BOOT, OptionScope.SYSTEM, OptionScope.SESSION, OptionScope.QUERY)); // Can be configured at the system, session, or query level

    private EnumSet<OptionScope> scopes;

    AccessibleScopes(EnumSet<OptionScope> scopes) {
      this.scopes = Preconditions.checkNotNull(scopes);
    }

    public boolean inScopeOf(OptionScope scope) {
      return scopes.contains(scope);
    }
  }

  public enum Kind {
    BOOLEAN, LONG, STRING, DOUBLE
  }

  /**
   * This defines where an option was actually configured.
   */
  public enum OptionScope {
    BOOT, SYSTEM, SESSION, QUERY
  }

  public final String name;
  public final Kind kind;
  public final AccessibleScopes accessibleScopes;
  public final Long num_val;
  public final String string_val;
  public final Boolean bool_val;
  public final Double float_val;
  public final OptionScope scope;

  public static OptionValue create(AccessibleScopes accessibleScopes, String name, long val, OptionScope scope) {
    return new OptionValue(Kind.LONG, accessibleScopes, name, val, null, null, null, scope);
  }

  public static OptionValue create(AccessibleScopes accessibleScopes, String name, boolean bool, OptionScope scope) {
    return new OptionValue(Kind.BOOLEAN, accessibleScopes, name, null, null, bool, null, scope);
  }

  public static OptionValue create(AccessibleScopes accessibleScopes, String name, String val, OptionScope scope) {
    return new OptionValue(Kind.STRING, accessibleScopes, name, null, val, null, null, scope);
  }

  public static OptionValue create(AccessibleScopes accessibleScopes, String name, double val, OptionScope scope) {
    return new OptionValue(Kind.DOUBLE, accessibleScopes, name, null, null, null, val, scope);
  }

  public static OptionValue create(Kind kind, AccessibleScopes accessibleScopes,
                                   String name, String val, OptionScope scope) {
    try {
      switch (kind) {
        case BOOLEAN:
          return create(accessibleScopes, name, Boolean.valueOf(val), scope);
        case STRING:
          return create(accessibleScopes, name, val, scope);
        case DOUBLE:
          return create(accessibleScopes, name, Double.parseDouble(val), scope);
        case LONG:
          return create(accessibleScopes, name, Long.parseLong(val), scope);
        default:
          throw new IllegalArgumentException(String.format("Unsupported kind %s", kind));
      }
    } catch (NumberFormatException e) {
      throw UserException.validationError(e)
        .message("'%s' is not a valid value option '%s' of type %s",
            val.toString(), name, kind.name())
        .build(logger);
    }
  }

  public static OptionValue create(AccessibleScopes type, String name, Object val, OptionScope scope, Kind kind) {
    Preconditions.checkArgument(val != null);
    if (val instanceof Boolean) {
      return create(type, name, ((Boolean) val).booleanValue(), scope);
    } else if (val instanceof Long) {
      return create(type, name, ((Long) val).longValue(), scope);
    } else if (val instanceof Integer) {
      return create(type, name, ((Integer) val).longValue(), scope);
    } else if (val instanceof String) {
      return fromString(type, name, (String) val, scope, kind);
    } else if (val instanceof Double) {
      return create(type, name, ((Double) val).doubleValue(), scope);
    } else if (val instanceof Float) {
      return create(type, name, ((Float) val).doubleValue(), scope);
    }

    throw new IllegalArgumentException(String.format("Unsupported type %s", val.getClass()));
  }

  private static OptionValue fromString(AccessibleScopes type, String name,
      String val, OptionScope scope, Kind kind) {
    Preconditions.checkArgument(val != null);
    val = val.trim();
    try {
      switch (kind) {
        case BOOLEAN: {

          // Strict enforcement of true or false, in any case
          val = val.toLowerCase();
          if (!val.equals("true") && !val.equals("false")) {
            throw UserException.validationError()
              .message("'%s' is not a valid value for option '%s' of type %s",
                  val, name, kind.name())
              .build(logger);
          }
          return create(type, name, Boolean.parseBoolean(val.toLowerCase()), scope);
        }
        case DOUBLE:
          return create(type, name, Double.parseDouble(val), scope);
        case LONG:
          return create(type, name, Long.parseLong(val), scope);
        case STRING:
          return create(type, name, val, scope);
        default:
          throw new IllegalStateException(kind.name());
      }
    } catch (NumberFormatException e) {
      throw UserException.validationError(e)
        .message("'%s' is not a valid value for option '%s' of type %s",
            val, name, kind.name())
        .build(logger);
    }
  }

  @JsonCreator
  private OptionValue(@JsonProperty(JSON_KIND) Kind kind,
                      @JsonProperty(JSON_ACCESSIBLE_SCOPES) AccessibleScopes accessibleScopes,
                      @JsonProperty(JSON_NAME) String name,
                      @JsonProperty(JSON_NUM_VAL) Long num_val,
                      @JsonProperty(JSON_STRING_VAL) String string_val,
                      @JsonProperty(JSON_BOOL_VAL) Boolean bool_val,
                      @JsonProperty(JSON_FLOAT_VAL) Double float_val,
                      @JsonProperty(JSON_SCOPE) OptionScope scope) {
    Preconditions.checkArgument(num_val != null || string_val != null || bool_val != null || float_val != null);
    this.kind = kind;
    this.accessibleScopes = accessibleScopes;
    this.name = name;
    this.float_val = float_val;
    this.num_val = num_val;
    this.string_val = string_val;
    this.bool_val = bool_val;
    this.scope = scope;
  }

  public String getName() {
    return name;
  }

  @JsonIgnore
  public Object getValue() {
    switch (kind) {
      case BOOLEAN:
        return bool_val;
      case LONG:
        return num_val;
      case STRING:
        return string_val;
      case DOUBLE:
        return float_val;
      default:
        return null;
    }
  }

  /**
   * Gets the value of this option if it exists at a scope at least as narrow as the given scope.
   * @param minScope scope which the option's scope should be narrower than
   * @return null if the option does not exist at a scope at least as narrow as minScope
   */
  @JsonIgnore
  public Object getValueMinScope(OptionScope minScope) {
    return scope.compareTo(minScope) >= 0 ? getValue() : null;
  }

  @JsonIgnore
  public OptionScope getScope() {
    return scope;
  }

  public PersistedOptionValue toPersisted() {
    return new PersistedOptionValue(kind, name, num_val, string_val, bool_val, float_val);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((bool_val == null) ? 0 : bool_val.hashCode());
    result = prime * result + ((float_val == null) ? 0 : float_val.hashCode());
    result = prime * result + ((kind == null) ? 0 : kind.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((num_val == null) ? 0 : num_val.hashCode());
    result = prime * result + ((string_val == null) ? 0 : string_val.hashCode());
    result = prime * result + ((accessibleScopes == null) ? 0 : accessibleScopes.hashCode());
    return result;
  }

  public boolean equalsIgnoreType(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final OptionValue other = (OptionValue) obj;
    if (bool_val == null) {
      if (other.bool_val != null) {
        return false;
      }
    } else if (!bool_val.equals(other.bool_val)) {
      return false;
    }
    if (float_val == null) {
      if (other.float_val != null) {
        return false;
      }
    } else if (!float_val.equals(other.float_val)) {
      return false;
    }
    if (kind != other.kind) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (num_val == null) {
      if (other.num_val != null) {
        return false;
      }
    } else if (!num_val.equals(other.num_val)) {
      return false;
    }
    if (string_val == null) {
      if (other.string_val != null) {
        return false;
      }
    } else if (!string_val.equals(other.string_val)) {
      return false;
    }

    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (!equalsIgnoreType(obj)) {
      return false;
    }
    final OptionValue other = (OptionValue) obj;
    return accessibleScopes == other.accessibleScopes;
  }

  @Override
  public int compareTo(OptionValue o) {
    return this.name.compareTo(o.name);
  }

  @Override
  public String toString() {
    return "OptionValue [ accessibleScopes=" + accessibleScopes + ", optionScope=" + scope + ", name=" + name + ", value=" + getValue() + " ]";
  }
}
