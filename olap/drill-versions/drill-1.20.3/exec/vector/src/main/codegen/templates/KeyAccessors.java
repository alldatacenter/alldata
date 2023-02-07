<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/accessor/KeyAccessors.java" />

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
package org.apache.drill.exec.vector.accessor;

import java.math.BigDecimal;
import java.util.Arrays;

import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * This class is generated using Freemarker and the ${.template_name} template.
 */
public class KeyAccessors {

  <#list [
      {"name": "Boolean", "javaType": "boolean"},
      {"name": "Integer", "javaType": "int"},
      {"name": "Long", "javaType": "long"},
      {"name": "Double", "javaType": "double"},
      {"name": "String", "javaType": "String"},
      {"name": "Bytes", "javaType": "byte[]"},
      {"name": "Decimal", "javaType": "BigDecimal"},
      {"name": "Period", "javaType": "Period"},
      {"name": "Date", "javaType": "LocalDate"},
      {"name": "Time", "javaType": "LocalTime"},
      {"name": "Timestamp", "javaType": "Instant"}
    ] as valueType>

  public static class ${valueType.name}KeyAccessor extends AbstractKeyAccessor {

    public ${valueType.name}KeyAccessor(DictReader dictReader, ScalarReader keyReader) {
      super(dictReader, keyReader);
    }

    @Override
    public boolean find(${valueType.javaType} key) {
      dictReader.rewind();
      while (dictReader.next()) {
        <#if valueType.name == "Bytes">
        if (Arrays.equals(key, keyReader.getBytes())) {
        <#elseif valueType.name == "Integer">
        if (key == keyReader.getInt()) {
        <#elseif valueType.name == "Boolean" || valueType.name == "Long" || valueType.name == "Double">
        if (key == keyReader.get${valueType.name}()) {
        <#else>
        if (key.equals(keyReader.get${valueType.name}())) { // key should not be null so it is safe
        </#if>
          return true;
        }
      }
      return false;
    }
  }
  </#list>

  public static KeyAccessor getAccessor(DictReader dictReader, ScalarReader keyReader) {
    switch (keyReader.valueType()) {
      case BOOLEAN:
        return new BooleanKeyAccessor(dictReader, keyReader);
      case BYTES:
        return new BytesKeyAccessor(dictReader, keyReader);
      case DECIMAL:
        return new DecimalKeyAccessor(dictReader, keyReader);
      case DOUBLE:
        return new DoubleKeyAccessor(dictReader, keyReader);
      case INTEGER:
        return new IntegerKeyAccessor(dictReader, keyReader);
      case LONG:
        return new LongKeyAccessor(dictReader, keyReader);
      case PERIOD:
        return new PeriodKeyAccessor(dictReader, keyReader);
      case STRING:
        return new StringKeyAccessor(dictReader, keyReader);
      case DATE:
        return new DateKeyAccessor(dictReader, keyReader);
      case TIME:
        return new TimeKeyAccessor(dictReader, keyReader);
      case TIMESTAMP:
        return new TimestampKeyAccessor(dictReader, keyReader);
      default:
        throw new IllegalStateException("Unexpected key type: " + keyReader.valueType());
    }
  }
}
