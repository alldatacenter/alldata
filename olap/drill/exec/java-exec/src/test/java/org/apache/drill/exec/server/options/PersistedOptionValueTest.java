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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.serialization.JacksonSerializer;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PersistedOptionValueTest extends BaseTest {
  /**
   * DRILL-5809
   * Note: If this test breaks you are probably breaking backward and forward compatibility. Verify with the community
   * that breaking compatibility is acceptable and planned for.
   * @throws Exception
   */
  @Test
  public void oldDeserializeTest() throws IOException {
    testHelper("/options/old_booleanopt.json",
      "/options/old_doubleopt.json",
      "/options/old_longopt.json",
      "/options/old_stringopt.json");
  }

  private void testHelper(String booleanOptionFile, String doubleOptionFile,
                          String longOptionFile, String stringOptionFile) throws IOException {
    JacksonSerializer serializer = new JacksonSerializer<>(new ObjectMapper(), PersistedOptionValue.class);
    String booleanOptionJson = DrillFileUtils.getResourceAsString(booleanOptionFile);
    String doubleOptionJson = DrillFileUtils.getResourceAsString(doubleOptionFile);
    String longOptionJson = DrillFileUtils.getResourceAsString(longOptionFile);
    String stringOptionJson = DrillFileUtils.getResourceAsString(stringOptionFile);

    PersistedOptionValue booleanValue = (PersistedOptionValue) serializer.deserialize(booleanOptionJson.getBytes());
    PersistedOptionValue doubleValue = (PersistedOptionValue) serializer.deserialize(doubleOptionJson.getBytes());
    PersistedOptionValue longValue = (PersistedOptionValue) serializer.deserialize(longOptionJson.getBytes());
    PersistedOptionValue stringValue = (PersistedOptionValue) serializer.deserialize(stringOptionJson.getBytes());

    PersistedOptionValue expectedBooleanValue = new PersistedOptionValue("true");
    PersistedOptionValue expectedDoubleValue = new PersistedOptionValue("1.5");
    PersistedOptionValue expectedLongValue = new PersistedOptionValue("5000");
    PersistedOptionValue expectedStringValue = new PersistedOptionValue("wabalubadubdub");

    Assert.assertEquals(expectedBooleanValue, booleanValue);
    Assert.assertEquals(expectedDoubleValue, doubleValue);
    Assert.assertEquals(expectedLongValue, longValue);
    Assert.assertEquals(expectedStringValue, stringValue);
  }

  @Test
  public void valueAssignment() {
    final String name = "myOption";
    final String stringContent = "val1";
    PersistedOptionValue stringValue =
      new PersistedOptionValue(OptionValue.Kind.STRING, name, null, stringContent, null, null);
    PersistedOptionValue numValue =
      new PersistedOptionValue(OptionValue.Kind.LONG, name, 100L, null, null, null);
    PersistedOptionValue boolValue =
      new PersistedOptionValue(OptionValue.Kind.BOOLEAN, name, null, null, true, null);
    PersistedOptionValue floatValue =
      new PersistedOptionValue(OptionValue.Kind.DOUBLE, name, null, null, null, 55.5);

    Assert.assertEquals(stringContent, stringValue.getValue());
    Assert.assertEquals("100", numValue.getValue());
    Assert.assertEquals("true", boolValue.getValue());
    Assert.assertEquals("55.5", floatValue.getValue());
  }

  /**
   * DRILL-5809 Test forward compatibility with Drill 1.11 and earlier.
   * Note: If this test breaks you are probably breaking forward compatibility. Verify with the community
   * that breaking compatibility is acceptable and planned for.
   * @throws Exception
   */
  @Test
  public void testForwardCompatibility() throws IOException {
    final String name = "myOption";

    JacksonSerializer realSerializer = new JacksonSerializer<>(new ObjectMapper(), PersistedOptionValue.class);
    JacksonSerializer mockSerializer = new JacksonSerializer<>(new ObjectMapper(), MockPersistedOptionValue.class);

    final String stringContent = "val1";
    PersistedOptionValue stringValue =
      new PersistedOptionValue(OptionValue.Kind.STRING, name, null, stringContent, null, null);
    PersistedOptionValue numValue =
      new PersistedOptionValue(OptionValue.Kind.LONG, name, 100L, null, null, null);
    PersistedOptionValue boolValue =
      new PersistedOptionValue(OptionValue.Kind.BOOLEAN, name, null, null, true, null);
    PersistedOptionValue floatValue =
      new PersistedOptionValue(OptionValue.Kind.DOUBLE, name, null, null, null, 55.5);

    byte[] stringValueBytes = realSerializer.serialize(stringValue);
    byte[] numValueBytes = realSerializer.serialize(numValue);
    byte[] boolValueBytes = realSerializer.serialize(boolValue);
    byte[] floatValueBytes = realSerializer.serialize(floatValue);

    MockPersistedOptionValue mockStringValue = (MockPersistedOptionValue) mockSerializer.deserialize(stringValueBytes);
    MockPersistedOptionValue mockNumValue = (MockPersistedOptionValue) mockSerializer.deserialize(numValueBytes);
    MockPersistedOptionValue mockBoolValue = (MockPersistedOptionValue) mockSerializer.deserialize(boolValueBytes);
    MockPersistedOptionValue mockFloatValue = (MockPersistedOptionValue) mockSerializer.deserialize(floatValueBytes);

    MockPersistedOptionValue expectedStringValue =
      new MockPersistedOptionValue(PersistedOptionValue.SYSTEM_TYPE, OptionValue.Kind.STRING, name,
        null, stringContent, null, null);
    MockPersistedOptionValue expectedNumValue =
      new MockPersistedOptionValue(PersistedOptionValue.SYSTEM_TYPE, OptionValue.Kind.LONG, name,
        100L, null, null, null);
    MockPersistedOptionValue expectedBoolValue =
      new MockPersistedOptionValue(PersistedOptionValue.SYSTEM_TYPE, OptionValue.Kind.BOOLEAN, name,
        null, null, true, null);
    MockPersistedOptionValue expectedFloatValue =
      new MockPersistedOptionValue(PersistedOptionValue.SYSTEM_TYPE, OptionValue.Kind.DOUBLE, name,
        null, null, null, 55.5);

    Assert.assertEquals(expectedStringValue, mockStringValue);
    Assert.assertEquals(expectedNumValue, mockNumValue);
    Assert.assertEquals(expectedBoolValue, mockBoolValue);
    Assert.assertEquals(expectedFloatValue, mockFloatValue);
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class MockPersistedOptionValue {
    public final String type;
    public final OptionValue.Kind kind;
    public final String name;
    public final Long num_val;
    public final String string_val;
    public final Boolean bool_val;
    public final Double float_val;

    public MockPersistedOptionValue(@JsonProperty(PersistedOptionValue.JSON_TYPE) String type,
                                    @JsonProperty(PersistedOptionValue.JSON_KIND) OptionValue.Kind kind,
                                    @JsonProperty(PersistedOptionValue.JSON_NAME) String name,
                                    @JsonProperty(PersistedOptionValue.JSON_NUM_VAL) Long num_val,
                                    @JsonProperty(PersistedOptionValue.JSON_STRING_VAL) String string_val,
                                    @JsonProperty(PersistedOptionValue.JSON_BOOL_VAL) Boolean bool_val,
                                    @JsonProperty(PersistedOptionValue.JSON_FLOAT_VAL) Double float_val) {
      this.type = type;
      this.kind = kind;
      this.name = name;
      this.num_val = num_val;
      this.string_val = string_val;
      this.bool_val = bool_val;
      this.float_val = float_val;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      MockPersistedOptionValue that = (MockPersistedOptionValue) o;

      if (!type.equals(that.type)) {
        return false;
      }

      if (kind != that.kind) {
        return false;
      }

      if (!name.equals(that.name)) {
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
      int result = type.hashCode();
      result = 31 * result + kind.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + (num_val != null ? num_val.hashCode() : 0);
      result = 31 * result + (string_val != null ? string_val.hashCode() : 0);
      result = 31 * result + (bool_val != null ? bool_val.hashCode() : 0);
      result = 31 * result + (float_val != null ? float_val.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "MockPersistedOptionValue{" + "type='" + type + '\'' + ", kind=" + kind + ", name='" + name +
        '\'' + ", num_val=" + num_val + ", string_val='" + string_val + '\'' + ", bool_val=" + bool_val +
        ", float_val=" + float_val + '}';
    }
  }
}
