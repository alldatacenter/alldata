/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.impexp;

import org.apache.atlas.exception.AtlasBaseException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ImportTransformerTest {

    @Test
    public void createWithCorrectParameters() throws AtlasBaseException, IllegalAccessException {
        String param1 = "@cl1";
        String param2 = "@cl2";

        ImportTransformer e = ImportTransformer.getTransformer(String.format("%s:%s:%s", "replace", param1, param2));

        assertTrue(e instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)e).getToFindStr(), param1);
        assertEquals(((ImportTransformer.Replace)e).getReplaceStr(), param2);
    }

    @Test
    public void createSeveralWithCorrectParameters() throws AtlasBaseException, IllegalAccessException {
        String param1 = "@cl1";
        String param2 = "@cl2";

        ImportTransformer e1 = ImportTransformer.getTransformer(String.format("%s:%s:%s", "replace", param1, param2));
        ImportTransformer e2 = ImportTransformer.getTransformer(String.format("replace:tt1:tt2"));

        assertTrue(e1 instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)e1).getToFindStr(), param1);
        assertEquals(((ImportTransformer.Replace)e1).getReplaceStr(), param2);

        assertTrue(e2 instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)e2).getToFindStr(), "tt1");
        assertEquals(((ImportTransformer.Replace)e2).getReplaceStr(), "tt2");
    }

    @Test
    public void createWithDefaultParameters() throws AtlasBaseException {
        ImportTransformer e1 = ImportTransformer.getTransformer("replace:@cl1");
        ImportTransformer e2 = ImportTransformer.getTransformer("replace");

        assertTrue(e1 instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)e1).getToFindStr(), "@cl1");
        assertEquals(((ImportTransformer.Replace)e1).getReplaceStr(), "");

        assertTrue(e2 instanceof ImportTransformer.Replace);
        assertEquals(((ImportTransformer.Replace)e2).getToFindStr(), "");
        assertEquals(((ImportTransformer.Replace)e2).getReplaceStr(), "");
    }

    @Test
    public void applyLowercaseTransformer() throws AtlasBaseException {
        ImportTransformer e = ImportTransformer.getTransformer("lowercase");

        assertEquals(e.apply("@CL1"), "@cl1");
        assertEquals(e.apply("@cl1"), "@cl1");
        assertEquals(e.apply(""), ""); // empty string
        assertEquals(e.apply(null), null); // null value: no change
        assertEquals(e.apply(Integer.valueOf(5)), Integer.valueOf(5)); // non-string value: no change
    }

    @Test
    public void applyUppercaseTransformer() throws AtlasBaseException {
        ImportTransformer e = ImportTransformer.getTransformer("uppercase");

        assertEquals(e.apply("@CL1"), "@CL1");
        assertEquals(e.apply("@cl1"), "@CL1");
        assertEquals(e.apply(""), ""); // empty string
        assertEquals(e.apply(null), null); // null value: no change
        assertEquals(e.apply(Integer.valueOf(5)), Integer.valueOf(5)); // non-string value: no change
    }

    @Test
    public void applyReplaceTransformer1() throws AtlasBaseException {
        ImportTransformer e = ImportTransformer.getTransformer("replace:@cl1:@cl2");

        assertEquals(e.apply("@cl1"), "@cl2");
        assertEquals(e.apply("default@cl1"), "default@cl2");
        assertEquals(e.apply("@cl11"), "@cl21");
        assertEquals(e.apply("@cl2"), "@cl2");
        assertEquals(e.apply(""), ""); // empty string
        assertEquals(e.apply(null), null); // null value
        assertEquals(e.apply(Integer.valueOf(5)), Integer.valueOf(5)); // non-string value: no change
    }

    @Test
    public void applyReplaceTransformer2() throws AtlasBaseException {
        ImportTransformer e = ImportTransformer.getTransformer("replace:@cl1");

        assertEquals(e.apply("@cl1"), "");
        assertEquals(e.apply("default@cl1"), "default");
        assertEquals(e.apply("@cl11"), "1");
        assertEquals(e.apply("@cl2"), "@cl2");
        assertEquals(e.apply(""), ""); // empty string
        assertEquals(e.apply(null), null); // null value
        assertEquals(e.apply(Integer.valueOf(5)), Integer.valueOf(5)); // non-string value: no change
    }

    @Test
    public void applyReplaceTransformer3() throws AtlasBaseException {
        ImportTransformer e = ImportTransformer.getTransformer("replace");

        assertEquals(e.apply("@cl1"), "@cl1");
        assertEquals(e.apply("default@cl1"), "default@cl1");
        assertEquals(e.apply("@cl11"), "@cl11");
        assertEquals(e.apply("@cl2"), "@cl2");
        assertEquals(e.apply(""), ""); // empty string
        assertEquals(e.apply(null), null); // null value
        assertEquals(e.apply(Integer.valueOf(5)), Integer.valueOf(5)); // non-string value: no change
    }
}
