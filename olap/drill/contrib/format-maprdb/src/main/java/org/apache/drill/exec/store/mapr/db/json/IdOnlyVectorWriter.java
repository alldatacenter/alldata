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

import static org.apache.drill.exec.store.mapr.PluginErrorHandler.schemaChangeException;
import static org.ojai.DocumentConstants.ID_KEY;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.ojai.Value;

import com.mapr.db.impl.IdCodec;
import com.mapr.db.ojai.DBDocumentReaderBase;

/**
 *  This implementation of DocumentReaderVectorWriter writes only the "_id" field from the OJAI
 *  Document to Drill Value Vectors. This is useful for "_id" only queries.
 */
class IdOnlyVectorWriter extends DocumentReaderVectorWriter {

  protected IdOnlyVectorWriter(final OjaiValueWriter valueWriter) {
    super(valueWriter);
  }

  @Override
  public void writeDBDocument(VectorContainerWriter vectorWriter, DBDocumentReaderBase reader)
      throws SchemaChangeException {
    MapWriter writer = vectorWriter.rootAsMap();

    Value id = reader.getId();
    try {
      switch(id.getType()) {
      case STRING:
        valueWriter.writeString(writer, ID_KEY, id.getString());
        break;
      case BINARY:
        valueWriter.writeBinary(writer, ID_KEY, id.getBinary());
        break;
      default:
        throw new UnsupportedOperationException(id.getType() +
            " is not a supported type for _id field.");
      }
    } catch (IllegalStateException | IllegalArgumentException e) {
      throw schemaChangeException(logger, e, "Possible schema change at _id: '%s'", IdCodec.asString(id));
    }

  }

}
