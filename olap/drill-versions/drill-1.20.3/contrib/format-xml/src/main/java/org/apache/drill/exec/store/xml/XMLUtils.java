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

import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

public class XMLUtils {

  /**
   * Empty events are not helpful so this method checks to see if the event consists solely of whitespace
   * or newline characters.  Unfortunately, newlines and other extraneous characters are treated as new elements, so
   * this function wraps a lot of those checks in one function.
   * @param event The input XMLEvent
   * @return True if the XMLEvent is only whitespace, false if not.
   */
  public static boolean isEmptyWhiteSpace(XMLEvent event) {
    if (event.getEventType() == XMLStreamConstants.COMMENT) {
      return true;
    } else if (event.getEventType() != XMLStreamConstants.CHARACTERS) {
      return false;
    }

    String value = event.asCharacters().getData();
    if (Strings.isNullOrEmpty(value.trim())) {
      return true;
    } else {
      return event.asCharacters().isIgnorableWhiteSpace();
    }
  }

  /**
   * Identifies XML events that may be populated but are not useful for extracting data.
   * @param event The XMLEvent in question
   * @return True if the event is useful, false if not
   */
  public static boolean isNotCruft(XMLEvent event) {
    int eventType = event.getEventType();
    return eventType == XMLStreamConstants.CHARACTERS ||
      eventType == XMLStreamConstants.START_ELEMENT ||
      eventType == XMLStreamConstants.END_ELEMENT;
  }

  /**
   * Generates a nested field name by combining a field prefix to the current field name.
   * @param prefix The prefix to be added to the field name.
   * @param field The field name
   * @return the prefix, followed by an underscore and the fieldname.
   */
  public static String addField(String prefix, String field) {
    if (Strings.isNullOrEmpty(prefix)) {
      return field;
    }
    return prefix + "_" + field;
  }

  /**
   * Returns the field name from nested field names.
   * @param fieldName The nested field name
   * @return The field name
   */
  public static String removeField(String prefix, String fieldName) {
    if (fieldName == null) {
      return "";
    }

    int index = prefix.lastIndexOf(fieldName);
    if (index <= 0) {
      return "";
    } else {
      return prefix.substring(0, index - 1);
    }
  }

  /**
   * Returns true if a given XMLEvent has attributes, false if not. Since only
   * start elements can by definition have attributes, returns false if the event
   * is not a start element.
   * @param event The XMLEvent in question
   * @return True if the XMLEvent has attributes, false if not.
   */
  public static boolean hasAttributes(XMLEvent event) {
    if (! event.isStartElement()) {
      return false;
    } else {
      return event.asStartElement().getAttributes().hasNext();
    }
  }
}
