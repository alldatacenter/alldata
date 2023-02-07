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

package org.apache.inlong.sort.base.validator;

import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.base.filter.RowKindValidator;
import org.apache.inlong.sort.base.filter.RowValidator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test row kind validator
 */
public class TestRowValidator {

    @Test
    public void testRowKindValidator() {
        RowValidator rowKindValidator = new RowKindValidator("+I&+U&-U");
        Assert.assertTrue(rowKindValidator.validate(RowKind.INSERT));
        Assert.assertFalse(rowKindValidator.validate(RowKind.DELETE));
        Assert.assertTrue(rowKindValidator.validate(RowKind.UPDATE_AFTER));
        Assert.assertTrue(rowKindValidator.validate(RowKind.UPDATE_BEFORE));
    }

    @Test
    public void testRowKindsString() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new RowKindValidator("+I&+U&-"));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RowKindValidator("+I&-I"));
        Assert.assertThrows(IllegalArgumentException.class, () -> new RowKindValidator("+I&+U&"));
    }

}
