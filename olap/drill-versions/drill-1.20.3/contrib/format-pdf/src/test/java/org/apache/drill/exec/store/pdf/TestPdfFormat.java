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

package org.apache.drill.exec.store.pdf;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.test.QueryTestUtil;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.time.LocalDate;

import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(RowSetTests.class)
public class TestPdfFormat extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("pdf/"));
  }

  @Test
  public void testStarQuery() throws RpcException {
    String sql = "SELECT * FROM cp.`pdf/argentina_diputados_voting_record.pdf` WHERE `Provincia` = 'Rio Negro'";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Apellido y Nombre", MinorType.VARCHAR)
      .addNullable("Bloque político", MinorType.VARCHAR)
      .addNullable("Provincia", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("ALBRIEU, Oscar Edmundo Nicolas", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("AVOSCAN, Herman Horacio", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("CEJAS, Jorge Alberto", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitQuery() throws RpcException {
    String sql = "SELECT `Apellido y Nombre`, `Bloque político`, `Provincia`, `field_0` " +
      "FROM cp.`pdf/argentina_diputados_voting_record.pdf` WHERE `Provincia` = 'Rio Negro'";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Apellido y Nombre", MinorType.VARCHAR)
      .addNullable("Bloque político", MinorType.VARCHAR)
      .addNullable("Provincia", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("ALBRIEU, Oscar Edmundo Nicolas", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("AVOSCAN, Herman Horacio", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("CEJAS, Jorge Alberto", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testFullScan() throws Exception {
    String sql = "SELECT * " +
      "FROM table(cp.`pdf/argentina_diputados_voting_record.pdf` " +
      "(type => 'pdf', combinePages => false, extractHeaders => false))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    assertEquals(31, results.rowCount());
    results.clear();

    sql = "SELECT * " +
      "FROM table(cp.`pdf/argentina_diputados_voting_record.pdf` " +
      "(type => 'pdf', combinePages => false, extractHeaders => true))";

    results = client.queryBuilder().sql(sql).rowSet();
    assertEquals(31,results.rowCount());
    results.clear();
  }

  @Test
  public void testEncryptedFile() throws Exception {
    String sql = "SELECT * " +
      "FROM table(cp.`pdf/encrypted.pdf` " +
      "(type => 'pdf', combinePages => false, extractHeaders => true, password => 'userpassword'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("FLA Audit Profile", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("Country", "China")
      .addRow("Factory name", "01001523B")
      .addRow("IEM", "BVCPS (HK), Shen Zhen Office")
      .addRow("Date of audit", "May 20-22, 2003")
      .addRow("PC(s)", "adidas-Salomon")
      .addRow("Number of workers", "243")
      .addRow("Product(s)", "Scarf, cap, gloves, beanies and headbands")
      .addRow("Production processes", "Sewing, cutting, packing, embroidery, die-cutting")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testNoHeaders() throws RpcException {
    String sql = "SELECT * " +
      "FROM table(cp.`pdf/argentina_diputados_voting_record.pdf` " +
      "(type => 'pdf', combinePages => false, extractHeaders => false)) WHERE field_2 = 'Rio Negro'";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("field_0", MinorType.VARCHAR)
      .addNullable("field_1", MinorType.VARCHAR)
      .addNullable("field_2", MinorType.VARCHAR)
      .addNullable("field_3", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("ALBRIEU, Oscar Edmundo Nicolas", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("AVOSCAN, Herman Horacio", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("CEJAS, Jorge Alberto", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testMetadataQuery() throws RpcException {
    String sql = "SELECT _page_count, " +
      "_title, " +
      "_author, " +
      "_subject, " +
      "_keywords, " +
      "_creator, " +
      "_producer," +
      "_creation_date, " +
      "_modification_date, " +
      "_trapped " +
      "FROM cp.`pdf/20.pdf` " +
      "LIMIT 1";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("_page_count", MinorType.INT)
      .addNullable("_title", MinorType.VARCHAR)
      .addNullable("_author", MinorType.VARCHAR)
      .addNullable("_subject", MinorType.VARCHAR)
      .addNullable("_keywords", MinorType.VARCHAR)
      .addNullable("_creator", MinorType.VARCHAR)
      .addNullable("_producer", MinorType.VARCHAR)
      .addNullable("_creation_date", MinorType.TIMESTAMP)
      .addNullable("_modification_date", MinorType.TIMESTAMP)
      .addNullable("_trapped", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, "Agricultural Landuse Survey in The Sumas River Watershed Summa",
        "Vision", "Agricultural Landuse Survey in The Sumas River Watershed Summa",
        "Agricultural Landuse Survey in The Sumas River Watershed Summa",
        "PScript5.dll Version 5.2.2",
        "Acrobat Distiller 7.0.5 (Windows)",
        857403000000L,
        1230835135000L,
        null)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testUnicode() throws Exception {
    String sql = "SELECT * FROM cp.`pdf/arabic.pdf`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("مرحباً", MinorType.VARCHAR)
      .addNullable("اسمي سلطان", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("انا من ولاية كارولينا الشمال", "من اين انت؟")
      .addRow( "1234", "عندي 47 قطط")
      .addRow("هل انت شباك؟", "اسمي Jeremy في الانجليزية")
      .addRow("Jeremy is جرمي in Arabic", null)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) AS cnt FROM " +
      "table(cp.`pdf/argentina_diputados_voting_record.pdf` (type => 'pdf', combinePages => false))";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match",31L, cnt);
  }

  @Test
  public void testPageMerge() throws Exception {
    String sql = "SELECT * FROM table(cp.`pdf/schools.pdf` (type => 'pdf', combinePages => true, extractHeaders=> true))";
    QuerySummary results = client.queryBuilder().sql(sql).run();
    assertEquals(221, results.recordCount());
  }

  @Test
  public void testFileWithNoTables() throws Exception {
    String sql = "SELECT * FROM table(cp.`pdf/labor.pdf` (type => 'pdf', extractionAlgorithm => 'spreadsheet'))";
    QuerySummary results = client.queryBuilder().sql(sql).run();
    assertEquals(1,results.recordCount());
  }

  @Test
  public void testMetadataQueryWithFileWithNoTables() throws RpcException {
    String sql = "SELECT _page_count, " +
      "_title, " +
      "_author, " +
      "_subject, " +
      "_keywords, " +
      "_creator, " +
      "_producer," +
      "_creation_date, " +
      "_modification_date, " +
      "_trapped " +
      "FROM table(cp.`pdf/labor.pdf` (type => 'pdf', extractionAlgorithm => 'spreadsheet')) LIMIT 1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("_page_count", MinorType.INT)
      .addNullable("_title", MinorType.VARCHAR)
      .addNullable("_author", MinorType.VARCHAR)
      .addNullable("_subject", MinorType.VARCHAR)
      .addNullable("_keywords", MinorType.VARCHAR)
      .addNullable("_creator", MinorType.VARCHAR)
      .addNullable("_producer", MinorType.VARCHAR)
      .addNullable("_creation_date", MinorType.TIMESTAMP)
      .addNullable("_modification_date", MinorType.TIMESTAMP)
      .addNullable("_trapped", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, null, null, null, null, "pdftk 2.01 - www.pdftk.com",
        "itext-paulo-155 (itextpdf.sf.net-lowagie.com)",
        QueryTestUtil.ConvertDateToLong("2015-04-25T23:09:47Z"),
        QueryTestUtil.ConvertDateToLong("2015-04-25T23:09:47Z"), null)
    .build();
    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExtractionAlgorithms() throws Exception {

    String sql = "SELECT * FROM table(cp.`pdf/schools.pdf` (type => 'pdf', combinePages => true, extractionAlgorithm => 'spreadsheet'))";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("field_0", MinorType.VARCHAR)
      .addNullable("Last Name", MinorType.VARCHAR)
      .addNullable("First Name", MinorType.VARCHAR)
      .addNullable("Address", MinorType.VARCHAR)
      .addNullable("City", MinorType.VARCHAR)
      .addNullable("State", MinorType.VARCHAR)
      .addNullable("Zip", MinorType.VARCHAR)
      .addNullable("Occupation", MinorType.VARCHAR)
      .addNullable("Employer", MinorType.VARCHAR)
      .addNullable("Date", MinorType.VARCHAR)
      .addNullable("Amount", MinorType.VARCHAR)
      .buildSchema();

    assertTrue(results.schema().isEquivalent(expectedSchema));
    assertEquals(216, results.rowCount());
    results.clear();

    sql = "SELECT * FROM table(cp.`pdf/schools.pdf` (type => 'pdf', combinePages => true, extractionAlgorithm => 'basic'))";
    results = client.queryBuilder().sql(sql).rowSet();

    expectedSchema = new SchemaBuilder()
      .addNullable("Last Name", MinorType.VARCHAR)
      .addNullable("First Name Address", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .addNullable("City", MinorType.VARCHAR)
      .addNullable("State", MinorType.VARCHAR)
      .addNullable("Zip", MinorType.VARCHAR)
      .addNullable("field_1", MinorType.VARCHAR)
      .addNullable("Occupation Employer", MinorType.VARCHAR)
      .addNullable("Date", MinorType.VARCHAR)
      .addNullable("field_2", MinorType.VARCHAR)
      .addNullable("Amount", MinorType.VARCHAR)
      .buildSchema();

    assertTrue(results.schema().isEquivalent(expectedSchema));
    assertEquals(221, results.rowCount());
    results.clear();
  }

  @Test
  public void testProvidedSchema() throws Exception {
    String sql = "SELECT * FROM table(cp.`pdf/schools.pdf` (type => 'pdf', combinePages => true, " +
      "schema => 'inline=(`Last Name` VARCHAR, `First Name Address` VARCHAR, `field_0` VARCHAR, `City` " +
      "VARCHAR, `State` VARCHAR, `Zip` VARCHAR, `field_1` VARCHAR, `Occupation Employer` VARCHAR, " +
      "`Date` VARCHAR, `field_2` DATE properties {`drill.format` = `M/d/yyyy`}, `Amount` DOUBLE)')) " +
      "LIMIT 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Last Name", MinorType.VARCHAR)
      .addNullable("First Name Address", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .addNullable("City", MinorType.VARCHAR)
      .addNullable("State", MinorType.VARCHAR)
      .addNullable("Zip", MinorType.VARCHAR)
      .addNullable("field_1", MinorType.VARCHAR)
      .addNullable("Occupation Employer", MinorType.VARCHAR)
      .addNullable("Date", MinorType.VARCHAR)
      .addNullable("field_2", MinorType.DATE)
      .addNullable("Amount", MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("Lidstad", "Dick & Peg 62 Mississippi River Blvd N", null, "Saint Paul", "MN", null, "55104", "retired", null, LocalDate.parse("2012-10-12"), 60.0)
      .addRow("Strom", "Pam 1229 Hague Ave", null, "St. Paul", "MN", null, "55104", null, null, LocalDate.parse("2012-09-12"), 60.0)
      .addRow("Seeba", "Louise & Paul 1399 Sheldon St", null, "Saint Paul", "MN", null, "55108", "BOE City of Saint Paul", null, LocalDate.parse("2012-10-12"), 60.0)
      .addRow("Schumacher / Bales", "Douglas L. / Patricia 948 County Rd. D W", null, "Saint Paul", "MN", null, "55126", null, null, LocalDate.parse("2012-10-13"), 60.0)
      .addRow("Abrams", "Marjorie 238 8th St east", null, "St Paul", "MN", null, "55101", "Retired Retired", null, LocalDate.parse("2012-08-08"), 75.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSpecificTable() throws Exception {
    String sql = "SELECT COUNT(*) FROM table(cp.`pdf/schools.pdf` (type => 'pdf', defaultTableIndex => 3))";
    long resultCount = client.queryBuilder().sql(sql).singletonLong();
    assertEquals(45L, resultCount);
  }

  @Test
  public void testWithCompressedFile() throws Exception {
    generateCompressedFile("pdf/argentina_diputados_voting_record.pdf", "zip", "pdf/compressed.pdf.zip" );

    String sql = "SELECT * FROM dfs.`pdf/compressed.pdf.zip` WHERE `Provincia` = 'Rio Negro'";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Apellido y Nombre", MinorType.VARCHAR)
      .addNullable("Bloque político", MinorType.VARCHAR)
      .addNullable("Provincia", MinorType.VARCHAR)
      .addNullable("field_0", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("ALBRIEU, Oscar Edmundo Nicolas", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("AVOSCAN, Herman Horacio", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .addRow("CEJAS, Jorge Alberto", "Frente para la Victoria - PJ", "Rio Negro", "AFIRMATIVO")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }
}
