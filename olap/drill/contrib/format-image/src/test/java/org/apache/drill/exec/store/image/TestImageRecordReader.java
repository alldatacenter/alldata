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
package org.apache.drill.exec.store.image;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.singleMap;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.QueryTestUtil;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestImageRecordReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get("image/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("bmp"), false, false, null));
    String sql = "select * from dfs.`image/*.bmp`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    assertEquals(1, sets.rowCount());
    sets.clear();
  }

  @Test
  public void testExplicitQuery() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("jpg"), false, false, null));
    String sql = "select Format, PixelWidth, HasAlpha, `XMP` from dfs.`image/withExifAndIptc.jpg`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("Format", MinorType.VARCHAR)
        .addNullable("PixelWidth", MinorType.INT)
        .addNullable("HasAlpha", MinorType.BIT)
        .addMap("XMP")
          .addNullable("XMPValueCount", MinorType.INT)
          .addMap("Photoshop")
            .addNullable("CaptionWriter", MinorType.VARCHAR)
            .addNullable("Headline", MinorType.VARCHAR)
            .addNullable("AuthorsPosition", MinorType.VARCHAR)
            .addNullable("Credit", MinorType.VARCHAR)
            .addNullable("Source", MinorType.VARCHAR)
            .addNullable("City", MinorType.VARCHAR)
            .addNullable("State", MinorType.VARCHAR)
            .addNullable("Country", MinorType.VARCHAR)
            .addNullable("Category", MinorType.VARCHAR)
            .addNullable("DateCreated", MinorType.VARCHAR)
            .addNullable("Urgency", MinorType.VARCHAR)
            .addArray("SupplementalCategories", MinorType.VARCHAR)
            .resumeMap()
          .addMap("XmpBJ")
            .addMapArray("JobRef")
              .addNullable("Name", MinorType.VARCHAR)
              .resumeMap()
            .resumeMap()
          .addMap("XmpMM")
            .addNullable("DocumentID", MinorType.VARCHAR)
            .addNullable("InstanceID", MinorType.VARCHAR)
            .resumeMap()
          .addMap("XmpRights")
            .addNullable("WebStatement", MinorType.VARCHAR)
            .addNullable("Marked", MinorType.VARCHAR)
            .resumeMap()
          .addMap("Dc")
            .addNullable("Description", MinorType.VARCHAR)
            .addArray("Creator", MinorType.VARCHAR)
            .addNullable("Title", MinorType.VARCHAR)
            .addNullable("Rights", MinorType.VARCHAR)
            .addArray("Subject", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("JPEG", 600, false,
          mapValue(25,
          mapValue("Ian Britton", "Communications", "Photographer", "Ian Britton", "FreeFoto.com", " ", " ", "Ubited Kingdom", "BUS", "2002-06-20", "5", strArray("Communications")),
          singleMap(mapArray(mapValue("Photographer"))),
          mapValue("adobe:docid:photoshop:84d4dba8-9b11-11d6-895d-c4d063a70fb0", "uuid:3ff5d382-9b12-11d6-895d-c4d063a70fb0"),
          mapValue("www.freefoto.com", "True"), mapValue("Communications", strArray("Ian Britton"), "Communications", "ian Britton - FreeFoto.com", strArray("Communications"))))
        .build();

    assertEquals(1, sets.rowCount());
    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("mp4"), false, false, null));
    String sql = "select * from dfs.`image/*.mp4` limit 1";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    assertEquals(1, sets.rowCount());
    sets.clear();
  }

  @Test
  public void testSerDe() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("jpg"), false, false, null));
    String sql = "select count(*) from dfs.`image/*.jpg`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();

    assertEquals("Counts should match", 2, cnt);
  }

  @Test
  public void testExplicitQueryWithCompressedFile() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("jpg"), false, false, null));
    QueryTestUtil.generateCompressedFile("image/LearningApacheDrill.jpg", "zip", "store/image/LearningApacheDrill.jpg.zip");
    String sql = "select Format, PixelWidth, PixelHeight, `FileType` from dfs.`store/image/LearningApacheDrill.jpg.zip`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("Format", MinorType.VARCHAR)
        .addNullable("PixelWidth", MinorType.INT)
        .addNullable("PixelHeight", MinorType.INT)
        .addMap("FileType")
          .addNullable("DetectedFileTypeName", MinorType.VARCHAR)
          .addNullable("DetectedFileTypeLongName", MinorType.VARCHAR)
          .addNullable("DetectedMIMEType", MinorType.VARCHAR)
          .addNullable("ExpectedFileNameExtension", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("JPEG", 800, 800, mapValue("JPEG", "Joint Photographic Experts Group", "image/jpeg", "jpg"))
        .build();

    assertEquals(1, sets.rowCount());
    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testFileSystemMetadataOption() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("gif"), true, false, null));
    String sql = "select FileSize, Format, PixelWidth, PixelHeight, ColorMode, BitsPerPixel,"
        + " Orientation, DPIWidth, DPIHeight, HasAlpha, Duration, VideoCodec, FrameRate, AudioCodec,"
        + " AudioSampleSize, AudioSampleRate from dfs.`image/*.gif`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("FileSize", MinorType.BIGINT)
        .addNullable("Format", MinorType.VARCHAR)
        .addNullable("PixelWidth", MinorType.INT)
        .addNullable("PixelHeight", MinorType.INT)
        .addNullable("ColorMode", MinorType.VARCHAR)
        .addNullable("BitsPerPixel", MinorType.INT)
        .addNullable("Orientation", MinorType.INT)
        .addNullable("DPIWidth", MinorType.FLOAT8)
        .addNullable("DPIHeight", MinorType.FLOAT8)
        .addNullable("HasAlpha", MinorType.BIT)
        .addNullable("Duration", MinorType.BIGINT)
        .addNullable("VideoCodec", MinorType.VARCHAR)
        .addNullable("FrameRate", MinorType.FLOAT8)
        .addNullable("AudioCodec", MinorType.VARCHAR)
        .addNullable("AudioSampleSize", MinorType.INT)
        .addNullable("AudioSampleRate", MinorType.FLOAT8)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(10463, "GIF", 128, 174, "Indexed", 8, 0, 0.0, 0.0, true, 0, "Unknown", 0.0, "Unknown", 0, 0.0)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testTimeZoneOption() throws Exception {
    cluster.defineFormat("dfs", "image", new ImageFormatConfig(Arrays.asList("psd"), true, false, "UTC"));
    String sql = "select ExifIFD0 from dfs.`image/*.psd`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addMap("ExifIFD0")
          .addNullable("Orientation", MinorType.INT)
          .addNullable("XResolution", MinorType.FLOAT8)
          .addNullable("YResolution", MinorType.FLOAT8)
          .addNullable("ResolutionUnit", MinorType.INT)
          .addNullable("Software", MinorType.VARCHAR)
          .addNullable("DateTime", MinorType.TIMESTAMP)
          .resumeSchema()
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(singleMap(mapValue(1, 72.009, 72.009, 2, "Adobe Photoshop CS2 Windows", Instant.ofEpochMilli(1454717337000L))))
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }
}