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
package org.apache.drill.exec.udfs;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestCryptoFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testMD5() throws Exception {
    final String query = "select md5('testing') as md5Hash from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("md5Hash").baselineValues("ae2b1fca515949e5d54fb22b8ed95575").go();
  }

  @Test
  public void testMD2() throws Exception {
    final String query = "select md2('testing') as md2Hash from (values(1))";
    testBuilder().sqlQuery(query).ordered().baselineColumns("md2Hash").baselineValues("fc134df10d6ecafceb5c75861d01b41f").go();
  }


  @Test
  public void testSHA1() throws Exception {
    final String query = "select sha('testing') as shaHash from (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("shaHash")
      .baselineValues("dc724af18fbdd4e59189f5fe768a5f8311527050")
      .go();
  }

  @Test
  public void testSHA384() throws Exception {
    final String query = "select sha384('testing') as shaHash from (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("shaHash")
      .baselineValues("cf4811d74fd40504674fc3273f824fa42f755b9660a2e902b57f1df74873db1a91a037bcee65f1a88ecd1ef57ff254c9")
      .go();
  }

  @Test
  public void testSHA512() throws Exception {
    final String query = "select sha512('testing') as shaHash from (values(1))";
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("shaHash")
      .baselineValues("521b9ccefbcd14d179e7a1bb877752870a6d620938b28a66a107eac6e6805b9d0989f45b5730508041aa5e710847d439ea74cd312c9355f1f2dae08d40e41d50")
      .go();
  }

  @Test
  public void testAESEncrypt() throws Exception {
    testBuilder()
      .sqlQuery("select aes_encrypt('testing', 'secret_key') as encrypted from (values(1))")
      .ordered()
      .baselineColumns("encrypted")
      .baselineValues("ICf+zdOrLitogB8HUDru0w==")
      .go();

    testBuilder()
        .sqlQuery("select aes_encrypt(cast(null as varchar), 'secret_key') as encrypted from (values(1))")
        .ordered()
        .baselineColumns("encrypted")
        .baselineValues((String) null)
        .go();
    testBuilder()
        .sqlQuery("select aes_encrypt('testing', cast (null as varchar)) as encrypted from (values(1))")
        .ordered()
        .baselineColumns("encrypted")
        .baselineValues((String) null)
        .go();
  }

  @Test
  public void testAESDecrypt() throws Exception {
    testBuilder()
      .sqlQuery("select aes_decrypt('ICf+zdOrLitogB8HUDru0w==', 'secret_key') as decrypt from (values(1))")
      .ordered()
      .baselineColumns("decrypt")
      .baselineValues("testing")
      .go();

    testBuilder()
        .sqlQuery("select aes_decrypt(cast(null as varchar), 'secret_key') as decrypt from (values(1))")
        .ordered()
        .baselineColumns("decrypt")
        .baselineValues((String) null)
        .go();

    testBuilder()
        .sqlQuery("select aes_decrypt('ICf+zdOrLitogB8HUDru0w==', cast(null as varchar)) as decrypt from (values(1))")
        .ordered()
        .baselineColumns("decrypt")
        .baselineValues((String) null)
        .go();
  }
}