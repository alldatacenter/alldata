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
 * CSV input serialization format
 */
public class CsvInput extends XmlSerialization {
    private Boolean allowQuotedRecordDelimiter;
    private Character comments;
    private Character fieldDelimiter;
    private String fileHeaderInfo;
    private Character quoteCharacter;
    private Character quoteEscapeCharacter;
    private Character recordDelimiter;

    /**
     * Specifies that CSV fields may contain quoted record delimiters
     * 
     * Default is FALSE
     * 
     * @param allowQuotedRecordDelimiter
     *      Informs when fields may contain record delimiters or not
     * 
     * @return Self
     */
    public CsvInput withAllowQuotedRecordDelimiter(boolean allowQuotedRecordDelimiter) {
        this.allowQuotedRecordDelimiter = allowQuotedRecordDelimiter;
        return this;
    }

    /**
     * Single character at the beginning of a line to discard the row as a comment
     * 
     * Default is none
     * 
     * @param comments
     *      Comment prefix
     * 
     * @return Self
     */
    public CsvInput withComments(char comments) {
        this.comments = comments;
        return this;
    }

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
    public CsvInput withFieldDelimiter(char fieldDelimiter) {
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
    public CsvInput withRecordDelimiter(char recordDelimiter) {
        this.recordDelimiter = recordDelimiter;
        return this;
    }

    /**
     * Describes the first line of input
     * 
     * Default is NONE
     * 
     * @param fileHeaderInfo
     *      Content and usage of first line of input
     * 
     * @return Self
     */
    public CsvInput withFileHeaderInfo(FileHeaderInfo fileHeaderInfo) {
        this.fileHeaderInfo = fileHeaderInfo.toString();
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
    public CsvInput withQuoteCharacter(char quoteCharacter) {
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
    public CsvInput withQuoteEscapeCharacter(char quoteEscapeCharacter) {
        this.quoteEscapeCharacter = quoteEscapeCharacter;
        return this;
    }

    /**
     * Formats the input settings into the XML request
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    @Override
    public void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder csvBuilder = xmlBuilder.elem("CSV");
        if (allowQuotedRecordDelimiter != null) {
            csvBuilder.elem("allowQuotedRecordDelimiter").text(allowQuotedRecordDelimiter.toString());
        }
        if (comments != null) {
            csvBuilder.elem("Comments").text(charToString(comments));
        }
        if (fieldDelimiter != null) {
            csvBuilder.elem("FieldDelimiter").text(charToString(fieldDelimiter));
        }
        if (recordDelimiter != null) {
            csvBuilder.elem("RecordDelimiter").text(charToString(recordDelimiter));
        }
        if (fileHeaderInfo != null) {
            csvBuilder.elem("FileHeaderInfo").text(fileHeaderInfo);
        }
        if (quoteCharacter != null) {
            csvBuilder.elem("QuoteCharacter").text(charToString(quoteCharacter));
        }
        if (quoteEscapeCharacter != null) {
            csvBuilder.elem("QuoteEscapeCharacter").text(charToString(quoteEscapeCharacter));
        }
    }
}
