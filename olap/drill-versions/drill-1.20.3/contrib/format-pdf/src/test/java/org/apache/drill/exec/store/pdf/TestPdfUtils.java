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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;
import technology.tabula.Table;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class TestPdfUtils {

  private static final String DATA_PATH = "src/test/resources/pdf/";

  @Test
  public void testTableExtractor() throws Exception {
    PDDocument document = getDocument("argentina_diputados_voting_record.pdf");
    List<Table> tableList = PdfUtils.extractTablesFromPDF(document);
    document.close();
    assertEquals(tableList.size(), 1);

    PDDocument document2 = getDocument("twotables.pdf");
    List<Table> tableList2 = PdfUtils.extractTablesFromPDF(document2);
    document2.close();
    assertEquals(tableList2.size(), 2);
  }

  @Test
  public void testTableExtractorWithNoBoundingFrame() throws Exception {
    PDDocument document = getDocument("spreadsheet_no_bounding_frame.pdf");
    List<Table> tableList = PdfUtils.extractTablesFromPDF(document);
    document.close();
    assertEquals(tableList.size(), 1);
  }

  @Test
  public void testTableExtractorWitMultipage() throws Exception {
    PDDocument document = getDocument("us-020.pdf");
    List<Table> tableList = PdfUtils.extractTablesFromPDF(document);
    document.close();
    assertEquals(tableList.size(), 4);
  }

  @Test
  public void testGetSpecificTable() throws Exception {
    PDDocument document = getDocument("us-020.pdf");
    Table table = PdfUtils.getSpecificTable(document, 0, null);
    assertNotNull(table);
    assertEquals(7, table.getColCount());
  }

  @Test
  public void testGetFullPageSpecificTable() throws Exception {
    PDDocument document = getDocument("schools.pdf");
    Table table = PdfUtils.getSpecificTable(document, 3, null);
    assertNotNull(table);
  }

  @Test
  public void testGetSpecificTableOutSideOfBounds() throws Exception {
    PDDocument document = getDocument("us-020.pdf");
    Table table = PdfUtils.getSpecificTable(document, 4, null);
    assertNull(table);
  }

  @Test
  public void testFirstRowExtractor() throws Exception {
    PDDocument document = getDocument("schools.pdf");
    List<Table> tableList = PdfUtils.extractTablesFromPDF(document);
    document.close();

    List<String> values = PdfUtils.extractFirstRowValues(tableList.get(0));
    assertEquals(values.size(), 11);
  }

  private PDDocument getDocument(String fileName) throws Exception {
    return PDDocument.load(new File(DATA_PATH + fileName));
  }
}
