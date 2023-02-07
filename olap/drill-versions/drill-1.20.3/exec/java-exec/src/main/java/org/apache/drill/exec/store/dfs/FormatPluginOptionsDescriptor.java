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
package org.apache.drill.exec.store.dfs;

import static java.util.Collections.unmodifiableMap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory.TableInstance;
import org.apache.drill.exec.store.table.function.TableParamDef;
import org.apache.drill.exec.store.table.function.TableSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Describes the options for a format plugin
 * extracted from the FormatPluginConfig subclass
 */
final class FormatPluginOptionsDescriptor {
  private static final Logger logger = LoggerFactory.getLogger(FormatPluginOptionsDescriptor.class);

  protected final Class<? extends FormatPluginConfig> pluginConfigClass;
  protected final String typeName;
  private final Map<String, TableParamDef> functionParamsByName;

  /**
   * Uses reflection to extract options based on the fields of the provided config class
   * ("List extensions" field is ignored, pending removal, Char is turned into String)
   * The class must be annotated with {@code @JsonTypeName("type name")}
   * @param pluginConfigClass the config class we want to extract options from through reflection
   */
  FormatPluginOptionsDescriptor(Class<? extends FormatPluginConfig> pluginConfigClass) {
    this.pluginConfigClass = pluginConfigClass;
    Map<String, TableParamDef> paramsByName = new LinkedHashMap<>();
    Field[] fields = pluginConfigClass.getDeclaredFields();
    // @JsonTypeName("text")
    JsonTypeName annotation = pluginConfigClass.getAnnotation(JsonTypeName.class);
    this.typeName = annotation != null ? annotation.value() : null;
    if (this.typeName != null) {
      paramsByName.put("type", TableParamDef.required("type", String.class, null));
    }
    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers())
          // we want to deprecate this field
          || (field.getName().equals("extensions") && field.getType() == List.class)) {
        continue;
      }
      Class<?> fieldType = field.getType();
      if (fieldType == char.class) {
        // calcite does not like char type. Just use String and enforce later that length == 1
        fieldType = String.class;
      }
      paramsByName.put(field.getName(), TableParamDef.optional(field.getName(), fieldType, null));
    }
    this.functionParamsByName = unmodifiableMap(paramsByName);
  }

  public String getTypeName() { return typeName; }

  /**
   * Returns the table function signature for this format plugin config class.
   *
   * @param tableName the table for which we want a table function signature
   * @param tableParameters common table parameters to be included
   * @return the signature
   */
  protected TableSignature getTableSignature(String tableName, List<TableParamDef> tableParameters) {
    return TableSignature.of(tableName, tableParameters, params());
  }

  /**
   * @return the parameters extracted from the provided format plugin config class
   */
  private List<TableParamDef> params() {
    return new ArrayList<>(functionParamsByName.values());
  }

  /**
   * @return a readable String of the parameters and their names
   */
  protected String presentParams() {
    StringBuilder sb = new StringBuilder("(");
    List<TableParamDef> params = params();
    for (int i = 0; i < params.size(); i++) {
      TableParamDef paramDef = params.get(i);
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(paramDef.getName()).append(": ").append(paramDef.getType().getSimpleName());
    }
    sb.append(")");
    return sb.toString();
  }
  /**
   * Creates an instance of the FormatPluginConfig based on the passed parameters.
   *
   * @param t the signature and the parameters passed to the table function
   * @param mapper
   * @return the corresponding config
   */
  FormatPluginConfig createConfigForTable(TableInstance t, ObjectMapper mapper, FormatPluginConfig baseConfig) {
    ConfigCreator configCreator = new ConfigCreator(t, mapper, baseConfig);
    return configCreator.createNewStyle();
  }

  @Override
  public String toString() {
    return "OptionsDescriptor [pluginConfigClass=" + pluginConfigClass + ", typeName=" + typeName
        + ", functionParamsByName=" + functionParamsByName + "]";
  }

  /**
   * Implements a table function to specify a format config. Provides two
   * Implementations. The first is the "legacy" version (Drill 1.17 and
   * before), which relies on a niladic constructor and mutable fields.
   * Since mutable fields conflicts with the desire for configs to be
   * immutable, the newer version (Drill 1.8 and later) use JSON serialization
   * to create a JSON object with the desired properties and to seriarialize
   * that object to a config. Since Jackson allows creating JSON objects
   * from an existing config, this newer method merges the existing plugin
   * properties with those specified in the table function. Essentially
   * the table function "inherits" any existing config, "overriding" only
   * those properties which are specified. Prior to Drill 1.18, a table
   * function inherited the default properties, even if there was an
   * existing plugin for the target file. See DRILL-6168. The original
   * behavior is retained in case we find we need to add an option to
   * cause Drill to revert to the old behavior.
   */
  private class ConfigCreator {
    final TableInstance t;
    final FormatPluginConfig baseConfig;
    final List<TableParamDef> formatParams;
    final List<Object> formatParamsValues;
    final ObjectMapper mapper;

    public ConfigCreator(TableInstance table, ObjectMapper mapper, FormatPluginConfig baseConfig) {
      this.t = table;
      this.mapper = mapper;

      // Abundance of caution: if the base is not of the correct
      // type, just ignore it to avoid introducing new errors.
      // Drill prior to 1.18 didn't use a base config.
      if (baseConfig == null || baseConfig.getClass() != pluginConfigClass) {
        this.baseConfig = null;
      } else {
        this.baseConfig = baseConfig;
      }
      formatParams = t.sig.getSpecificParams();
      // Exclude common params values, leave only format related params
      formatParamsValues = t.params.subList(0, t.params.size() - t.sig.getCommonParams().size());
    }

    public FormatPluginConfig createNewStyle() {
      verifyType();
      ObjectNode configObject = makeConfigNode();
      applyParams(configObject);
      // Do the following to visualize the merged object
      // System.out.println(mapper.writeValueAsString(configObject));
      return nodeToConfig(configObject);
    }

    /**
     * Create a JSON node for the config: from the existing config
     * if available, else an empty node.
     */
    private ObjectNode makeConfigNode() {
      if (baseConfig == null) {
        ObjectNode configObject = mapper.createObjectNode();

        // Type field is required to deserialize config
        configObject.replace("type",
            mapper.convertValue(typeName, JsonNode.class));
        return configObject;
      } else {
        return mapper.valueToTree(baseConfig);
      }
    }

    /**
     * Replace any existing properties with the fields from the
     * table function.
     */
    private void applyParams(ObjectNode configObject) {
      for (int i = 1; i < formatParamsValues.size(); i++) {
        applyParam(configObject, i);
      }
    }

    private void applyParam(ObjectNode configObject, int i) {
      Object param = paramValue(i);
      // when null is passed, we leave the default defined in the config instance
      if (param != null) {
        configObject.replace(formatParams.get(i).getName(),
            mapper.convertValue(param, JsonNode.class));
      }
    }

    private Object paramValue(int i) {
      Object param = formatParamsValues.get(i);
      if (param != null && param instanceof String) {
        // normalize Java literals, ex: \t, \n, \r
        param = StringEscapeUtils.unescapeJava((String) param);
      }
      return param;
    }

    /**
     * Convert the JSON node to a format config.
     */
    private FormatPluginConfig nodeToConfig(ObjectNode configObject) {
      try {
        return mapper.readerFor(pluginConfigClass).readValue(configObject);
      } catch (IOException e) {
        String jsonConfig;
        try {
          jsonConfig = mapper.writeValueAsString(configObject);
        } catch (JsonProcessingException e1) {
          jsonConfig = "unavailable: " + e1.getMessage();
        }
        throw UserException.parseError(e)
          .message(
              "configuration for format of type %s can not be created (class: %s)",
              typeName, pluginConfigClass.getName())
          .addContext("table", t.sig.getName())
          .addContext("JSON configuration", jsonConfig)
          .build(logger);
      }
    }

    /**
     * Creates a format plugin config in the style prior to
     * Drill 1.8: binds parameters to public, mutable fields.
     * However, this causes issues: fields should be immutable (DRILL-7612, DRILL-6672).
     * Also, this style does not allow retaining some fields
     * while customizing others. (DRILL-6168).
     * @return
     */
    @SuppressWarnings("unused")
    public FormatPluginConfig createOldStyle() {
      verifyType();
      FormatPluginConfig config = configInstance();
      bindParams(config);
      return config;
    }

    public void verifyType() {

      // Per the constructor, the first param is always "type"
      TableParamDef typeParamDef = formatParams.get(0);
      Object typeParam = formatParamsValues.get(0);
      if (!typeParamDef.getName().equals("type")
          || typeParamDef.getType() != String.class
          || !(typeParam instanceof String)
          || !typeName.equalsIgnoreCase((String)typeParam)) {
        // if we reach here, there's a bug as all signatures generated start with a type parameter
        throw UserException.parseError()
            .message(
                "This function signature is not supported: %s\n"
                + "expecting %s",
                t.presentParams(), presentParams())
            .addContext("table", t.sig.getName())
            .build(logger);
      }
    }

    public FormatPluginConfig configInstance() {
      try {
        return pluginConfigClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        throw UserException.parseError(e)
            .message(
                "configuration for format of type %s can not be created (class: %s)",
                typeName, pluginConfigClass.getName())
            .addContext("table", t.sig.getName())
            .build(logger);
      }
    }

    private void bindParams(FormatPluginConfig config) {
      for (int i = 1; i < formatParamsValues.size(); i++) {
        bindParam(config, i);
      }
    }

    private void bindParam(FormatPluginConfig config, int i) {
      Object param = paramValue(i);
      if (param == null) {
        // when null is passed, we leave the default defined in the config class
        return;
      }
      TableParamDef paramDef = formatParams.get(i);
      TableParamDef expectedParamDef = functionParamsByName.get(paramDef.getName());
      if (expectedParamDef == null || expectedParamDef.getType() != paramDef.getType()) {
        throw UserException.parseError()
        .message(
            "The parameters provided are not applicable to the type specified:\n"
                + "provided: %s\nexpected: %s",
            t.presentParams(), presentParams())
        .addContext("table", t.sig.getName())
        .build(logger);
      }
      try {
        Field field = pluginConfigClass.getField(paramDef.getName());
        field.setAccessible(true);
        if (field.getType() == char.class && param instanceof String) {
          String stringParam = (String) param;
          if (stringParam.length() != 1) {
            throw UserException.parseError()
              .message("Expected single character but was String: %s", stringParam)
              .addContext("Table", t.sig.getName())
              .addContext("Parameter", paramDef.getName())
              .build(logger);
          }
          param = stringParam.charAt(0);
        }
        field.set(config, param);
      } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
        throw UserException.parseError(e)
            .message("Can not set value %s to parameter %s: %s", param, paramDef.getName(), paramDef.getType())
            .addContext("Table", t.sig.getName())
            .addContext("Parameter", paramDef.getName())
            .build(logger);
      }
    }
  }
}
