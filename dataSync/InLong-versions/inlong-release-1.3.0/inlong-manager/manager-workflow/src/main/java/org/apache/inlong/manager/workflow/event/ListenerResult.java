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

package org.apache.inlong.manager.workflow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Listener execution result
 */
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListenerResult {

    /**
     * Whether the execution was successful
     */
    private boolean success;

    /**
     * Remarks on execution results
     */
    private String remark;

    /**
     * Exception information
     */
    private Exception exception;

    /**
     * Listener execute success.
     */
    public static ListenerResult success() {
        return ListenerResult.builder().success(true).build();
    }

    /**
     * Listener execute success
     *
     * @param remark remarks on execution results.
     * @return listener execute success info.
     */
    public static ListenerResult success(String remark) {
        return ListenerResult.builder().success(true).remark(remark).build();
    }

    /**
     * Listener execute fail.
     */
    public static ListenerResult fail() {
        return ListenerResult.builder().success(false).build();
    }

    /**
     * Listener execute fail.
     *
     * @param remark remarks on execution results.
     * @return listener execute fail info.
     */
    public static ListenerResult fail(String remark) {
        return ListenerResult.builder().success(false).remark(remark).build();
    }

    /**
     * Listener execute fail.
     *
     * @param exception exception on execution results.
     * @return listener execute fail info.
     */
    public static ListenerResult fail(Exception exception) {
        return ListenerResult.builder().success(false).exception(exception).build();
    }

    /**
     * Listener execute fail.
     *
     * @param exception exception on execution results.
     * @param remark remarks on execution results.
     * @return listener execute fail info.
     */
    public static ListenerResult fail(Exception exception, String remark) {
        return ListenerResult.builder().success(false).exception(exception).remark(remark).build();
    }

}
