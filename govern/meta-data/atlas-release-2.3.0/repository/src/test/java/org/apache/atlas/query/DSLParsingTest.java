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
package org.apache.atlas.query;

import org.apache.atlas.exception.AtlasBaseException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

@Test
public class DSLParsingTest {
    // Add all invalid constructs here to make sure that the
    // DSL parsing behavior doesn't change
    @DataProvider(name = "badDSLProvider")
    public Object[][] getBadDSLQueries() {
        return new Object[][] {
                {"db orderby(name) orderby(owner)"},
                {"db orderby(name) select name, owner orderby(owner)"},
                {"db groupby(name) orderby(owner) select name, owner orderby(owner)"},
                {"db groupby(name) select name, owner orderby(owner) orderby(owner)"},
                {"db select name, owner orderby(owner) orderby(owner)"},
                {"db select name, owner orderby(owner) groupby(owner)"},
                {"db select name, owner groupby(owner) orderby(owner)"},
                {"db groupby(name) groupby(name) select name, owner orderby(owner) orderby(owner)"},
        };
    }

    @Test(dataProvider = "badDSLProvider", expectedExceptions = AtlasBaseException.class)
    public void testInvalidDSL(String query) throws AtlasBaseException {
        AtlasDSL.Parser.parse(query);
        fail("The invalid query parsing should've failed");
    }
}
