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

package org.apache.inlong.sort.protocol.transformation;

import com.google.common.base.Preconditions;
import org.apache.inlong.sort.SerializeBaseTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Function test base class
 */
public abstract class FunctionBaseTest extends SerializeBaseTest<Function> {

    private String expectFormat;

    public abstract String getExpectFormat();

    /**
     * Init the expectFormat, function instance
     */
    @Before
    public void init() {
        super.init();
        this.expectFormat = Preconditions.checkNotNull(getExpectFormat());
    }

    /**
     * Test format in standard sql
     */
    @Test
    public void testFormat() {
        Assert.assertEquals(expectFormat, getTestObject().format());
    }
}
