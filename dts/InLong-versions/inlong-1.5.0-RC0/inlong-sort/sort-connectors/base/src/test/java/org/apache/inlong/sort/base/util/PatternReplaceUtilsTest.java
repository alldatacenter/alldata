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

package org.apache.inlong.sort.base.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link PatternReplaceUtils}
 */
public class PatternReplaceUtilsTest {

    @Test
    public void testParseLabels() {
        String labels = "database=${database}&table=${table}";
        Map<String, String> params = new HashMap<>();
        params.put("database", "inlong");
        params.put("table", "student");
        String expected = "database=inlong&table=student";
        Assert.assertEquals(expected, PatternReplaceUtils.replace(labels, params));
    }
}
