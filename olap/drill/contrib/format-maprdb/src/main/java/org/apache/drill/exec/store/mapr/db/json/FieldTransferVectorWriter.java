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
package org.apache.drill.exec.store.mapr.db.json;

import static org.apache.drill.exec.store.mapr.PluginErrorHandler.dataReadError;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.vector.complex.impl.MapOrListWriterImpl;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.ojai.DocumentReader.EventType;

import com.mapr.db.ojai.DBDocumentReaderBase;

/**
 *  This implementation of DocumentReaderVectorWriter does field by field transfer the OJAI Document
 *  to Drill Value Vectors.
 */
class FieldTransferVectorWriter extends DocumentReaderVectorWriter {

  protected FieldTransferVectorWriter(final OjaiValueWriter valueWriter) {
    super(valueWriter);
  }

  @Override
  protected void writeDBDocument(VectorContainerWriter vectorWriter, DBDocumentReaderBase reader)
      throws SchemaChangeException {
    MapOrListWriterImpl writer = new MapOrListWriterImpl(vectorWriter.rootAsMap());
    if (reader.next() != EventType.START_MAP) {
      throw dataReadError(logger, "The document did not start with START_MAP!");
    }
    valueWriter.writeToListOrMap(writer, reader);
  }

}
