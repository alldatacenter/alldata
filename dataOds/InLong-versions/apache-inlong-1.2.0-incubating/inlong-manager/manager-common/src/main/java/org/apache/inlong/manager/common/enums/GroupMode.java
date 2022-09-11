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

package org.apache.inlong.manager.common.enums;

import lombok.Getter;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;

/**
 * Mode of inlong group
 */
public enum GroupMode {
    /**
     * Normal group init with all components in Inlong Cluster
     * StreamSource -> Agent/SDK -> DataProxy -> Cache -> Sort -> StreamSink
     */
    NORMAL("normal"),

    /**
     * Light group init with sort in Inlong Cluster
     * StreamSource -> Sort -> StreamSink
     */
    LIGHT("light");

    @Getter
    private final String mode;

    GroupMode(String mode) {
        this.mode = mode;
    }

    public static GroupMode forMode(String mode) {
        for (GroupMode groupMode : values()) {
            if (groupMode.getMode().equals(mode)) {
                return groupMode;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported group mode=%s", mode));
    }

    public static GroupMode parseGroupMode(InlongGroupInfo groupInfo) {
        if (groupInfo.getLightweight() != null && groupInfo.getLightweight() == 1) {
            return GroupMode.LIGHT;
        }
        return GroupMode.NORMAL;
    }
}
