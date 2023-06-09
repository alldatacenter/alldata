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
package org.apache.drill.exec.record.metadata.schema;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestSchemaProvider extends BaseTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testInlineProviderExists() throws Exception {
    SchemaProvider provider = new InlineSchemaProvider("(i int)");
    assertTrue(provider.exists());
  }

  @Test
  public void testInlineProviderDelete() throws Exception {
    SchemaProvider provider = new InlineSchemaProvider("(i int)");
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Schema deletion is not supported");
    provider.delete();
  }

  @Test
  public void testInlineProviderStore() throws Exception {
    SchemaProvider provider = new InlineSchemaProvider("(i int)");
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("Schema storage is not supported");
    provider.store("i int", null, StorageProperties.builder().build());
  }

  @Test
  public void testInlineProviderRead() throws Exception {
    SchemaProvider provider = new InlineSchemaProvider("(i int) properties { 'k1' = 'v1' }");

    SchemaContainer schemaContainer = provider.read();
    assertNotNull(schemaContainer);

    assertNull(schemaContainer.getTable());
    TupleMetadata metadata = schemaContainer.getSchema();
    assertNotNull(metadata);
    assertEquals(1, metadata.size());
    assertEquals(TypeProtos.MinorType.INT, metadata.metadata("i").type());

    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("k1", "v1");
    assertEquals(properties, metadata.properties());

    SchemaContainer.Version version = schemaContainer.getVersion();
    assertFalse(version.isUndefined());
    assertEquals(SchemaContainer.Version.CURRENT_DEFAULT_VERSION, version.getValue());
  }

  @Test
  public void testInlineProviderWithoutColumns() throws Exception {
    SchemaProvider provider = new InlineSchemaProvider("() properties { 'k1' = 'v1' }");
    SchemaContainer schemaContainer = provider.read();
    assertNotNull(schemaContainer);
    TupleMetadata metadata = schemaContainer.getSchema();
    assertTrue(metadata.isEmpty());
    assertEquals("v1", metadata.property("k1"));
  }

  @Test
  public void testPathProviderExists() throws Exception {
    File schema = new File(folder.getRoot(), "schema");
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schema.getPath()));
    assertFalse(provider.exists());

    assertTrue(schema.createNewFile());
    assertTrue(provider.exists());
  }

  @Test
  public void testPathProviderDelete() throws Exception {
    File schema = folder.newFile("schema");
    assertTrue(schema.exists());
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schema.getPath()));
    provider.delete();
    assertFalse(schema.exists());
  }

  @Test
  public void testPathProviderDeleteAbsentFile() throws Exception {
    File schema = new File(folder.getRoot(), "absent_file");
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schema.getPath()));
    assertFalse(schema.exists());
    provider.delete();
    assertFalse(schema.exists());
  }

  @Test
  public void testPathProviderStore() throws Exception {
    File schema = new File(folder.getRoot(), "schema");
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schema.getPath()));

    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("k1", "v1");
    properties.put("k2", "v2");

    assertFalse(provider.exists());
    provider.store("i int, v varchar(10), s struct<s1 int, s2 varchar>", properties, StorageProperties.builder().build());
    assertTrue(provider.exists());

    String expectedContent = "{\n"
      + "  \"schema\" : {\n"
      + "    \"type\" : \"tuple_schema\",\n"
      + "    \"columns\" : [\n"
      + "      {\n"
      + "        \"name\" : \"i\",\n"
      + "        \"type\" : \"INT\",\n"
      + "        \"mode\" : \"OPTIONAL\"\n"
      + "      },\n"
      + "      {\n"
      + "        \"name\" : \"v\",\n"
      + "        \"type\" : \"VARCHAR(10)\",\n"
      + "        \"mode\" : \"OPTIONAL\"\n"
      + "      },\n"
      + "      {\n"
      + "        \"name\" : \"s\",\n"
      + "        \"type\" : \"STRUCT<`s1` INT, `s2` VARCHAR>\",\n"
      + "        \"mode\" : \"REQUIRED\"\n"
      + "      }\n"
      + "    ],\n"
      + "    \"properties\" : {\n"
      + "      \"k1\" : \"v1\",\n"
      + "      \"k2\" : \"v2\"\n"
      + "    }\n"
      + "  },\n"
      + "  \"version\" : 1\n"
      + "}";
    List<String> lines = Files.readAllLines(schema.toPath());
    assertEquals(expectedContent, String.join("\n", lines));
  }

  @Test
  public void testPathProviderStoreInExistingFile() throws Exception {
    File schemaFile = folder.newFile("schema");
    org.apache.hadoop.fs.Path schema = new org.apache.hadoop.fs.Path(schemaFile.getPath());
    SchemaProvider provider = new PathSchemaProvider(schema);
    assertTrue(provider.exists());

    thrown.expect(IOException.class);
    thrown.expectMessage("File already exists");

    provider.store("i int", null, StorageProperties.builder().build());
  }

  @Test
  public void testPathProviderStoreInExistingFileOverwrite() throws Exception {
    File schemaFile = folder.newFile("schema");
    org.apache.hadoop.fs.Path schema = new org.apache.hadoop.fs.Path(schemaFile.getPath());
    SchemaProvider provider = new PathSchemaProvider(schema);
    assertTrue(provider.exists());

    StorageProperties storageProperties = StorageProperties.builder()
      .overwrite()
      .build();
    provider.store("i int", null, storageProperties);

    TupleMetadata metadata = provider.read().getSchema();
    assertEquals(1, metadata.size());
  }

  @Test
  public void testPathProviderRead() throws Exception {
    Path schemaPath = folder.newFile("schema").toPath();
    String schema = "{\n"
      + "  \"table\" : \"tbl\",\n"
      + "  \"schema\" : {\n"
      + "    \"columns\" : [\n"
      + "      {\n"
      + "        \"name\" : \"i\",\n"
      + "        \"type\" : \"INT\",\n"
      + "        \"mode\" : \"REQUIRED\",\n"
      + "        \"properties\" : {\n"
      + "          \"drill.default\" : \"10\"\n"
      + "        }\n"
      + "      },\n"
      + "      {\n"
      + "        \"name\" : \"a\",\n"
      + "        \"type\" : \"ARRAY<VARCHAR(10)>\",\n"
      + "        \"mode\" : \"REPEATED\",\n"
      + "        \"properties\" : {\n"
      + "          \"ck1\" : \"cv1\",\n"
      + "          \"ck2\" : \"cv2\"\n"
      + "        }\n"
      + "      },\n"
      + "      {\n"
      + "        \"name\" : \"t\",\n"
      + "        \"type\" : \"DATE\",\n"
      + "        \"mode\" : \"OPTIONAL\",\n"
      + "        \"properties\" : {\n"
      + "          \"drill.format\" : \"yyyy-mm-dd\"\n"
      + "        }\n"
      + "      }\n"
      + "    ],\n"
      + "    \"properties\" : {\n"
      + "      \"sk1\" : \"sv1\",\n"
      + "      \"sk2\" : \"sv2\"\n"
      + "    }\n"
      + "  }\n"
      + "}";
    Files.write(schemaPath, Collections.singletonList(schema));
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schemaPath.toUri().getPath()));
    assertTrue(provider.exists());
    SchemaContainer schemaContainer = provider.read();
    assertNotNull(schemaContainer);
    assertEquals("tbl", schemaContainer.getTable());

    TupleMetadata metadata = schemaContainer.getSchema();
    assertNotNull(metadata);
    Map<String, String> schemaProperties = new LinkedHashMap<>();
    schemaProperties.put("sk1", "sv1");
    schemaProperties.put("sk2", "sv2");
    assertEquals(schemaProperties, metadata.properties());

    assertEquals(3, metadata.size());

    ColumnMetadata i = metadata.metadata("i");
    assertEquals(TypeProtos.MinorType.INT, i.type());
    assertEquals(TypeProtos.DataMode.REQUIRED, i.mode());
    assertEquals(10, i.decodeDefaultValue());

    ColumnMetadata a = metadata.metadata("a");
    assertEquals(TypeProtos.MinorType.VARCHAR, a.type());
    assertEquals(TypeProtos.DataMode.REPEATED, a.mode());
    Map<String, String> columnProperties = new LinkedHashMap<>();
    columnProperties.put("ck1", "cv1");
    columnProperties.put("ck2", "cv2");
    assertEquals(columnProperties, a.properties());

    ColumnMetadata t = metadata.metadata("t");
    assertEquals(TypeProtos.MinorType.DATE, t.type());
    assertEquals(TypeProtos.DataMode.OPTIONAL, t.mode());
    assertEquals("yyyy-mm-dd", t.format());

    assertTrue(schemaContainer.getVersion().isUndefined());
  }

  @Test
  public void testPathProviderReadAbsentFile() throws Exception {
    org.apache.hadoop.fs.Path schema = new org.apache.hadoop.fs.Path(new File(folder.getRoot(), "absent_file").getPath());
    SchemaProvider provider = new PathSchemaProvider(schema);
    assertFalse(provider.exists());

    thrown.expect(FileNotFoundException.class);

    provider.read();
  }

  @Test
  public void testPathProviderReadSchemaWithComments() throws Exception {
    Path schemaPath = folder.newFile("schema").toPath();
    String schema =  "// my schema file start\n" +
      "{\n"
      + "  \"schema\" : {\n"
      + "    \"columns\" : [ // start columns list\n"
      + "      {\n"
      + "        \"name\" : \"i\",\n"
      + "        \"type\" : \"INT\",\n"
      + "        \"mode\" : \"OPTIONAL\"\n"
      + "      }\n"
      + "    ]\n"
      + "  }\n"
      + "}"
      + "// schema file end\n"
      + "/* multiline comment */";
    Files.write(schemaPath, Collections.singletonList(schema));

    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schemaPath.toUri().getPath()));
    assertTrue(provider.exists());
    assertNotNull(provider.read());
  }

  @Test
  public void testPathProviderWithoutColumns() throws Exception {
    Path schemaPath = folder.newFile("schema").toPath();
    String schema = "{\n"
      + "  \"table\" : \"tbl\",\n"
      + "  \"schema\" : {\n"
      + "    \"properties\" : {\n"
      + "      \"prop\" : \"val\"\n"
      + "    }\n"
      + "  }\n"
      + "}";
    Files.write(schemaPath, Collections.singletonList(schema));
    SchemaProvider provider = new PathSchemaProvider(new org.apache.hadoop.fs.Path(schemaPath.toUri().getPath()));
    SchemaContainer schemaContainer = provider.read();
    assertNotNull(schemaContainer);
    TupleMetadata tableMetadata = schemaContainer.getSchema();
    assertTrue(tableMetadata.isEmpty());
    assertEquals("val", tableMetadata.property("prop"));
  }

}
