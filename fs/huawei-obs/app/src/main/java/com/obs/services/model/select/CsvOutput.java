/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

import com.obs.services.internal.xml.OBSXMLBuilder;

/**
 * CSV output serialization format
 */
public class CsvOutput extends XmlSerialization {
    Character fieldDelimiter;
    Character quoteCharacter;
    Character quoteEscapeCharacter;
    String quoteFields;
    Character recordDelimiter;

    /**
     * Single character as field delimiter
     * 
     * Default is a comma (,)
     * 
     * @param fieldDelimiter
     *      Field delimiter
     * 
     * @return Self
     */
    public CsvOutput withFieldDelimiter(char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
        return this;
    }

    /**
     * Single character as record delimiter
     * 
     * Default is a newline character (\n)
     * 
     * @param recordDelimiter
     *      Record delimiter
     * 
     * @return Self
     */
    public CsvOutput withRecordDelimiter(char recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
        return this;
    }

    /**
     * Escape character to quote a field delimiter inside a value
     * 
     * Default is a double quote character (")
     * 
     * @param quoteCharacter
     *      Quote character
     * 
     * @return Self
     */
    public CsvOutput withQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
        return this;
    }

    /**
     * Escape character for escaping the quotation mark inside an already escaped value
     * 
     * Default is undefined
     * 
     * @param quoteEscapeCharacter
     *      Quote escape character
     * 
     * @return Self
     */
    public CsvOutput withQuoteEscapeCharacter(char quoteEscapeCharacter) {
        this.quoteEscapeCharacter = quoteEscapeCharacter;
        return this;
    }

    /**
     * Indicates whether to quote fields
     * 
     * Default is undefined (never)
     * 
     * @param quoteFields
     *      Expected field quotes
     * 
     * @return Self
     */
    public CsvOutput withQuoteFields(QuoteFields quoteFields) {
        this.quoteFields = quoteFields.toString();
        return this;
    }

    /**
     * Formats the output settings into the XML request
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    @Override
    public void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder csvBuilder = xmlBuilder.elem("CSV");
        if (fieldDelimiter != null) {
            csvBuilder.elem("FieldDelimiter").text(charToString(fieldDelimiter));
        }
        if (recordDelimiter != null) {
            csvBuilder.elem("RecordDelimiter").text(charToString(recordDelimiter));
        }
        if (quoteCharacter != null) {
            csvBuilder.elem("QuoteCharacter").text(charToString(quoteCharacter));
        }
        if (quoteFields != null) {
            csvBuilder.elem("QuoteFields").text(quoteFields);
        }
        if (quoteEscapeCharacter != null) {
            csvBuilder.elem("QuoteEscapeCharacter").text(charToString(quoteEscapeCharacter));
        }
    }
}
