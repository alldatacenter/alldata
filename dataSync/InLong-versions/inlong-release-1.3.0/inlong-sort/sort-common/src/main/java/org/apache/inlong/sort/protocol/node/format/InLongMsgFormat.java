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
import org.apache.inlong.common.msg.InLongMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link InLongMsg} format.
 *
 * @see <a href="https://inlong.apache.org/docs/development/inlong_msg">InLongMsg Format</a>
 */
@JsonTypeName("inLongMsgFormat")
@Data
public class InLongMsgFormat implements Format {

    private static final long serialVersionUID = 1L;

    @JsonProperty(value = "innerFormat")
    private Format innerFormat;

    @JsonProperty(value = "ignoreParseErrors", defaultValue = "false")
    private Boolean ignoreParseErrors;

    @JsonCreator
    public InLongMsgFormat(@JsonProperty(value = "innerFormat") Format innerFormat,
            @JsonProperty(value = "ignoreParseErrors", defaultValue = "false") Boolean ignoreParseErrors) {
        this.innerFormat = innerFormat;
        this.ignoreParseErrors = ignoreParseErrors;
    }

    public InLongMsgFormat() {
        this(new CsvFormat(), false);
    }

    @JsonIgnore
    @Override
    public String getFormat() {
        return "inlong-msg";
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
        options.put("inlong-msg.inner.format", innerFormat.getFormat());
        innerFormat.generateOptions().entrySet()
                .stream()
                .filter(entry -> !"format".equals(entry.getKey()))
                .forEach(entry -> options.put("inlong-msg." + entry.getKey(), entry.getValue()));
        if (this.ignoreParseErrors != null) {
            options.put("inlong-msg.ignore-parse-errors", this.ignoreParseErrors.toString());
        }

        return options;
    }
}
