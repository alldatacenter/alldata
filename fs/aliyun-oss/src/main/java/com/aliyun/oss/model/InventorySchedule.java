/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.io.Serializable;

/**
 * Schedule for generating inventory results.
 */
public class InventorySchedule implements Serializable {
    private static final long serialVersionUID = 3682054949992485516L;

    /** Specifies how frequently inventory results are produced. */
    private String frequency;

    /**
     * Gets the frequency for producing inventory results.
     * @return The frequency in {@link String} format.
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * Sets the frequency for producing inventory results.
     *
     * @param frequency
     *            The frequency in {@link String} format.
     */
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    /**
     * Sets the frequency for producing inventory results.
     *
     * @param frequency
     *            The {@link InventoryFrequency} instance.
     */
    public void setFrequency(InventoryFrequency frequency) {
        setFrequency(frequency == null ? (String) null : frequency.toString());
    }

    /**
     * Sets the frequency for producing inventory results.
     * And returns the {@link InventorySchedule} object itself.
     *
     * @param frequency
     *           The frequency in {@link String} format.
     *
     * @return  The {@link InventorySchedule} instance.
     */
    public InventorySchedule withFrequency(String frequency) {
        setFrequency(frequency);
        return this;
    }

    /**
     * Sets the frequency for producing inventory results
     * And returns the {@link InventorySchedule} object itself.
     *
     * @param frequency
     *            The {@link InventoryFrequency} instance.
     *
     * @return  The {@link InventorySchedule} instance.
     */
    public InventorySchedule withFrequency(InventoryFrequency frequency) {
        setFrequency(frequency);
        return this;
    }
}