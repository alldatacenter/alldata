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

package org.apache.inlong.sort.base.dirty;

import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.ValidationException;

import java.io.Serializable;
import static org.apache.inlong.sort.base.Constants.DIRTY_IDENTIFIER;
import static org.apache.inlong.sort.base.Constants.DIRTY_IGNORE;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_CONNECTOR;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_ENABLE;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_IGNORE_ERRORS;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_LABELS;
import static org.apache.inlong.sort.base.Constants.DIRTY_SIDE_OUTPUT_LOG_TAG;

/**
 * Dirty common options
 */
public class DirtyOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean ignoreDirty;
    private final boolean enableDirtySideOutput;
    private final boolean ignoreSideOutputErrors;
    private final String dirtyConnector;
    private final String labels;
    private final String logTag;
    private final String identifier;

    private DirtyOptions(boolean ignoreDirty, boolean enableDirtySideOutput, boolean ignoreSideOutputErrors,
            String dirtyConnector, String labels, String logTag, String identifier) {
        this.ignoreDirty = ignoreDirty;
        this.enableDirtySideOutput = enableDirtySideOutput;
        this.ignoreSideOutputErrors = ignoreSideOutputErrors;
        this.dirtyConnector = dirtyConnector;
        this.labels = labels;
        this.logTag = logTag;
        this.identifier = identifier;
    }

    /**
     * Get dirty options from {@link ReadableConfig}
     *
     * @param config The config
     * @return Dirty options
     */
    public static DirtyOptions fromConfig(ReadableConfig config) {
        boolean ignoreDirty = config.get(DIRTY_IGNORE);
        boolean enableDirtySink = config.get(DIRTY_SIDE_OUTPUT_ENABLE);
        boolean ignoreSinkError = config.get(DIRTY_SIDE_OUTPUT_IGNORE_ERRORS);
        String dirtyConnector = config.getOptional(DIRTY_SIDE_OUTPUT_CONNECTOR).orElse(null);
        String labels = config.getOptional(DIRTY_SIDE_OUTPUT_LABELS).orElse(null);
        String logTag = config.get(DIRTY_SIDE_OUTPUT_LOG_TAG);
        String identifier = config.get(DIRTY_IDENTIFIER);
        return new DirtyOptions(ignoreDirty, enableDirtySink, ignoreSinkError,
                dirtyConnector, labels, logTag, identifier);
    }

    public void validate() {
        if (!ignoreDirty || !enableDirtySideOutput) {
            return;
        }
        if (dirtyConnector == null || dirtyConnector.trim().length() == 0) {
            throw new ValidationException(
                    "The option 'dirty.side-output.connector' is not allowed to be empty "
                            + "when the option 'dirty.ignore' is 'true' "
                            + "and the option 'dirty.side-output.enable' is 'true'");
        }
    }

    public boolean ignoreDirty() {
        return ignoreDirty;
    }

    public String getDirtyConnector() {
        return dirtyConnector;
    }

    public String getLabels() {
        return labels;
    }

    public String getLogTag() {
        return logTag;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean ignoreSideOutputErrors() {
        return ignoreSideOutputErrors;
    }

    public boolean enableDirtySideOutput() {
        return enableDirtySideOutput;
    }
}
