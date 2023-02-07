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
package org.apache.drill.exec.util;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This helper class holds any custom Jackson serializers used when outputing
 * the data in JSON format.
 */
public class SerializationModule {

  // copied from DateUtility. Added here for inclusion into drill-jdbc-all
  public static final DateTimeFormatter formatDate      = DateTimeFormatter.ofPattern("uuuu-MM-dd")
      .withZone(ZoneOffset.UTC);
  public static final DateTimeFormatter formatTimeStamp = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
      .withZone(ZoneOffset.UTC);
  public static final DateTimeFormatter formatTime      = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
      .withZone(ZoneOffset.UTC);

  public static final SimpleModule drillModule = new SimpleModule("DrillModule");

  static {
    drillModule.addSerializer(LocalTime.class, new JsonSerializer<LocalTime>() {
      @Override
      public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(formatTime.format(value));
      }
    });

    drillModule.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
      @Override
      public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(formatDate.format(value));
      }
    });

    drillModule.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
      @Override
      public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(formatTimeStamp.format(value));
      }
    });
  }

  public static final SimpleModule getModule() {
    return drillModule;
  }
}
