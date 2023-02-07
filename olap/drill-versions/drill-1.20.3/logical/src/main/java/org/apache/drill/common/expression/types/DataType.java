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
package org.apache.drill.common.expression.types;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonSerialize(using = DataType.Se.class)
@JsonDeserialize(using = DataType.De.class)
abstract class DataType {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataType.class);

  public static enum Comparability{
    UNKNOWN, NONE, EQUAL, ORDERED;
  }

  public abstract String getName();
  public abstract boolean isLateBind();
  public abstract boolean hasChildType();
  public abstract DataType getChildType();
  public abstract Comparability getComparability();
  public abstract boolean isNumericType();



  public static final DataType LATEBIND = new LateBindType();
  public static final DataType BOOLEAN = new AtomType("BOOLEAN", Comparability.EQUAL, false);
  public static final DataType BYTES = new AtomType("BYTES", Comparability.ORDERED, false);
  public static final DataType SIGNED_BYTE = new AtomType("SIGNED_BYTE", Comparability.ORDERED, true);
  public static final DataType SIGNED_INT16 = new AtomType("SIGNED_INT16", Comparability.ORDERED, true);
  public static final DataType NVARCHAR = new AtomType("VARCHAR", Comparability.ORDERED, false);
  public static final DataType FLOAT32 = new AtomType("FLOAT32", Comparability.ORDERED, true);
  public static final DataType FLOAT64 = new AtomType("FLOAT64", Comparability.ORDERED, true);
  public static final DataType INT64 = new AtomType("INT64", Comparability.ORDERED, true);
  public static final DataType INT32 = new AtomType("INT32", Comparability.ORDERED, true);
  public static final DataType INT16 = new AtomType("INT16", Comparability.ORDERED, true);
  public static final DataType UINT16 = new AtomType("UINT16", Comparability.ORDERED, true);
//  public static final DataType INT16 = new AtomType("int16", Comparability.ORDERED, true);
//  public static final DataType BIG_INTEGER = new AtomType("bigint", Comparability.ORDERED, true);
//  public static final DataType BIG_DECIMAL = new AtomType("bigdecimal", Comparability.ORDERED, true);
  public static final DataType DATE = new AtomType("DATE", Comparability.ORDERED, false);
  public static final DataType DATETIME = new AtomType("DATETIME", Comparability.ORDERED, false);
  public static final DataType MAP = new AtomType("MAP", Comparability.NONE, false);
  public static final DataType ARRAY = new AtomType("ARRAY", Comparability.NONE, false);
  public static final DataType NULL = new AtomType("NULL", Comparability.NONE, false);

  //TODO: Hack to get some size data, needs to be fixed so that each type reveals it's size.
  public int size() {
    if(this == BOOLEAN) {
      return 1;
    }else if(this == INT32) {
      return 4;
    }else if(this == INT16) {
      return 4;
    }
    return 2;
  }

  static final Map<String, DataType> TYPES;
  static {
    Field[] fields = DataType.class.getFields();
    Map<String, DataType> types = new HashMap<String, DataType>();
    for (Field f : fields) {
      //logger.debug("Reviewing {}, Field: {}", f.getClass(), f);
      if (Modifier.isStatic(f.getModifiers())) {
        try {
          Object o = f.get(null);
          //logger.debug("Object {}", o);

          if (o instanceof DataType) {
            types.put(((DataType) o).getName(), (DataType) o);
          }
        } catch (IllegalArgumentException | IllegalAccessException e) {
          logger.warn("Failure while reading DataType.", e);
        }
      }
    }
    TYPES = Collections.unmodifiableMap(types);
  }

  public static DataType getDataType(String name) {
    if (TYPES.containsKey(name)) {
      return TYPES.get(name);
    } else {
      throw new IllegalArgumentException(String.format("Unknown type requested of [%s].", name));
    }
  }

  public static class De extends StdDeserializer<DataType> {

    public De() {
      super(DataType.class);
    }

    @Override
    public DataType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException {
      return getDataType(this._parseString(jp, ctxt));
    }

  }

  public static class Se extends StdSerializer<DataType> {

    public Se() {
      super(DataType.class);
    }

    @Override
    public void serialize(DataType value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
        JsonGenerationException {
      jgen.writeString(value.getName());
    }

  }

}
