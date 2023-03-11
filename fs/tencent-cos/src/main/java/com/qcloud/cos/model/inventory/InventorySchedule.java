/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */
package com.qcloud.cos.model.inventory;

import java.io.Serializable;

/**
 * Schedule for generating inventory results.
 */
public class InventorySchedule implements Serializable {

    /** Specifies how frequently inventory results are produced. */
    private String frequency;

    /**
     * Returns the frequency for producing inventory results
     * in {@link String} format.
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * Sets the frequency for producing inventory results.
     */
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    /**
     * Sets the frequency for producing inventory results.
     */
    public void setFrequency(InventoryFrequency frequency) {
        setFrequency(frequency == null ? (String) null : frequency.toString());
    }

    /**
     * Sets the frequency for producing inventory results
     * and returns {@link InventorySchedule} object
     * for method chaining.
     */
    public InventorySchedule withFrequency(String frequency) {
        setFrequency(frequency);
        return this;
    }

    /**
     * Sets the frequency for producing inventory results
     * and returns {@link InventorySchedule} object
     * for method chaining.
     */
    public InventorySchedule withFrequency(InventoryFrequency frequency) {
        setFrequency(frequency);
        return this;
    }
}
