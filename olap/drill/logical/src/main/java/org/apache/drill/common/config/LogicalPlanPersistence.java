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
package org.apache.drill.common.config;

import java.util.Set;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;


public class LogicalPlanPersistence {
  private static final Logger logger = LoggerFactory.getLogger(LogicalPlanPersistence.class);

  private final ObjectMapper mapper;

  public LogicalPlanPersistence(DrillConfig conf, ScanResult scanResult) {
    this(conf, scanResult, new ObjectMapper());
  }

  public LogicalPlanPersistence(DrillConfig conf, ScanResult scanResult, ObjectMapper mapper) {
    // The UI allows comments in JSON. Since we use the mapper
    // here to serialize plugin configs to/from the pe
    this.mapper = mapper
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    SimpleModule deserModule = new SimpleModule("LogicalExpressionDeserializationModule")
        .addDeserializer(LogicalExpression.class, new LogicalExpression.De(conf))
        .addDeserializer(SchemaPath.class, new SchemaPath.De());

    InjectableValues injectables = new InjectableValues.Std()
        .addValue(DrillConfig.class, conf);

    mapper.setInjectableValues(injectables);
    mapper.registerModule(deserModule);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
    mapper.configure(Feature.ALLOW_COMMENTS, true);
    mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
    // For LogicalOperatorBase
    registerSubtypes(getSubTypes(scanResult, LogicalOperator.class));
    // For StoragePluginConfigBase
    registerSubtypes(getSubTypes(scanResult, StoragePluginConfig.class));
    // For FormatPluginConfigBase
    registerSubtypes(getSubTypes(scanResult, FormatPluginConfig.class));
    registerSubtypes(getSubTypes(scanResult, CredentialsProvider.class));
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  private <T> void registerSubtypes(Set<Class<? extends T>> types) {
    for (Class<? extends T> type : types) {
      mapper.registerSubtypes(type);
    }
  }

  /**
   * Scan for implementations of the given interface.
   *
   * @param classpathScan Drill configuration object used to find the packages to scan
   * @return list of classes that implement the interface.
   */
  public static <T> Set<Class<? extends T>> getSubTypes(final ScanResult classpathScan, Class<T> parent) {
    Set<Class<? extends T>> subclasses = classpathScan.getImplementations(parent);
    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder()
        .append("Found ")
        .append(subclasses.size())
        .append(" ")
        .append(parent.getSimpleName())
        .append(" subclasses:\n");
      for (Class<?> c : subclasses) {
        sb.append('\t');
        sb.append(c.getName());
        sb.append('\n');
      }
      logger.debug(sb.toString());
    }
    return subclasses;
  }
}
