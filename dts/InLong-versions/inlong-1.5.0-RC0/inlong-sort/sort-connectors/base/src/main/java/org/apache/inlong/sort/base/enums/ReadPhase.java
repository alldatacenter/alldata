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

package org.apache.inlong.sort.base.enums;

import java.util.stream.Stream;

/**
 * A enum class for metrics read phase
 */
public enum ReadPhase {

    /**
     * This indicator type represents the snapshot phase when the entire database is migrated reading the snapshot data.
     */
    SNAPSHOT_PHASE("snapshot_phase", 1),
    /**
     * This indicator type represents the incremental phase when the entire database is migrated reading the incremental data.
     */
    INCREASE_PHASE("incremental_phase", 2);

    private String phase;
    private int code;

    ReadPhase(String phase, int code) {
        this.phase = phase;
        this.code = code;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    /**
     * get the read phase enum by code
     *
     * @param code the code
     * @return the read phase enum
     */
    public static ReadPhase getReadPhaseByCode(int code) {
        return Stream.of(ReadPhase.values()).filter(v -> v.getCode() == code).findFirst().orElse(null);
    }

    /**
     * get the read phase enum by phase
     *
     * @param phase the phase name
     * @return the read phase enum
     */
    public static ReadPhase getReadPhaseByPhase(String phase) {
        return Stream.of(ReadPhase.values()).filter(v -> v.getPhase().equals(phase)).findFirst().orElse(null);
    }
}