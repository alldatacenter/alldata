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

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.mock.MockTableDef.MockScanEntry;
import org.apache.drill.exec.store.mock.MockTableDef.MockTableSelection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

public class MockStorageEngine extends AbstractStoragePlugin {

  private final MockStorageEngineConfig configuration;
  private final MockSchema schema;

  public MockStorageEngine(MockStorageEngineConfig configuration, DrillbitContext context, String name) {
    super(context, name);
    this.configuration = configuration;
    this.schema = new MockSchema(this, name);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection, List<SchemaPath> columns)
      throws IOException {

    MockTableSelection tableSelection = selection.getWith(
      new ObjectMapper(),
      MockTableSelection.class
    );
    List<MockScanEntry> readEntries = tableSelection.getEntries();
    assert ! readEntries.isEmpty();
    return new MockGroupScanPOP(null, readEntries);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    parent.add(schema.getName(), schema);
  }

  @Override
  public StoragePluginConfig getConfig() {
    return configuration;
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  /**
   * Resolves table names within the mock data source. Tables can be of two forms:
   * <p>
   * <tt><name>_<n><unit></tt>
   * <p>
   * Where the "name" can be anything, "n" is the number of rows, and "unit" is
   * the units for the row count: non, K (thousand) or M (million).
   * <p>
   * The above form generates a table directly with no other information needed.
   * Column names must be provided, and must be of the form:
   * <p>
   * <tt><name>_<type><size></tt>
   * <p>
   * Where the name can be anything, the type must be i (integer), d (double),
   * b (boolean)
   * or s (string, AKA VarChar). The length is needed only for string fields.
   * <p>
   * Direct tables are quick, but limited. The other option is to provide the
   * name of a definition file:
   * <p>
   * <tt><fileName>.json</tt>
   * <p>
   * In this case, the JSON file must be a resource visible on the class path.
   * Omit the leading slash in the resource path name.
   */

  private static class MockSchema extends AbstractSchema {

    private final MockStorageEngine engine;
    private final Map<String, Table> tableCache = new WeakHashMap<>();

    public MockSchema(MockStorageEngine engine) {
      super(ImmutableList.<String>of(), MockStorageEngineConfig.NAME);
      this.engine = engine;
    }

    public MockSchema(MockStorageEngine engine, String name) {
      super(ImmutableList.<String>of(), name);
      this.engine = engine;
    }

    @Override
    public Table getTable(String name) {
      Table table = tableCache.get(name);
      if (table == null) {
        if (name.toLowerCase().endsWith(".json")) {
          table = getConfigFile(name);
        } else {
          table = getDirectTable(name);
        }
        tableCache.put(name, table);
      }
      return table;
    }

    private Table getConfigFile(String name) {
      final URL url = Resources.getResource(name);
      if (url == null) {
        throw new IllegalArgumentException(
            "Unable to find mock table config file " + name);
      }
      MockTableDef mockTableDefn;
      try {
        String json = Resources.toString(url, Charsets.UTF_8);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mockTableDefn = mapper.readValue(json, MockTableDef.class);
      } catch (JsonParseException e) {
        throw new IllegalArgumentException("Unable to parse mock table definition file: " + name, e);
      } catch (JsonMappingException e) {
        throw new IllegalArgumentException("Unable to Jackson deserialize mock table definition file: " + name, e);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to read mock table definition file: " + name, e);
      }

      return new DynamicDrillTable(engine, this.name, mockTableDefn.getEntries());
    }

    private Table getDirectTable(String name) {
      Pattern p = Pattern.compile("(\\w+)_(\\d+)(k|m)?", Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(name);
      if (! m.matches()) {
        return null;
      }
      @SuppressWarnings("unused")
      String baseName = m.group(1);
      int n = Integer.parseInt(m.group(2));
      String unit = m.group(3);
      if (unit == null) { }
      else if (unit.equalsIgnoreCase("K")) { n *= 1000; }
      else if (unit.equalsIgnoreCase("M")) { n *= 1_000_000; }
      MockScanEntry entry = new MockTableDef.MockScanEntry(n, true, 0, 1, null);
      MockTableSelection entries = new MockTableSelection(ImmutableList.<MockScanEntry>of(entry));
      return new DynamicDrillTable(engine, this.name, entries);
    }

    @Override
    public Set<String> getTableNames() {
      return new HashSet<>();
    }

    @Override
    public String getTypeName() {
      return MockStorageEngineConfig.NAME;
    }
  }
}
