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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <p>
 * This represents a persisted {@link OptionValue}. Decoupling the {@link OptionValue} from what
 * is persisted will prevent us from accidentally breaking backward compatibility in the future
 * when the {@link OptionValue} changes. Additionally when we do change the format of stored options we
 * will not have to change much code since this is already designed with backward compatibility in mind.
 * This class is also forward compatible with the Drill Option storage format in Drill 1.11 and earlier.
 * </p>
 *
 * <p>
 * <b>Contract:</b>
 * Only {@link PersistedOptionValue}s created from an {@link OptionValue} should be persisted.
 * And {@link OptionValue}s should only be created from {@link PersistedOptionValue}s that are
 * retrieved from a store.
 * </p>
 */

// Custom Deserializer required for backward compatibility DRILL-5809
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = PersistedOptionValue.Deserializer.class)
public class PersistedOptionValue {
  /**
   * This is present for forward compatability with Drill 1.11 and earlier
   */
  public static final String SYSTEM_TYPE = "SYSTEM";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_TYPE = "type";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_KIND = "kind";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_NAME = "name";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_NUM_VAL = "num_val";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_STRING_VAL = "string_val";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_BOOL_VAL = "bool_val";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_FLOAT_VAL = "float_val";
  /**
   * This constant cannot be changed for backward and forward compatibility reasons.
   */
  public static final String JSON_INTEGER_VAL = "int_val";

  private String value;
  private OptionValue.Kind kind;
  private String name;
  private Long num_val;
  private String string_val;
  private Boolean bool_val;
  private Double float_val;

  public PersistedOptionValue(String value) {
    this.value = Preconditions.checkNotNull(value);
  }

  public PersistedOptionValue(OptionValue.Kind kind, String name,
                              Long num_val, String string_val,
                              Boolean bool_val, Double float_val) {
    this.kind = kind;
    this.name = name;
    this.num_val = num_val;
    this.string_val = string_val;
    this.bool_val = bool_val;
    this.float_val = float_val;

    switch (kind) {
      case BOOLEAN:
        Preconditions.checkNotNull(bool_val);
        value = bool_val.toString();
        break;
      case STRING:
        Preconditions.checkNotNull(string_val);
        value = string_val;
        break;
      case DOUBLE:
        Preconditions.checkNotNull(float_val);
        value = float_val.toString();
        break;
      case LONG:
        Preconditions.checkNotNull(num_val);
        value = num_val.toString();
        break;
      default:
        throw new UnsupportedOperationException(String.format("Unsupported type %s", kind));
    }
  }

  /**
   * This is ignored for forward compatibility.
   */
  @JsonIgnore
  public String getValue() {
    return value;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_TYPE)
  public String getType() {
    return SYSTEM_TYPE;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_KIND)
  public OptionValue.Kind getKind() {
    return kind;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_NAME)
  public String getName() {
    return name;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_NUM_VAL)
  public Long getNumVal() {
    return num_val;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_STRING_VAL)
  public String getStringVal() {
    return string_val;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_BOOL_VAL)
  public Boolean getBoolVal() {
    return bool_val;
  }

  /**
   * This is present for forward compatibility.
   */
  @JsonProperty(JSON_FLOAT_VAL)
  public Double getFloatVal() {
    return float_val;
  }

  public OptionValue toOptionValue(final OptionDefinition optionDefinition, final OptionValue.OptionScope optionScope) {
    Preconditions.checkNotNull(value, "The value must be defined in order for this to be converted to an " +
    "option value");
    final OptionValidator validator = optionDefinition.getValidator();
    final OptionValue.Kind kind = validator.getKind();
    final String name = validator.getOptionName();
    final OptionValue.AccessibleScopes accessibleScopes = optionDefinition.getMetaData().getAccessibleScopes();
    return OptionValue.create(kind, accessibleScopes, name, value, optionScope);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PersistedOptionValue that = (PersistedOptionValue) o;

    if (value != null ? !value.equals(that.value) : that.value != null) {
      return false;
    }

    if (kind != that.kind) {
      return false;
    }

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }

    if (num_val != null ? !num_val.equals(that.num_val) : that.num_val != null) {
      return false;
    }

    if (string_val != null ? !string_val.equals(that.string_val) : that.string_val != null) {
      return false;
    }

    if (bool_val != null ? !bool_val.equals(that.bool_val) : that.bool_val != null) {
      return false;
    }

    return float_val != null ? float_val.equals(that.float_val) : that.float_val == null;
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (num_val != null ? num_val.hashCode() : 0);
    result = 31 * result + (string_val != null ? string_val.hashCode() : 0);
    result = 31 * result + (bool_val != null ? bool_val.hashCode() : 0);
    result = 31 * result + (float_val != null ? float_val.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PersistedOptionValue{" + "value='" + value + '\'' + ", kind=" + kind + ", name='" + name +
      '\'' + ", num_val=" + num_val + ", string_val='" + string_val + '\'' + ", bool_val=" + bool_val +
      ", float_val=" + float_val + '}';
  }

  /**
   * This deserializer only fetches the relevant information we care about from a store, which is the
   * value of an option. This deserializer is essentially future proof since it only requires a value
   * to be stored for an option.
   */
  @SuppressWarnings("serial")
  public static class Deserializer extends StdDeserializer<PersistedOptionValue> {
    private static final Logger logger = LoggerFactory.getLogger(Deserializer.class);

    private Deserializer() {
      super(PersistedOptionValue.class);
    }

    protected Deserializer(JavaType valueType) {
      super(valueType);
    }

    protected Deserializer(StdDeserializer<?> src) {
      super(src);
    }

    @Override
    public PersistedOptionValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      ObjectCodec oc = p.getCodec();
      JsonNode node = oc.readTree(p);
      String value = null;

      if (node.has(OptionValue.JSON_NUM_VAL)) {
        value = node.get(OptionValue.JSON_NUM_VAL).asText();
      }

      if (node.has(OptionValue.JSON_STRING_VAL)) {
        value = node.get(OptionValue.JSON_STRING_VAL).asText();
      }

      if (node.has(OptionValue.JSON_BOOL_VAL)) {
        value = node.get(OptionValue.JSON_BOOL_VAL).asText();
      }

      if (node.has(OptionValue.JSON_FLOAT_VAL)) {
        value = node.get(OptionValue.JSON_FLOAT_VAL).asText();
      }

      if (node.has(OptionValue.JSON_INTEGER_VAL)) {
        value = node.get(OptionValue.JSON_INTEGER_VAL).asText();
      }

      if (value == null) {
        logger.error("Invalid json stored {}.", new ObjectMapper().writeValueAsString(node));
      }

      return new PersistedOptionValue(value);
    }
  }
}
