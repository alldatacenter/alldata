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

package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.TestModules;
import org.apache.atlas.repository.RepositoryException;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class BulkLoadingTest {

    @Inject
    AtlasGraphProvider graphProvider;

    @Test(expectedExceptions = RuntimeException.class)
    public void instantiateBulkLoadingForBerkeleyDB() throws RepositoryException {
        assertNotNull(graphProvider.get());
        assertNotNull(graphProvider.getBulkLoading());
    }
}
