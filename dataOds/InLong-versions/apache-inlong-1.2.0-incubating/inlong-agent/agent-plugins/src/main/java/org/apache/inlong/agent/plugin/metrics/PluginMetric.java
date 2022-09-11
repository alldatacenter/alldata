/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.agent.plugin.metrics;

public interface PluginMetric {

    /**
     * The tag name of plugin metrics.
     */
    String getTagName();

    /**
     *  Increment the count of the read number metric.
     */
    void incReadNum();

    /**
     *  Count of the read number metric.
     */
    long getReadNum();

    /**
     *  Increment the count of the send number metric.
     */
    void incSendNum();

    /**
     *  Count of the send number metric.
     */
    long getSendNum();

    /**
     * Increment the count of the read failed number metric.
     */
    void incReadFailedNum();

    /**
     *  Count of the read failed number metric.
     */
    long getReadFailedNum();

    /**
     * Increment the count of the send failed number metric.
     */
    void incSendFailedNum();

    /**
     *  Count of the send failed number metric.
     */
    long getSendFailedNum();

    /**
     * Increment the count of the read success number metric.
     */
    void incReadSuccessNum();

    /**
     *  Count of the read success number metric.
     */
    long getReadSuccessNum();

    /**
     * Increment the count of the send success number metric.
     */
    void incSendSuccessNum();

    /**
     * Increment the count of the send success number metric.
     *
     * @param delta The increment value to be added.
     */
    void incSendSuccessNum(int delta);

    /**
     *  Count of the send success number metric.
     */
    long getSendSuccessNum();

}
