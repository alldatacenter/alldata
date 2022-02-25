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
package org.apache.ambari.server.utils;

import static org.apache.ambari.server.utils.VersionUtils.DEV_VERSION;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import junit.framework.Assert;

public class TestVersionUtils {

  private static final String MODULE_ERR_MESSAGE = "Module version can't be empty or null";
  private static final String STACK_ERR_MESSAGE = "Stack version can't be empty or null";
  private static final String MPACK_ERR_MESSAGE = "Mpack version can't be empty or null";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testStackVersions() {
    Assert.assertTrue(VersionUtils.areVersionsEqual("2.2.0.0", "2.2.0.0", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual("2.2.0.0-111", "2.2.0.0-999", false));

    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0", "2.2.0.1"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0", "2.2.0.10"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.2.0.20", "2.2.2.145"));

    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.0.1", "2.2.0.0"));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.0.10", "2.2.0.0"));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.2.2.145", "2.2.2.20"));

    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0-200", "2.2.0.1-100"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.0.0-101", "2.2.0.10-20"));
    Assert.assertEquals(-1, VersionUtils.compareVersions("2.2.2.0.20-996", "2.2.2.145-846"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2", "2.2.VER"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2.VAR", "2.2.VER"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.2.3", "2.2.3.VER1.V"));

    Assert.assertEquals(0, VersionUtils.compareVersions("2.2.0.1-200", "2.2.0.1-100"));
    Assert.assertEquals(1, VersionUtils.compareVersionsWithBuild("2.2.0.1-200", "2.2.0.1-100", 4));
  }

  @Test
  public void testVersionCompareSuccess() {
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3", "1.2.3", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3", "1.2.3", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual("", "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(null, null, true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, "1.2.3", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, null, true));

    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1", "1.2.3", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3", "1.2.3", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", "1.2.3", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", null, true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "1.2.3", true));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4"));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4", "1.2.3"));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3"));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4", "1.2.3", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3", 3));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3.9", "1.2.4.6", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.4.6", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2", "1.2.4.6", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4.8", "1.2.3.6.7", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3.4", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3.6.7", "1.2.3.4", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7", "1.2.3.4", 4));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3", "1.2.3.0", 4));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3", "1.2.3.1", 4));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7\n", "1.2.3.4\n", 4)); //test version trimming

    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.1", "1.2.3", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.1.3", "1.2.3", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions("", "1.2.3", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("", null, true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "1.2.3", true));
  }

  @Test
  public void testVersionCompareSuccessCustomVersion() {
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual("1.2.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual("", "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(null, null, true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, "1.2.3_MYAMBARI_000000", false));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, "", true));
    Assert.assertTrue(VersionUtils.areVersionsEqual(DEV_VERSION, null, true));

    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", false));
    Assert.assertFalse(VersionUtils.areVersionsEqual("1.2.3.1_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("2.1.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", "1.2.3_MYAMBARI_000000", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual("", null, true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "", true));
    Assert.assertFalse(VersionUtils.areVersionsEqual(null, "1.2.3_MYAMBARI_000000", true));

    //Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.4_MYAMBARI_000000"));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4_MYAMBARI_000000", "1.2.3_MYAMBARI_000000"));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000"));
    Assert.assertEquals(0, VersionUtils.compareVersions("2.99.99.0", "2.99.99"));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.4_MYAMBARI_000000", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", 3));

    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3.9_MYAMBARI_000000", "1.2.4.6_MYAMBARI_000000", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.4.6_MYAMBARI_000000", 3));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2_MYAMBARI_000000", "1.2.4.6_MYAMBARI_000000", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.4.8_MYAMBARI_000000", "1.2.3.6.7_MYAMBARI_000000", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.3.4_MYAMBARI_000000", 3));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3.6.7_MYAMBARI_000000", "1.2.3.4_MYAMBARI_000000", 3));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7_MYAMBARI_000000", "1.2.3.4_MYAMBARI_000000", 4));
    Assert.assertEquals(0, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.3.0_MYAMBARI_000000", 4));
    Assert.assertEquals(-1, VersionUtils.compareVersions("1.2.3_MYAMBARI_000000", "1.2.3.1_MYAMBARI_000000", 4));
    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.6.7_MYAMBARI_000000\n", "1.2.3.4_MYAMBARI_000000\n", 4)); //test version trimming

    Assert.assertEquals(1, VersionUtils.compareVersions("1.2.3.1_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("2.1.3_MYAMBARI_000000", "1.2.3_MYAMBARI_000000", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions("", "1.2.3_MYAMBARI_000000", true));
    Assert.assertEquals(1, VersionUtils.compareVersions("", null, true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "", true));
    Assert.assertEquals(-1, VersionUtils.compareVersions(null, "1.2.3_MYAMBARI_000000", true));
  }

  @Test
  public void testVersionCompareError() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version2 cannot be empty");
    VersionUtils.areVersionsEqual("1.2.3", "", false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version1 cannot be null");
    VersionUtils.areVersionsEqual(null, "", false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version2 cannot be null");
    VersionUtils.areVersionsEqual("", null, false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version1 cannot be empty");
    VersionUtils.compareVersions("", "1", 2);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("maxLengthToCompare cannot be less than 0");
    VersionUtils.compareVersions("2", "1", -1);
  }

  @Test
  public void testMpackVersionWithNotValidVersions() {
    String errMessage = null;
    try {
      MpackVersion.parse(null);
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(MPACK_ERR_MESSAGE, errMessage);


    try {
      errMessage = null;
      MpackVersion.parse("");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(MPACK_ERR_MESSAGE, errMessage);

    try {
      errMessage = null;
      MpackVersion.parse("1.2.3.4-b10");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals("Wrong format for mpack version, should be N.N.N-bN or N.N.N-hN-bN", errMessage);
  }


  @Test
  public void testStackVersionWithNotValidVersions() {
    String errMessage = null;
    try {
      errMessage = null;
      MpackVersion.parseStackVersion(null);
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(STACK_ERR_MESSAGE, errMessage);


    try {
      errMessage = null;
      MpackVersion.parseStackVersion("");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(STACK_ERR_MESSAGE, errMessage);

    try {
      errMessage = null;
      MpackVersion.parseStackVersion("1.2.3-10");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals("Wrong format for stack version, should be N.N.N.N-N or N.N.N-hN-bN", errMessage);
  }


  @Test
  public void testModuleVersionWithNotValidVersions() {
    String errMessage = null;
    try {
      errMessage = null;
      ModuleVersion.parse(null);
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(MODULE_ERR_MESSAGE, errMessage);


    try {
      errMessage = null;
      ModuleVersion.parse("");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals(MODULE_ERR_MESSAGE, errMessage);


    try {
      errMessage = null;
      ModuleVersion.parse("1.2.3-10");
    } catch (IllegalArgumentException e) {
      errMessage = e.getMessage();
    }
    Assert.assertEquals("Wrong format for module version, should be N.N.N.N-bN or N.N.N-hN-bN", errMessage);
  }

  @Test
  public void testMpackVersionWithValidVersions() {
    Assert.assertEquals(1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parse("1.2.3-b888")));
    Assert.assertEquals(1, MpackVersion.parse("2.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-b888")));
    Assert.assertEquals(1, MpackVersion.parse("1.3.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-b888")));
    Assert.assertEquals(1, MpackVersion.parse("1.2.4-h0-b10").compareTo(MpackVersion.parse("1.2.3-b888")));
    Assert.assertEquals(1, MpackVersion.parse("1.2.3-h0-b1000").compareTo(MpackVersion.parse("1.2.3-b888")));
    Assert.assertEquals(0, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-b10")));
    Assert.assertEquals(0, MpackVersion.parse("1.2.3-b10").compareTo(MpackVersion.parse("1.2.3-b10")));
    Assert.assertEquals(0, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-h0-b10")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.4-b10")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.3.3-b10")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("2.2.3-b10")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-h1-b10")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h0-b10").compareTo(MpackVersion.parse("1.2.3-h0-b11")));

    Assert.assertEquals(0, MpackVersion.parse("1", false).compareTo(MpackVersion.parse("1.0.0-h0-b0")));
    Assert.assertEquals(0, MpackVersion.parse("1.0", false).compareTo(MpackVersion.parse("1.0.0-h0-b0")));
    Assert.assertEquals(0, MpackVersion.parse("1.0.0", false).compareTo(MpackVersion.parse("1.0.0-h0-b0")));
    Assert.assertEquals(0, MpackVersion.parse("1.0.0-b0", false).compareTo(MpackVersion.parse("1.0.0-h0-b0")));
    Assert.assertEquals(0, MpackVersion.parse("1.0.0-h0-b0", false).compareTo(MpackVersion.parse("1.0.0-h0-b0")));

    Assert.assertEquals(-1, MpackVersion.parse("1", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(-1, MpackVersion.parse("1.0", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(-1, MpackVersion.parse("1.0.0", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(-1, MpackVersion.parse("1.0.0-b0", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(-1, MpackVersion.parse("1.0.0-h0-b0", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));

    Assert.assertEquals(1, MpackVersion.parse("2", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(1, MpackVersion.parse("1.1", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
    Assert.assertEquals(1, MpackVersion.parse("1.0.1", false).compareTo(MpackVersion.parse("1.0.0-h0-b111")));
  }

  @Test
  public void testStackVersionWithValidVersions() {
    Assert.assertEquals(1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));
    Assert.assertEquals(1, MpackVersion.parse("1.2.4-h1-b1").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));
    Assert.assertEquals(1, MpackVersion.parse("1.3.3-h1-b1").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));
    Assert.assertEquals(1, MpackVersion.parse("2.2.3-h1-b1").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));

    Assert.assertEquals(0, MpackVersion.parse("1.2.3-h4-b888").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));

    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parseStackVersion("1.2.3.11-888")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parseStackVersion("1.2.4.1-1")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parseStackVersion("1.3.3.1-1")));
    Assert.assertEquals(-1, MpackVersion.parse("1.2.3-h10-b10").compareTo(MpackVersion.parseStackVersion("2.2.3.1-1")));

    Assert.assertEquals(1, MpackVersion.parseStackVersion("1.2.3.4-999").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));
    Assert.assertEquals(0, MpackVersion.parseStackVersion("1.2.3.4-999").compareTo(MpackVersion.parseStackVersion("1.2.3.4-999")));
    Assert.assertEquals(-1, MpackVersion.parseStackVersion("1.2.3.1-999").compareTo(MpackVersion.parseStackVersion("1.2.3.4-888")));
  }

  @Test
  public void testModuleVersionWithValidVersions() {
    Assert.assertEquals(1, ModuleVersion.parse("1.2.3.4-h10-b888").compareTo(ModuleVersion.parse("1.2.3.4-b888")));
    Assert.assertEquals(1, ModuleVersion.parse("1.2.3.5-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(1, ModuleVersion.parse("1.2.4.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(1, ModuleVersion.parse("1.3.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(1, ModuleVersion.parse("2.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(1, ModuleVersion.parse("1.2.3.4-h0-b11").compareTo(ModuleVersion.parse("1.2.3.4-b10")));

    Assert.assertEquals(0, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(0, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-h0-b10")));
    Assert.assertEquals(0, ModuleVersion.parse("1.2.3.4-b10").compareTo(ModuleVersion.parse("1.2.3.4-b10")));
    Assert.assertEquals(0, ModuleVersion.parse("1.2.3.4-b10").compareTo(ModuleVersion.parse("1.2.3.4-h0-b10")));

    Assert.assertEquals(-1, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.4-b888")));
    Assert.assertEquals(-1, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.3.5-b10")));
    Assert.assertEquals(-1, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.2.4.4-b10")));
    Assert.assertEquals(-1, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("1.3.3.4-b10")));
    Assert.assertEquals(-1, ModuleVersion.parse("1.2.3.4-h0-b10").compareTo(ModuleVersion.parse("2.2.3.4-b10")));
  }
}
