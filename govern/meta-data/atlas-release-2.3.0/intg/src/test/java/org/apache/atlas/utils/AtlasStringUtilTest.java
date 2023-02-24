/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.utils;

import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class AtlasStringUtilTest {
    @Test
    public void testGetOption() {
        // null options
        assertNull(AtlasStringUtil.getOption(null, "opt"));

        // null optionName
        assertNull(AtlasStringUtil.getOption(Collections.emptyMap(), null));

        // explicit null value for the option
        assertNull(AtlasStringUtil.getOption(Collections.singletonMap("opt", null), null));

        // option not present
        assertNull(AtlasStringUtil.getOption(Collections.emptyMap(), "opt"));
        assertNull(AtlasStringUtil.getOption(Collections.singletonMap("opt", "val"), "opt1"));

        // option present
        assertEquals(AtlasStringUtil.getOption(Collections.singletonMap("opt", "val"), "opt"), "val");
        assertNotEquals(AtlasStringUtil.getOption(Collections.singletonMap("opt", "val"), "opt"), "val1");
    }

    @Test
    public void testGetBooleanOption() {
        // null options
        assertFalse(AtlasStringUtil.getBooleanOption(null, "opt"));

        // null optionName
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), null));

        // explicit null value for the option
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", null), "opt"));

        // option not present
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), "opt"));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt1"));

        // invalid value for the option
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt"));

        // option present
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "false"), "opt"));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "true"), "opt"));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "TRUE"), "opt"));
    }

    @Test
    public void testGetBooleanOptionWithDefault() {
        // null options
        assertTrue(AtlasStringUtil.getBooleanOption(null, "opt", true));
        assertFalse(AtlasStringUtil.getBooleanOption(null, "opt", false));

        // null optionName
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), null, true));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), null, false));

        // explicit null value for the option
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", null), "opt", false));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", null), "opt", true));

        // option not present
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), "opt", true));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.emptyMap(), "opt", false));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt1", true));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt1", false));

        // invalid value for the option
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt", true));
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "val"), "opt", false));

        // valid value for the option
        assertFalse(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "false"), "opt", true));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "true"), "opt", true));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "TRUE"), "opt", true));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "true"), "opt", false));
        assertTrue(AtlasStringUtil.getBooleanOption(Collections.singletonMap("opt", "TRUE"), "opt", false));
    }

    @Test
    public void testOptionEquals() {
        // null options
        assertTrue(AtlasStringUtil.optionEquals(null, "opt", null));
        assertFalse(AtlasStringUtil.optionEquals(null, "opt", "val"));

        // null optionName
        assertTrue(AtlasStringUtil.optionEquals(Collections.emptyMap(), null, null));

        // explicit null value for the option
        assertTrue(AtlasStringUtil.optionEquals(Collections.singletonMap("opt", null), "opt", null));

        // option not present
        assertTrue(AtlasStringUtil.optionEquals(Collections.emptyMap(), "opt", null));
        assertTrue(AtlasStringUtil.optionEquals(Collections.singletonMap("opt", "val"), "opt1", null));

        // option present
        assertTrue(AtlasStringUtil.optionEquals(Collections.singletonMap("opt", "val"), "opt", "val"));
        assertFalse(AtlasStringUtil.optionEquals(Collections.singletonMap("opt", "val"), "opt", "val1"));
    }
}
