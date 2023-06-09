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

package org.apache.drill.exec.store.xml;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.store.ImplicitColumnUtils.ImplicitColumns;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class XMLReader implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(XMLReader.class);
  private static final String ATTRIBUTE_MAP_NAME = "attributes";

  private final Stack<String> fieldNameStack;
  private final Stack<TupleWriter> rowWriterStack;
  private final int dataLevel;
  private final int maxRecords;
  private final Map<String, XMLMap> nestedMapCollection;

  private TupleWriter attributeWriter;
  private CustomErrorContext errorContext;
  private RowSetLoader rootRowWriter;
  private int currentNestingLevel;
  private XMLEvent currentEvent;
  private String rootDataFieldName;
  private String fieldName;
  private xmlState currentState;
  private TupleWriter currentTupleWriter;
  private boolean rowStarted;
  private String attributePrefix;
  private String fieldValue;
  private InputStream fsStream;
  private XMLEventReader reader;
  private ImplicitColumns metadata;
  private boolean isSelfClosingEvent;

  /**
   * This field indicates the various states in which the reader operates. The names should be self-explanatory,
   * but they are used as the reader iterates over the XML tags to know what to do.
   */
  private enum xmlState {
    ROW_STARTED,
    POSSIBLE_MAP,
    NESTED_MAP_STARTED,
    GETTING_DATA,
    WRITING_DATA,
    FIELD_ENDED,
    ROW_ENDED
  }

  public XMLReader(InputStream fsStream, int dataLevel, int maxRecords) throws XMLStreamException {
    this.fsStream = fsStream;
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    reader = inputFactory.createXMLEventReader(fsStream);
    fieldNameStack = new Stack<>();
    rowWriterStack = new Stack<>();
    nestedMapCollection = new HashMap<>();
    this.dataLevel = dataLevel;
    this.maxRecords = maxRecords;
    isSelfClosingEvent = false;
  }

  public void open(RowSetLoader rootRowWriter, CustomErrorContext errorContext ) {
    this.errorContext = errorContext;
    this.rootRowWriter = rootRowWriter;
    attributeWriter = getAttributeWriter();
  }

  public boolean next() {
    while (!rootRowWriter.isFull()) {
      try {
        if (!processElements()) {
          return false;
        }
      } catch (Exception e) {
        throw UserException
          .dataReadError(e)
          .message("Error parsing file: " + e.getMessage())
          .addContext(errorContext)
          .build(logger);
      }
    }
    return true;
  }


  public void close() {
    if (fsStream != null) {
      AutoCloseables.closeSilently(fsStream);
      fsStream = null;
    }

    if (reader != null) {
      try {
        reader.close();
      } catch (XMLStreamException e) {
        logger.warn("Error when closing XML stream: {}", e.getMessage());
      }
      reader = null;
    }
  }

  /**
   * This function processes the XML elements.  This function stops reading when the
   * limit (if any) which came from the query has been reached or the Iterator runs out of
   * elements.
   * @return True if there are more elements to parse, false if not
   */
  private boolean processElements() {
    XMLEvent nextEvent;

    if (!reader.hasNext()) {
      // Stop reading if there are no more results
      return false;
    } else if (rootRowWriter.limitReached(maxRecords)) {
      // Stop if the query limit has been reached
      return false;
    }

    // Iterate over XML events
    while (reader.hasNext()) {
      // get the current event
      try {
        nextEvent = reader.nextEvent();

        // If the next event is whitespace, newlines, or other cruft that we don't need,
        // ignore the event and move to the next event
        if (XMLUtils.isEmptyWhiteSpace(nextEvent)) {
          continue;
        }

        // Reset the self-closing tag flag.
        isSelfClosingEvent = isSelfClosingEvent(currentEvent, nextEvent);
        if (isSelfClosingEvent) {
          logger.debug("Found self closing event!!");
        }

        // Capture the previous and current event
        XMLEvent lastEvent = currentEvent;
        currentEvent = nextEvent;

        // Process the event
        processEvent(currentEvent, lastEvent);
      } catch (XMLStreamException e) {
        throw UserException
          .dataReadError(e)
          .message("Error parsing XML file: " + e.getMessage())
          .addContext(errorContext)
          .build(logger);
      }
    }
    return true;
  }

  /**
   * One of the challenges with XML parsing are self-closing events. The streaming XML parser
   * treats self-closing events as two events: a start event and an ending event.  The issue is that
   * the self-closing events can cause schema issues with Drill specifically, if a self-closing event
   * is detected prior to a non-self-closing event, and that populated event contains a map or other nested data
   * Drill will throw a schema change exception.
   *
   * Since Drill uses Java's streaming XML parser, unfortunately, it does not provide a means of identifying
   * self-closing tags.  This function does that by comparing the event with the previous event and looking for
   * a condition where one event is a start and the other is an ending event.  Additionally, the column number and
   * character offsets must be the same, indicating that the two events are the same.
   *
   * @param e1 The first XMLEvent
   * @param e2 The second XMLEvent
   * @return True if the events represent a self-closing event, false if not.
   */
  private boolean isSelfClosingEvent(XMLEvent e1, XMLEvent e2) {
    // If either event is null return false.
    if (e1 == null || e2 == null) {
      return false;
    } else if (XMLUtils.hasAttributes(e1) || XMLUtils.hasAttributes(e2)) {
      return false;
    }

    return (e1.getLocation().getCharacterOffset() == e2.getLocation().getCharacterOffset()) &&
      (e1.getLocation().getColumnNumber() == e2.getLocation().getColumnNumber()) &&
      e1.isStartElement() && e2.isEndElement();
  }

  /**
   * This function processes an actual XMLEvent. There are three possibilities:
   * 1.  The event is a start event
   * 2.  The event contains text
   * 3.  The event is a closing tag
   * There are other possible elements, but they are not relevant for our purposes.
   *
   * @param currentEvent The current event to be processed
   * @param lastEvent The previous event which was processed
   */
  private void processEvent(XMLEvent currentEvent,
                            XMLEvent lastEvent) {
    String mapName;
    switch (currentEvent.getEventType()) {

      /*
       * This case handles start elements.
       * Case 1:  The current nesting level is less than the data level.
       * In this case, increase the nesting level and stop processing.
       *
       * Case 2: The nesting level is higher than the data level.
       * In this case, a few things must happen.
       * 1.  We capture the field name
       * 2.  If the row has not started, we start the row
       * 3.  Set the possible map flag
       * 4.  Process attributes
       * 5.  Push both the field name and writer to the stacks
       */
      case XMLStreamConstants.START_ELEMENT:
        currentNestingLevel++;

        // Case 1: Current nesting level is less than the data level
        if (currentNestingLevel < dataLevel) {
          // Stop here if the current level of nesting has not reached the data.
          break;
        }

        StartElement startElement = currentEvent.asStartElement();
        // Get the field name
        fieldName = startElement.getName().getLocalPart();

        if (rootDataFieldName == null && currentNestingLevel == dataLevel) {
          rootDataFieldName = fieldName;
          logger.debug("Root field name: {}", rootDataFieldName);
        }

        if (!rowStarted) {
          currentTupleWriter = startRow(rootRowWriter);
        } else {
          if (lastEvent != null &&
            lastEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
            /*
             * Check the flag in the next section.  If the next element is a character AND the flag is set,
             * start a map.  If not... ignore it all.
             */
            changeState(xmlState.POSSIBLE_MAP);
            rowWriterStack.push(currentTupleWriter);
          }

          fieldNameStack.push(fieldName);
          if (currentNestingLevel > dataLevel) {
            attributePrefix = XMLUtils.addField(attributePrefix, fieldName);
          }

          Iterator<Attribute> attributes = startElement.getAttributes();
          if (attributes != null && attributes.hasNext()) {
            writeAttributes(attributePrefix, attributes);
          }
        }
        break;

      /*
       * This case processes character elements.
       */
      case XMLStreamConstants.CHARACTERS:
        /*
         * This is the case for comments or other characters after a closing tag
         */
        if (currentState == xmlState.ROW_ENDED) {
          break;
        }

        // Get the field value but ignore characters outside of rows
        if (rowStarted) {
          if (currentState == xmlState.POSSIBLE_MAP && currentNestingLevel > dataLevel +1) {
            changeState(xmlState.NESTED_MAP_STARTED);

            // Remove the current field name from the stack
            if (fieldNameStack.size() > 1) {
              fieldNameStack.pop();
            }
            // Get the map name and push to stack
            mapName = fieldNameStack.pop();
            currentTupleWriter = getMapWriter(mapName, currentTupleWriter);
          } else {
            changeState(xmlState.ROW_STARTED);
          }
        }

        // Get the field value
        fieldValue = currentEvent.asCharacters().getData().trim();
        changeState(xmlState.GETTING_DATA);
        break;

      case XMLStreamConstants.END_ELEMENT:
        currentNestingLevel--;

        if (isSelfClosingEvent) {
          logger.debug("Closing self-closing event {}. ", fieldName);
          isSelfClosingEvent = false;
          attributePrefix = XMLUtils.removeField(attributePrefix,fieldName);
          break;
        }

        if (currentNestingLevel < dataLevel - 1) {
          break;
        } else if (currentEvent.asEndElement().getName().toString().compareTo(rootDataFieldName) == 0) {
          // End the row
          currentTupleWriter = endRow();

          // Clear stacks
          rowWriterStack.clear();
          fieldNameStack.clear();
          attributePrefix = "";

        } else if (currentState == xmlState.FIELD_ENDED && currentNestingLevel >= dataLevel) {
          // Case to end nested maps
          // Pop tupleWriter off stack
          if (rowWriterStack.size() > 0) {
            currentTupleWriter = rowWriterStack.pop();
          }
          // Pop field name
          if (fieldNameStack.size() > 0) {
            fieldNameStack.pop();
          }

          attributePrefix = XMLUtils.removeField(attributePrefix,fieldName);

        } else if (currentState != xmlState.ROW_ENDED) {
          if ( !isSelfClosingEvent) {
            writeFieldData(fieldName, fieldValue, currentTupleWriter);
          }
          // Clear out field name and value
          attributePrefix = XMLUtils.removeField(attributePrefix, fieldName);

          // Pop field name
          if (fieldNameStack.size() > 0) {
            fieldNameStack.pop();
          }
          fieldName = null;
          fieldValue = null;
        }
        break;
    }
  }

  public void implicitFields(ImplicitColumns metadata) {
    this.metadata = metadata;
  }

  private TupleWriter startRow(RowSetLoader writer) {
    if (currentNestingLevel == dataLevel) {
      rootRowWriter.start();
      rowStarted = true;
      rowWriterStack.push(rootRowWriter);
      changeState(xmlState.ROW_STARTED);
      return rootRowWriter;
    } else {
      rowStarted = false;
      return writer;
    }
  }

  /**
   * This method executes the steps to end a row from an XML dataset.
   * @return the root row writer
   */
  private TupleWriter endRow() {
    logger.debug("Ending row");
    if (metadata != null) {
      metadata.writeImplicitColumns();
    }
    rootRowWriter.save();
    rowStarted = false;
    changeState(xmlState.ROW_ENDED);
    return rootRowWriter;
  }

  /**
   * Writes a field. If the field does not have a corresponding ScalarWriter, this method will
   * create one.
   * @param fieldName The field name
   * @param fieldValue The field value to be written
   * @param writer The TupleWriter which represents
   */
  private void writeFieldData(String fieldName, String fieldValue, TupleWriter writer) {
    if (fieldName == null) {
      return;
    }

    changeState(xmlState.WRITING_DATA);

    // Find the TupleWriter object
    int index = writer.tupleSchema().index(fieldName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL);
      index = writer.addColumn(colSchema);
    }
    ScalarWriter colWriter = writer.scalar(index);
    if (fieldValue != null && (currentState != xmlState.ROW_ENDED && currentState != xmlState.FIELD_ENDED)) {
      colWriter.setString(fieldValue);
      changeState(xmlState.FIELD_ENDED);
    }
  }

  /**
   * Writes a attribute. If the field does not have a corresponding ScalarWriter, this method will
   * create one.
   * @param fieldName The field name
   * @param fieldValue The field value to be written
   * @param writer The TupleWriter which represents
   */
  private void writeAttributeData(String fieldName, String fieldValue, TupleWriter writer) {
    if (fieldName == null) {
      return;
    }

    // Find the TupleWriter object
    int index = writer.tupleSchema().index(fieldName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL);
      index = writer.addColumn(colSchema);
    }
    ScalarWriter colWriter = writer.scalar(index);
    if (fieldValue != null) {
      colWriter.setString(fieldValue);
    }
  }

  /**
   * Returns a MapWriter for a given field.  If the writer does not exist, add one to the schema
   * @param mapName The Map's name
   * @param rowWriter The current TupleWriter
   * @return A TupleWriter of the new map
   */
  private TupleWriter getMapWriter(String mapName, TupleWriter rowWriter) {
    logger.debug("Adding map: {}", mapName);
    int index = rowWriter.tupleSchema().index(mapName);
    if (index == -1) {
      // Check to see if the map already exists in the map collection
      // This condition can occur in deeply nested data.
      String tempFieldName = mapName + "-" + currentNestingLevel;
      XMLMap mapObject = nestedMapCollection.get(tempFieldName);
      if (mapObject != null) {
        logger.debug("Found map {}", tempFieldName);
        return mapObject.getMapWriter();
      }

      index = rowWriter.addColumn(SchemaBuilder.columnSchema(mapName, MinorType.MAP, DataMode.REQUIRED));
      // Add map to map collection for future use
      nestedMapCollection.put(tempFieldName, new XMLMap(mapName, rowWriter.tuple(index)));
    }
    logger.debug("Index: {}, Fieldname: {}", index, mapName);
    return rowWriter.tuple(index);
  }

  private void changeState(xmlState newState) {
    xmlState previousState = currentState;
    currentState = newState;
  }

  private TupleWriter getAttributeWriter() {
    int attributeIndex = rootRowWriter.addColumn(SchemaBuilder.columnSchema(ATTRIBUTE_MAP_NAME, MinorType.MAP, DataMode.REQUIRED));
    return rootRowWriter.tuple(attributeIndex);
  }

  /**
   * Helper function which writes attributes of an XML element.
   * @param prefix The attribute prefix
   * @param attributes An iterator of Attribute objects
   */
  private void writeAttributes(String prefix, Iterator<Attribute> attributes) {
    while (attributes.hasNext()) {
      Attribute currentAttribute = attributes.next();
      String key = prefix + "_" + currentAttribute.getName().toString();
      writeAttributeData(key, currentAttribute.getValue(), attributeWriter);
    }
  }
}
