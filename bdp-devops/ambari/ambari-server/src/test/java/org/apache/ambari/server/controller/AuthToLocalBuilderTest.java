/*
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

package org.apache.ambari.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.utils.CollectionPresentationUtils;
import org.junit.Test;

public class AuthToLocalBuilderTest {

  @Test
  public void testRuleGeneration() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }


  @Test
  public void testRuleGeneration_caseInsensitiveSupport() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), true);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*///L\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testRuleGeneration_changeToCaseInsensitiveSupport() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    String existingRules = builder.generate();

    builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), true);
    builder.addRules(existingRules);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*///L\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testRuleGeneration_changeToCaseSensitiveSupport() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), true);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    String existingRules = builder.generate();

    builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules(existingRules);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testRuleGeneration_ExistingRules() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    // previously generated non-host specific rules
    builder.addRule("foobar@EXAMPLE.COM", "hdfs");
    // doesn't exist in latter generation
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    String existingRules = builder.generate();

    builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    // set previously existing rules
    builder.addRules(existingRules);

    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate of existing rule should not result in duplicate rule generation
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // duplicated again in this builder should not result in duplicate rule generation
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testRuleGeneration_ExistingRules_existingMoreSpecificRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    // previously generated non-host specific rules
    builder.addRule("foobar@EXAMPLE.COM", "hdfs");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    String existingRules = builder.generate();
    // prepend host specific rule
    existingRules = "RULE:[2:$1/$2@$0](dn/somehost.com@EXAMPLE.COM)s/.*/hdfs/\n" + existingRules;
    // append default realm rule for additional realm
    existingRules += "\nRULE:[1:$1@$0](.*@OTHER_REALM.COM)s/@.*//";

    builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    // set previously existing rules
    builder.addRules(existingRules);
    // more specific host qualifed rule exists for dn
    // non-host specific rule should still be generated but occur later in generated string
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // duplicate of existing rule
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");


    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[1:$1@$0](.*@OTHER_REALM.COM)s/@.*//\n" +
            "RULE:[2:$1/$2@$0](dn/somehost.com@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testAddNullExistingRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules(null);

    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "DEFAULT",
        builder.generate()
    );
  }

  @Test
  public void testRuleRegexWithDifferentEnding() {
    String rules =
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\\\\\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\ntext\\\\" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\\\\\\" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\\/\\";

    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules(rules);

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "DEFAULT",
        builder.generate());
  }


  @Test
  public void testRuleRegexWithComplexReplacements() {
    String rules =
        "RULE:[1:$1@$0](foobar@\\QEXAMPLE1.COM\\E$)s/.*@\\QEXAMPLE1.COM\\E$/hdfs/\n" +
            "RULE:[1:$1@$0](.*@\\QEXAMPLE1.COM\\E)s/@\\QEXAMPLE1.COM\\E//\n" +
            "RULE:[2:$1@$0](.*@\\QEXAMPLE1.COM\\E)s/@\\QEXAMPLE1.COM\\E//";

    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules(rules);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("ambari-qa-c1@EXAMPLE.COM", "ambari-qa");

    assertEquals(
        "RULE:[1:$1@$0](ambari-qa-c1@EXAMPLE.COM)s/.*/ambari-qa/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[1:$1@$0](.*@\\QEXAMPLE1.COM\\E)s/@\\QEXAMPLE1.COM\\E//\n" +
            "RULE:[1:$1@$0](foobar@\\QEXAMPLE1.COM\\E$)s/.*@\\QEXAMPLE1.COM\\E$/hdfs/\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](.*@\\QEXAMPLE1.COM\\E)s/@\\QEXAMPLE1.COM\\E//\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testRulesWithWhitespace() {
    String rulesWithWhitespace =
        "RULE:   [1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[  1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:   $1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0   ](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0]   (jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)   s/.*/hdfs/\n";

    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules(rulesWithWhitespace);

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "DEFAULT",
        builder.generate());

  }

  @Test
  public void testExistingRuleWithNoRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules("RULE:[1:$1](foobar)s/.*/hdfs/");

    assertEquals(
        "RULE:[1:$1](foobar)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testExistingRuleWithNoRealm2() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules("RULE:[1:$1/$2](foobar/someHost)s/.*/hdfs/");

    assertEquals(
        "RULE:[1:$1/$2](foobar/someHost)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNewRuleWithNoRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("someUser", "hdfs");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNewRuleWithNoRealm2() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("someUser/someHost", "hdfs");
  }

  @Test
  public void testExistingWildcardRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);
    builder.addRules("RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n" +
        "RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](.*@EXAMPLE.COM)s/.*/yarn/\n" +
        "DEFAULT");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");

    // ensure that no default realm rule is generated for .* realm and
    // also that that .* realm rules are ordered last in relation to
    // other rules with the same number of expected principal components
    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](.*@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testClone() throws CloneNotSupportedException {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    AuthToLocalBuilder copy = (AuthToLocalBuilder) builder.clone();
    assertNotSame(builder, copy);
    assertEquals(builder.generate(), copy.generate());

    // Ensure that mutable fields do not change the copy when changed in the original
    builder.addRule("user@EXAMPLE.COM", "hdfs");
    assertTrue(!copy.generate().equals(builder.generate()));
  }

  @Test
  public void testAdditionalRealms() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", "REALM2,REALM3, REALM1  ", false);

    builder.addRules(
        "RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//\n" +
            "DEFAULT");

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    // Depends on hashing, string representation can be different
    List<String> rules = Arrays.asList("RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//",
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//",
        "RULE:[1:$1@$0](.*@REALM2)s/@.*//",
        "RULE:[1:$1@$0](.*@REALM1)s/@.*//",
        "RULE:[1:$1@$0](.*@REALM3)s/@.*//",
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/",
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/",
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/",
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/",
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/",
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/",
        "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/",
        "DEFAULT");
    assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(builder.generate(), rules,
        "\n", 0, 0));
  }

  @Test
  public void testAdditionalRealms_Null() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testAdditionalRealms_Empty() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", "", false);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "DEFAULT",
        builder.generate());
  }

  @Test
  public void testUseCase() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", "FOOBAR.COM,HW.HDP,BAZ.NET", false);

    String existingRules =
        "RULE:[1:$1@$0](.*@BAZ.NET)s/@.*//\n" +
            "RULE:[1:$1@$0](accumulo-c1@EXAMPLE.COM)s/.*/accumulo/\n" +
            "RULE:[1:$1@$0](ambari-qa-c1@EXAMPLE.COM)s/.*/ambari-qa/\n" +
            "RULE:[1:$1@$0](hbase-c1@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[1:$1@$0](hdfs-c1@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](spark-c1@EXAMPLE.COM)s/.*/spark/\n" +
            "RULE:[1:$1@$0](tracer-c1@EXAMPLE.COM)s/.*/accumulo/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "RULE:[1:$1@$0](.*@FOOBAR.COM)s/@.*//\n" +
            "RULE:[1:$1@$0](.*@HW.HDP)s/@.*//\n" +
            "RULE:[2:$1@$0](accumulo@EXAMPLE.COM)s/.*/accumulo/\n" +
            "RULE:[2:$1@$0](amshbase@EXAMPLE.COM)s/.*/ams/\n" +
            "RULE:[2:$1@$0](amszk@EXAMPLE.COM)s/.*/ams/\n" +
            "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](falcon@EXAMPLE.COM)s/.*/falcon/\n" +
            "RULE:[2:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](hive@EXAMPLE.COM)s/.*/hive/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](nm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](oozie@EXAMPLE.COM)s/.*/oozie/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
            "DEFAULT";

    builder.addRules(existingRules);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("yarn/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("kafka/_HOST@EXAMPLE.COM", null);
    builder.addRule("hdfs-c1@EXAMPLE.COM", "hdfs");

    assertEquals(existingRules, builder.generate());
  }

  @Test
  public void testCustomRuleCanBeAddedWithCaseSensitivity() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false)
      .addRule("yarn/_HOST@EXAMPLE.COM", "yarn")
      .addRules(
      "RULE:[1:$1@$0](.*@HDP01.LOCAL)s/.*/ambari-qa//L\n" +
    "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
    "DEFAULT");
    assertEquals(
      "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[1:$1@$0](.*@HDP01.LOCAL)s/.*/ambari-qa//L\n" +
        "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
        "DEFAULT"
      , builder.generate());
  }

  @Test
  public void testCaseSensitivityFlagIsRemovedAfterItWasAddedToAmbariRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), false)
      .addRule("yarn/_HOST@EXAMPLE.COM", "yarn")
      .addRules(
          "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn//L\n" +
          "DEFAULT");
    assertEquals(
      "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
        "DEFAULT"
      , builder.generate());
  }

  @Test
  public void testCaseSensitivityFlagIsAddedAfterItWasFromAmbariRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder("EXAMPLE.COM", Collections.emptyList(), true)
      .addRule("yarn/_HOST@EXAMPLE.COM", "yarn")
      .addRules(
          "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
          "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
          "DEFAULT");
    assertEquals(
      "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*///L\n" +
        "RULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\n" +
        "DEFAULT"
      , builder.generate());
  }
}