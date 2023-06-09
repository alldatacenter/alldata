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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PdfMetadataReader {

  private final Map<String, Object> metadata;
  private final List<PdfBatchReader.PdfColumnWriter> writers;
  private RowSetLoader rowWriter;


  public PdfMetadataReader(PDDocument document) {
    this.writers = new ArrayList<>();
    // We are using a LinkedHashMap to preserve the order
    this.metadata = new LinkedHashMap<>();
    PDDocumentInformation info = document.getDocumentInformation();
    metadata.put("pageCount", document.getNumberOfPages());
    metadata.put("title",info.getTitle());
    metadata.put("author", info.getAuthor());
    metadata.put("subject", info.getSubject());
    metadata.put("keywords", info.getKeywords());
    metadata.put("creator", info.getCreator());
    metadata.put("producer", info.getProducer());
    metadata.put("creationDate", info.getCreationDate());
    metadata.put("modificationDate", info.getModificationDate());
    metadata.put("trapped", info.getTrapped());
  }

  public void setRowWriter(RowSetLoader rowWriter) {
    this.rowWriter = rowWriter;
  }

  public void addImplicitColumnsToSchema() {
    // Add to schema
    addMetadataColumnToSchema("_page_count", MinorType.INT);
    addMetadataColumnToSchema("_title", MinorType.VARCHAR);
    addMetadataColumnToSchema("_author", MinorType.VARCHAR);
    addMetadataColumnToSchema("_subject", MinorType.VARCHAR);
    addMetadataColumnToSchema("_keywords", MinorType.VARCHAR);
    addMetadataColumnToSchema("_creator", MinorType.VARCHAR);
    addMetadataColumnToSchema("_producer", MinorType.VARCHAR);
    addMetadataColumnToSchema("_creation_date", MinorType.TIMESTAMP);
    addMetadataColumnToSchema("_modification_date", MinorType.TIMESTAMP);
    addMetadataColumnToSchema("_trapped", MinorType.VARCHAR);
  }

  public void writeMetadata() {
    int counter = 0;
    for (Object value : metadata.values()) {
      writers.get(counter).loadFromValue(value);
      counter++;
    }
  }

  private void addMetadataColumnToSchema(String columnName, MinorType dataType) {
    int index = rowWriter.tupleSchema().index(columnName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(columnName, dataType, DataMode.OPTIONAL);

      // Exclude from wildcard queries
      colSchema.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
      index = rowWriter.addColumn(colSchema);
    }
    if (dataType == MinorType.VARCHAR) {
      writers.add(new PdfBatchReader.StringPdfColumnWriter(index, columnName, rowWriter));
    } else if (dataType == MinorType.TIMESTAMP) {
      writers.add(new PdfBatchReader.TimestampPdfColumnWriter(index, columnName, rowWriter));
    } else if (dataType == MinorType.INT) {
      writers.add(new PdfBatchReader.IntPdfColumnWriter(index, columnName, rowWriter));
    }
  }
}
