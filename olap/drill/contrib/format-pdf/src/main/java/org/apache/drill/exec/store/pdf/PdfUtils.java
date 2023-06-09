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

import org.apache.drill.common.exceptions.UserException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.ExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class PdfUtils {

  public static final ExtractionAlgorithm DEFAULT_ALGORITHM = new BasicExtractionAlgorithm();
  private static final Logger logger = LoggerFactory.getLogger(PdfUtils.class);

  /**
   * Returns a list of tables found in a given PDF document.  There are several extraction algorithms
   * available and this function uses the default Basic Extraction Algorithm.
   * @param document The input PDF document to search for tables
   * @return A list of tables found in the document.
   */
  public static List<Table> extractTablesFromPDF(PDDocument document) {
    return extractTablesFromPDF(document, DEFAULT_ALGORITHM);
  }

  /**
   * Returns a list of tables found in a given PDF document.  There are several extraction algorithms
   * available and this function allows the user to select which to use.
   * @param document The input PDF document to search for tables
   * @param algorithm The extraction algorithm
   * @return A list of tables found in the document.
   */
  public static List<Table> extractTablesFromPDF(PDDocument document, ExtractionAlgorithm algorithm) {
    NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();

    ExtractionAlgorithm algExtractor;

    SpreadsheetExtractionAlgorithm extractor = new SpreadsheetExtractionAlgorithm();

    ObjectExtractor objectExtractor = new ObjectExtractor(document);
    PageIterator pages = objectExtractor.extract();
    List<Table> tables= new ArrayList<>();
    while (pages.hasNext()) {
      Page page = pages.next();

      algExtractor = algorithm;
      List<Rectangle> tablesOnPage = detectionAlgorithm.detect(page);

      for (Rectangle guessRect : tablesOnPage) {
        Page guess = page.getArea(guessRect);
        tables.addAll(algExtractor.extract(guess));
      }
    }

    try {
      objectExtractor.close();
    } catch (Exception e) {
      throw UserException.parseError(e)
        .message("Error extracting table: " + e.getMessage())
        .build(logger);
    }

    return tables;
  }

  /**
   * Returns a specific table from a PDF document. Returns null in the event that
   * the user requests a table that does not exist.  If there is an error with the document
   * the function will throw a UserException.
   * @param document The source PDF document
   * @param tableIndex The index of the desired table
   * @return The desired Table, null if the table is not valid, or if the document has no tables.
   */
  public static Table getSpecificTable(PDDocument document, int tableIndex, ExtractionAlgorithm algorithm) {
    NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();
    ExtractionAlgorithm algExtractor;

    if (algorithm == null) {
      algExtractor = DEFAULT_ALGORITHM;
    } else {
      algExtractor = algorithm;
    }

    ObjectExtractor objectExtractor = new ObjectExtractor(document);
    PageIterator pages = objectExtractor.extract();

    Table specificTable;
    int tableCounter = 0;
    while (pages.hasNext()) {
      Page page = pages.next();

      List<Rectangle> rectanglesOnPage = detectionAlgorithm.detect(page);
      List<Table> tablesOnPage = new ArrayList<>();

      for (Rectangle guessRect : rectanglesOnPage) {
        Page guess = page.getArea(guessRect);
        tablesOnPage.addAll(algExtractor.extract(guess));
        if (tablesOnPage.size() == 0) {
          return null;
        }

        for (Table table : tablesOnPage) {
          if (tableCounter == tableIndex) {
            specificTable = table;
            return specificTable;
          }
          tableCounter++;
        }
      }
    }
    try {
      objectExtractor.close();
    } catch (Exception e) {
      throw UserException.parseError(e)
        .message("Error extracting table: " + e.getMessage())
        .build(logger);
    }

    return null;
  }

  /**
   * Returns the values contained in a PDF Table row
   * @param table The source table
   * @return A list of the header rows
   */
  public static List<String> extractFirstRowValues(Table table) {
    List<String> values = new ArrayList<>();
    if (table == null) {
      return values;
    }
    List<RectangularTextContainer> firstRow = table.getRows().get(0);

    if (firstRow != null) {
      for (RectangularTextContainer rectangularTextContainer : firstRow) {
        values.add(rectangularTextContainer.getText());
      }
    }
    return values;
  }

  /**
   * This function retuns the contents of a specific row in a PDF table as a list of Strings.
   * @param table The table containing the data.
   * @param rowIndex The desired row index
   * @return A list of Strings with the data.
   */
  public static List<String> getRowAsStringList(Table table, int rowIndex) {
    List<String> values = new ArrayList<>();
    if (table == null) {
      return values;
    }

    List<RectangularTextContainer> row = table.getRows().get(rowIndex);
    for (RectangularTextContainer rectangularTextContainer : row) {
      values.add(rectangularTextContainer.getText());
    }
    return values;
  }

  public static List<String> convertRowToStringArray(List<RectangularTextContainer> input) {
    List<String> values = new ArrayList<>();
    for (RectangularTextContainer rectangularTextContainer : input) {
      values.add(rectangularTextContainer.getText());
    }
    return values;
  }


  /**
   * This function retuns the contents of a specific row in a PDF table as a list of Strings.
   * @param table The table containing the data.
   * @param rowIndex The desired row index
   * @return A list of Strings with the data.
   */
  public static List<RectangularTextContainer> getRow(Table table, int rowIndex) {
    List<RectangularTextContainer> values = new ArrayList<>();
    if (table == null) {
      return values;
    }
    return table.getRows().get(rowIndex);
  }
}
