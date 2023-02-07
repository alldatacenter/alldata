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
package org.apache.drill.exec.store.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.planner.logical.DrillTableSelection;

/**
 * Structure of a mock table definition file. Yes, using Jackson deserialization to parse
 * the file is brittle, but this is for testing so we're favoring convenience
 * over robustness.
 */

@JsonTypeName("mock-table")
public class MockTableDef {

  /**
   * Describes one simulated file (or block) within the logical file scan
   * described by this group scan. Each block can have a distinct schema to test
   * for schema changes.
   */

  public static class MockScanEntry {

    final int records;
    final boolean extended;
    final int batchSize;
    final int repeat;
    private final MockColumn[] types;

    @JsonCreator
    public MockScanEntry(@JsonProperty("records") int records,
                         @JsonProperty("extended") Boolean extended,
                         @JsonProperty("batchSize") Integer batchSize,
                         @JsonProperty("repeat") Integer repeat,
                         @JsonProperty("types") MockTableDef.MockColumn[] types) {
      this.records = records;
      this.types = types;
      this.extended = (extended == null) ? false : extended;
      this.batchSize = (batchSize == null) ? 0 : batchSize;
      this.repeat = (repeat == null) ? 1 : repeat;
    }

    public int getRecords() { return records; }
    public boolean isExtended() { return extended; }
    public int getBatchSize() { return batchSize; }
    public int getRepeat() { return repeat; }

    public MockTableDef.MockColumn[] getTypes() {
      return types;
    }

    @Override
    public String toString() {
      return "MockScanEntry [records=" + records + ", columns="
          + Arrays.toString(types) + "]";
    }
  }

  /**
   * A tiny wrapper class to add required DrillTableSelection behaviour to
   * the entries list.
   */
  public static class MockTableSelection implements DrillTableSelection {
    private final List<MockScanEntry> entries;

    @JsonCreator
    public MockTableSelection(@JsonProperty("entries") List<MockScanEntry> entries) {
      this.entries = entries;
    }

    @JsonIgnore
    @Override
    public String digest() {
      return entries.toString();
    }

    public List<MockScanEntry> getEntries() {
      return entries;
    }
  }

  /**
   * Meta-data description of the columns we wish to create during a simulated
   * scan.
   */

  @JsonInclude(Include.NON_NULL)
  public static class MockColumn {

    /**
     * Column type given as a Drill minor type (that is, a type without the
     * extra information such as cardinality, width, etc.
     */

    @JsonProperty("type")
    public MinorType minorType;
    public String name;
    public DataMode mode;
    public Integer width;
    public Integer precision;
    public Integer scale;

    /**
     * The scan can request to use a specific data generator class. The name of
     * that class appears here. The name can be a simple class name, if that
     * class resides in this Java package. Or, it can be a fully qualified name
     * of a class that resides elsewhere. If null, the default generator for the
     * data type is used.
     */

    public String generator;

    /**
     * Some tests want to create a very wide row with many columns. This field
     * eases that task: specify a value other than 1 and the data source will
     * generate that many copies of the column, each with separately generated
     * random values. For example, to create 20 copies of field, "foo", set
     * repeat to 20 and the actual generated batches will contain fields
     * foo1, foo2, ... foo20.
     */

    public Integer repeat;
    public Map<String,Object> properties;

    @JsonCreator
    public MockColumn(@JsonProperty("name") String name,
                      @JsonProperty("type") MinorType minorType,
                      @JsonProperty("mode") DataMode mode,
                      @JsonProperty("width") Integer width,
                      @JsonProperty("precision") Integer precision,
                      @JsonProperty("scale") Integer scale,
                      @JsonProperty("generator") String generator,
                      @JsonProperty("repeat") Integer repeat,
                      @JsonProperty("properties") Map<String,Object> properties) {
      this.name = name;
      this.minorType = minorType;
      this.mode = mode;
      this.width = width;
      this.precision = precision;
      this.scale = scale;
      this.generator = generator;
      this.repeat = repeat;
      this.properties = properties;
    }

    @JsonProperty("type")
    public MinorType getMinorType() { return minorType; }
    public String getName() { return name; }
    public DataMode getMode() { return mode; }
    public Integer getWidth() { return width; }
    public Integer getPrecision() { return precision; }
    public Integer getScale() { return scale; }
    public String getGenerator() { return generator; }
    public Integer getRepeat() { return repeat; }
    @JsonIgnore
    public int getRepeatCount() { return repeat == null ? 1 : repeat; }
    @JsonIgnore
    public int getWidthValue() { return width == null ? 0 : width; }
    public Map<String,Object> getProperties() { return properties; }

    @JsonIgnore
    public MajorType getMajorType() {
      MajorType.Builder b = MajorType.newBuilder();
      b.setMode(mode);
      b.setMinorType(minorType);
      if (precision != null) {
        b.setPrecision(precision);
      }
      if (width != null) {
        //b.setWidth(width); // Legacy
        b.setPrecision(width); // Since DRILL-5419
      }
      if (scale != null) {
        b.setScale(scale);
      }
      return b.build();
    }

    @Override
    public String toString() {
      return "MockColumn [minorType=" + minorType + ", name=" + name + ", mode="
          + mode + "]";
    }
  }

  private String descrip;
  MockTableSelection entries;

  public MockTableDef(@JsonProperty("descrip") final String descrip,
                      @JsonProperty("entries") final MockTableSelection entries) {
    this.descrip = descrip;
    this.entries = entries;
  }

  /**
   * Description of this data source. Ignored by the scanner, purely
   * for the convenience of the author.
   */

  public String getDescrip() { return descrip; }

  /**
   * The set of entries that define the groups within the file. Each
   * group can have a distinct schema; each may be read in a separate
   * fragment.
   * @return
   */

  public MockTableSelection getEntries() { return entries; }
}
