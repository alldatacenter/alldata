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
package org.apache.drill.store.kudu;

import org.apache.drill.PlanTestBase;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.KuduStorageTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Ignore("requires a remote kudu server to run.")
@Category(KuduStorageTest.class)
public class TestKuduPlugin extends BaseTestQuery {

  @Test
  public void testBasicQuery() throws Exception {
    test("select * from kudu.demo;");
  }

  @Test
  public void testDescribe() throws Exception {
    test("use kudu;");
    test("show tables;");
    test("describe demo");
  }

  @Test
  public void testCreate() throws Exception {
    test("create table kudu.regions as select 1, * from sys.options limit 1");
    test("select * from kudu.regions");
    test("drop table kudu.regions");
  }

  @Test
  public void testPhysicalPlanSubmission() throws Exception {
    PlanTestBase.testPhysicalPlanExecutionBasedOnQuery("select * from kudu.demo");
  }

}
