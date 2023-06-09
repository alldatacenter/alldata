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

package org.apache.drill.exec.store.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.store.easy.json.loader.JsonLoaderOptions;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonDeserialize(builder = HttpJsonOptions.HttpJsonOptionsBuilder.class)
public class HttpJsonOptions {

  @JsonInclude
  private final Boolean allowNanInf;

  @JsonInclude
  private final Boolean allTextMode;

  @JsonInclude
  private final Boolean readNumbersAsDouble;

  @JsonInclude
  private final Boolean enableEscapeAnyChar;

  HttpJsonOptions(HttpJsonOptionsBuilder builder) {
    this.allowNanInf = builder.allowNanInf;
    this.allTextMode = builder.allTextMode;
    this.readNumbersAsDouble = builder.readNumbersAsDouble;
    this.enableEscapeAnyChar = builder.enableEscapeAnyChar;
  }

  public static HttpJsonOptionsBuilder builder() {
    return new HttpJsonOptionsBuilder();
  }

  @JsonIgnore
  public JsonLoaderOptions getJsonOptions(OptionSet optionSet) {
    JsonLoaderOptions options = new JsonLoaderOptions(optionSet);
    if (allowNanInf != null) {
      options.allowNanInf = allowNanInf;
    }
    if (allTextMode != null) {
      options.allTextMode = allTextMode;
    }
    if (readNumbersAsDouble != null) {
      options.readNumbersAsDouble = readNumbersAsDouble;
    }
    if (enableEscapeAnyChar != null) {
      options.enableEscapeAnyChar = enableEscapeAnyChar;
    }
    return options;
  }

  public Boolean allowNanInf() {
    return this.allowNanInf;
  }

  public Boolean allTextMode() {
    return this.allTextMode;
  }

  public Boolean readNumbersAsDouble() {
    return this.readNumbersAsDouble;
  }

  public Boolean enableEscapeAnyChar() {
    return this.enableEscapeAnyChar;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HttpJsonOptions that = (HttpJsonOptions) o;
    return Objects.equals(allowNanInf, that.allowNanInf)
      && Objects.equals(allTextMode, that.allTextMode)
      && Objects.equals(readNumbersAsDouble, that.readNumbersAsDouble)
      && Objects.equals(enableEscapeAnyChar, that.enableEscapeAnyChar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowNanInf, allTextMode, readNumbersAsDouble, enableEscapeAnyChar);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("allowNanInf", allowNanInf)
      .field("allTextMode", allTextMode)
      .field("readNumbersAsDouble", readNumbersAsDouble)
      .field("enableEscapeAnyChar", enableEscapeAnyChar)
      .toString();
  }

  public static class HttpJsonOptionsBuilder {
    private Boolean allowNanInf;

    private Boolean allTextMode;

    private Boolean readNumbersAsDouble;

    private Boolean enableEscapeAnyChar;

    public HttpJsonOptionsBuilder allowNanInf(Boolean allowNanInf) {
      this.allowNanInf = allowNanInf;
      return this;
    }

    public HttpJsonOptionsBuilder allTextMode(Boolean allTextMode) {
      this.allTextMode = allTextMode;
      return this;
    }

    public HttpJsonOptionsBuilder readNumbersAsDouble(Boolean readNumbersAsDouble) {
      this.readNumbersAsDouble = readNumbersAsDouble;
      return this;
    }

    public HttpJsonOptionsBuilder enableEscapeAnyChar(Boolean enableEscapeAnyChar) {
      this.enableEscapeAnyChar = enableEscapeAnyChar;
      return this;
    }

    public HttpJsonOptions build() {
      return new HttpJsonOptions(this);
    }
  }
}
