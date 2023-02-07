/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.protocol.node.format;

import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;

/**
 * The CSV format.
 *
 * @see <a href="https://nightlies.apache.org/flink/flink-docs-release-1.13/zh/docs/connectors/table/formats/csv/">CSV
 *         Format</a>
 */
@JsonTypeName("csvFormat")
@Data
public class CsvFormat implements Format {

    private static final long serialVersionUID = 1L;

    private static final String IDENTIFIER = "csv";

    @JsonProperty(value = "fieldDelimiter", defaultValue = ",")
    private String fieldDelimiter;
    @JsonProperty(value = "disableQuoteCharacter", defaultValue = "true")
    private Boolean disableQuoteCharacter;
    @JsonProperty(value = "quoteCharacter", defaultValue = "\"")
    private String quoteCharacter;
    @JsonProperty(value = "allowComments", defaultValue = "false")
    private Boolean allowComments;
    @JsonProperty(value = "ignoreParseErrors", defaultValue = "false")
    private Boolean ignoreParseErrors;
    @JsonProperty(value = "arrayElementDelimiter", defaultValue = ";")
    private String arrayElementDelimiter;
    @JsonProperty(value = "escapeCharacter")
    private String escapeCharacter;
    @JsonProperty(value = "nullLiteral")
    private String nullLiteral;

    @JsonCreator
    public CsvFormat(@JsonProperty(value = "fieldDelimiter", defaultValue = ",") String fieldDelimiter,
            @JsonProperty(value = "disableQuoteCharacter", defaultValue = "false") Boolean disableQuoteCharacter,
            @JsonProperty(value = "quoteCharacter", defaultValue = "\"") String quoteCharacter,
            @JsonProperty(value = "allowComments", defaultValue = "false") Boolean allowComments,
            @JsonProperty(value = "ignoreParseErrors", defaultValue = "true") Boolean ignoreParseErrors,
            @JsonProperty(value = "arrayElementDelimiter", defaultValue = ";") String arrayElementDelimiter,
            @JsonProperty(value = "escapeCharacter") String escapeCharacter,
            @JsonProperty(value = "nullLiteral") String nullLiteral) {
        this.fieldDelimiter = fieldDelimiter;
        this.disableQuoteCharacter = disableQuoteCharacter;
        this.quoteCharacter = quoteCharacter;
        this.allowComments = allowComments;
        this.ignoreParseErrors = ignoreParseErrors;
        this.arrayElementDelimiter = arrayElementDelimiter;
        this.escapeCharacter = escapeCharacter;
        this.nullLiteral = nullLiteral;
    }

    @JsonCreator
    public CsvFormat() {
        this(",", true, null, false, true, ";", null, null);
    }

    @JsonCreator
    public CsvFormat(String fieldDelimiter) {
        this(fieldDelimiter, true, null, false, true, ";", null, null);
    }

    @JsonIgnore
    @Override
    public String getFormat() {
        return IDENTIFIER;
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    /**
     * Generate options for connector
     *
     * @return options
     */
    @Override
    public Map<String, String> generateOptions() {
        Map<String, String> options = new HashMap<>(16);
        options.put("format", getFormat());
        options.put("csv.field-delimiter", this.fieldDelimiter);
        options.put("csv.disable-quote-character", this.disableQuoteCharacter.toString());
        // disable quote and quote character cannot appear at the same time
        if (!this.disableQuoteCharacter) {
            options.put("csv.quote-character", this.quoteCharacter);
        }
        if (this.allowComments != null) {
            options.put("csv.allow-comments", this.allowComments.toString());
        }
        if (this.ignoreParseErrors != null) {
            options.put("csv.ignore-parse-errors", this.ignoreParseErrors.toString());
        }
        options.put("csv.array-element-delimiter", this.arrayElementDelimiter);
        if (this.escapeCharacter != null) {
            options.put("csv.escape-character", this.escapeCharacter);
        }
        if (this.nullLiteral != null) {
            options.put("csv.null-literal", this.nullLiteral);
        }
        return options;
    }
}
