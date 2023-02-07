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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.ExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.util.Collections;
import java.util.List;
import java.util.Objects;


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = PdfFormatConfig.PdfFormatConfigBuilder.class)
@JsonTypeName(PdfFormatPlugin.DEFAULT_NAME)
public class PdfFormatConfig implements FormatPluginConfig {

  private static final Logger logger = LoggerFactory.getLogger(PdfFormatConfig.class);

  @JsonProperty
  private final List<String> extensions;

  @JsonProperty
  private final boolean combinePages;

  @JsonProperty
  private final boolean extractHeaders;

  @JsonProperty
  private final String extractionAlgorithm;

  @JsonProperty
  private final String password;

  @JsonProperty
  private final int defaultTableIndex;

  private PdfFormatConfig(PdfFormatConfig.PdfFormatConfigBuilder builder) {
    this.extensions = builder.extensions == null ? Collections.singletonList("pdf") : ImmutableList.copyOf(builder.extensions);
    this.combinePages = builder.combinePages;
    this.extractHeaders = builder.extractHeaders;
    this.defaultTableIndex = builder.defaultTableIndex;
    this.extractionAlgorithm = builder.extractionAlgorithm;
    this.password = builder.password;
  }

  public static PdfFormatConfigBuilder builder() {
    return new PdfFormatConfigBuilder();
  }

  @JsonIgnore
  public PdfBatchReader.PdfReaderConfig getReaderConfig(PdfFormatPlugin plugin) {
    return new PdfBatchReader.PdfReaderConfig(plugin);
  }

  @JsonIgnore
  public ExtractionAlgorithm getAlgorithm() {
    if (StringUtils.isEmpty(this.extractionAlgorithm) || this.extractionAlgorithm.equalsIgnoreCase("basic")) {
      return new BasicExtractionAlgorithm();
    } else if (this.extractionAlgorithm.equalsIgnoreCase("spreadsheet")) {
      return new SpreadsheetExtractionAlgorithm();
    } else {
      throw UserException.validationError()
        .message(extractionAlgorithm + " is not a valid extraction algorithm. The available choices are basic or spreadsheet.")
        .build(logger);
    }
  }

  public List<String> extensions() {
    return this.extensions;
  }

  public boolean combinePages() {
    return this.combinePages;
  }

  public boolean extractHeaders() {
    return this.extractHeaders;
  }

  public String extractionAlgorithm() {
    return this.extractionAlgorithm;
  }

  public String password() {
    return this.password;
  }

  public int defaultTableIndex() {
    return this.defaultTableIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PdfFormatConfig that = (PdfFormatConfig) o;
    return combinePages == that.combinePages
      && extractHeaders == that.extractHeaders
      && defaultTableIndex == that.defaultTableIndex
      && Objects.equals(extensions, that.extensions)
      && Objects.equals(extractionAlgorithm, that.extractionAlgorithm)
      && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, combinePages, extractHeaders, extractionAlgorithm,
      password, defaultTableIndex);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("extensions", extensions)
      .field("combinePages", combinePages)
      .field("extractHeaders", extractHeaders)
      .field("extractionAlgorithm", extractionAlgorithm)
      .field("password", password)
      .field("defaultTableIndex", defaultTableIndex)
      .toString();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class PdfFormatConfigBuilder {
    private List<String> extensions;

    private boolean combinePages;

    private boolean extractHeaders;

    private String extractionAlgorithm;

    private String password;

    private int defaultTableIndex;

    public PdfFormatConfig build() {
      return new PdfFormatConfig(this);
    }

    public PdfFormatConfigBuilder extensions(List<String> extensions) {
      this.extensions = extensions;
      return this;
    }

    public PdfFormatConfigBuilder combinePages(boolean combinePages) {
      this.combinePages = combinePages;
      return this;
    }

    public PdfFormatConfigBuilder extractHeaders(boolean extractHeaders) {
      this.extractHeaders = extractHeaders;
      return this;
    }

    public PdfFormatConfigBuilder extractionAlgorithm(String extractionAlgorithm) {
      this.extractionAlgorithm = extractionAlgorithm;
      return this;
    }

    public PdfFormatConfigBuilder password(String password) {
      this.password = password;
      return this;
    }

    public PdfFormatConfigBuilder defaultTableIndex(int defaultTableIndex) {
      this.defaultTableIndex = defaultTableIndex;
      return this;
    }
  }
}
