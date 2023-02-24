/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.impexp;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AtlasImportRequest implements Serializable {
    private static final long   serialVersionUID = 1L;

    public  static final String TRANSFORMS_KEY             = "transforms";
    public  static final String TRANSFORMERS_KEY           = "transformers";
    public  static final String OPTION_KEY_REPLICATED_FROM = "replicatedFrom";
    public  static final String OPTION_KEY_MIGRATION_FILE_NAME = "migrationFileName";
    public  static final String OPTION_KEY_MIGRATION       = "migration";
    public  static final String OPTION_KEY_NUM_WORKERS     = "numWorkers";
    public  static final String OPTION_KEY_BATCH_SIZE      = "batchSize";
    public  static final String OPTION_KEY_FORMAT          = "format";
    public  static final String OPTION_KEY_FORMAT_ZIP_DIRECT = "zipDirect";
    public  static final String START_POSITION_KEY         = "startPosition";
    public  static final String UPDATE_TYPE_DEFINITION_KEY = "updateTypeDefinition";
    private static final String START_GUID_KEY             = "startGuid";
    private static final String FILE_NAME_KEY              = "fileName";
    private static final String OPTION_KEY_STREAM_SIZE     = "size";

    private Map<String, String> options;

    public AtlasImportRequest() {
        this.options = new HashMap<>();
    }

    public Map<String, String> getOptions() { return options; }

    public void setOptions(Map<String, String> options) { this.options = options; }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasImportRequest{");
        sb.append("options={");
        AtlasBaseTypeDef.dumpObjects(options, sb);
        sb.append("}");
        sb.append("}");

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @JsonIgnore
    public String getStartGuid() {
        return getOptionForKey(START_GUID_KEY);
    }

    @JsonIgnore
    public String getFileName() {
        return getOptionForKey(FILE_NAME_KEY);
    }

    @JsonIgnore
    public void setFileName(String fileName) {
        setOption(FILE_NAME_KEY, fileName);
    }

    @JsonIgnore
    public String getStartPosition() {
        return getOptionForKey(START_POSITION_KEY);
    }

    @JsonIgnore
    public String getUpdateTypeDefs() {
        return getOptionForKey(UPDATE_TYPE_DEFINITION_KEY);
    }

    private String getOptionForKey(String key) {
        if (MapUtils.isEmpty(this.options) || !this.options.containsKey(key)) {
            return null;
        }

        return this.options.get(key);
    }

    @JsonIgnore
    public boolean isReplicationOptionSet() {
        return MapUtils.isNotEmpty(options) && options.containsKey(OPTION_KEY_REPLICATED_FROM);
    }

    @JsonIgnore
    public String getOptionKeyReplicatedFrom() {
        return isReplicationOptionSet() ? options.get(OPTION_KEY_REPLICATED_FROM) : StringUtils.EMPTY;
    }

    @JsonIgnore
    public int getOptionKeyNumWorkers() {
        return getOptionsValue(OPTION_KEY_NUM_WORKERS, 1);
    }

    @JsonIgnore
    public int getOptionKeyBatchSize() {
        return getOptionsValue(OPTION_KEY_BATCH_SIZE, 1);
    }

    private int getOptionsValue(String optionKeyBatchSize, int defaultValue) {
        String optionsValue = getOptionForKey(optionKeyBatchSize);

        return StringUtils.isEmpty(optionsValue) ?
                defaultValue :
                Integer.valueOf(optionsValue);
    }

    @JsonAnySetter
    public void setOption(String key, String value) {
        if (null == options) {
            options = new HashMap<>();
        }
        options.put(key, value);
    }

    public void setSizeOption(int size) {
        setOption(OPTION_KEY_STREAM_SIZE, Integer.toString(size));
    }

    public int getSizeOption() {
        if (!this.options.containsKey(OPTION_KEY_STREAM_SIZE)) {
            return 1;
        }

        return Integer.valueOf(this.options.get(OPTION_KEY_STREAM_SIZE));
    }
}
