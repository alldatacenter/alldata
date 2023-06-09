/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.dfs;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.TestTools;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestSchemaNotFoundException extends BaseTestQuery {

    @Test(expected = Exception.class)
    public void testSchemaNotFoundForWrongStoragePlgn() throws Exception {
        final String table = String.format("%s/empty", TestTools.WORKING_PATH.resolve(TestTools.TEST_RESOURCES_REL));
        final String query = String.format("select * from dfs1.`%s`", table);
        try {
            testNoResult(query);
        } catch (Exception ex) {
            final String pattern = String.format("[[dfs1]] is not valid with respect to either root schema or current default schema").toLowerCase();
            final boolean isSchemaNotFound = ex.getMessage().toLowerCase().contains(pattern);
            assertTrue(isSchemaNotFound);
            throw ex;
        }
    }

    @Test(expected = Exception.class)
    public void testSchemaNotFoundForWrongWorkspace() throws Exception {
        final String table = String.format("%s/empty", TestTools.WORKING_PATH.resolve(TestTools.TEST_RESOURCES_REL));
        final String query = String.format("select * from dfs.tmp1.`%s`", table);
        try {
            testNoResult(query);
        } catch (Exception ex) {
            final String pattern = String.format("[[dfs, tmp1]] is not valid with respect to either root schema or current default schema").toLowerCase();
            final boolean isSchemaNotFound = ex.getMessage().toLowerCase().contains(pattern);
            assertTrue(isSchemaNotFound);
            throw ex;
        }
    }

    @Test(expected = Exception.class)
    public void testSchemaNotFoundForWrongWorkspaceUsingDefaultWorkspace() throws Exception {
        final String table = String.format("%s/empty", TestTools.WORKING_PATH.resolve(TestTools.TEST_RESOURCES_REL));
        final String query = String.format("select * from tmp1.`%s`", table);
        try {
            testNoResult("use dfs");
            testNoResult(query);
        } catch (Exception ex) {
            final String pattern = String.format("[[tmp1]] is not valid with respect to either root schema or current default schema").toLowerCase();
            final boolean isSchemaNotFound = ex.getMessage().toLowerCase().contains(pattern);
            assertTrue(isSchemaNotFound);
            throw ex;
        }
    }

    @Test(expected = Exception.class)
    public void testTableNotFoundException() throws Exception {
        final String table = String.format("%s/missing.parquet", TestTools.WORKING_PATH.resolve(TestTools.TEST_RESOURCES_REL));
        final String query = String.format("select * from tmp.`%s`", table);
        try {
            testNoResult("use dfs");
            testNoResult(query);
        } catch (Exception ex) {
            final String pattern = String.format("[[dfs, tmp1]] is not valid with respect to either root schema or current default schema").toLowerCase();
            final boolean isSchemaNotFound = ex.getMessage().toLowerCase().contains(pattern);
            final boolean isTableNotFound = ex.getMessage().toLowerCase().contains(String.format("%s' not found", table).toLowerCase());
            assertTrue(!isSchemaNotFound && isTableNotFound);
            throw ex;
        }
    }
}
