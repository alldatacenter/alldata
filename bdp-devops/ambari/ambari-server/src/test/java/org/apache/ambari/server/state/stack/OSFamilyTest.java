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
package org.apache.ambari.server.state.stack;


import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;


@Category({ category.StackUpgradeTest.class})
public class OSFamilyTest {
   OsFamily os_family = null;
   private Injector injector;

  @Before
  public void setup() throws Exception {
    injector  = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    os_family = injector.getInstance(OsFamily.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testOSListing() throws Exception{
   Set<String> actual_oslist =  os_family.os_list();
   Set<String> expected_oslist = new HashSet<>(Arrays.asList(
     "redhat6", "oraclelinux5", "suse11", "fedora6", "opensuse11",
     "centos6", "fedora5", "centos5", "ubuntu12", "redhat5", "sles11",
     "oraclelinux6", "debian12", "sled11", "win2012server6", "win2012serverr26", "win2008serverr26", "win2008server6"
   ));

   Assert.assertNotNull(actual_oslist);
   Assert.assertEquals(expected_oslist, actual_oslist);
  }

  @Test
  public void testParsingOS() throws Exception{
    // test data
    Map<String,String> expected_map = new HashMap<>();
    expected_map.put("distro", "ubuntu");
    expected_map.put("versions", "12");

    String test_value = "ubuntu12";

    // trying to call private method
    Class[] parse_os_args = {
       String.class
    };
    Method parse_os = os_family.getClass().getDeclaredMethod("parse_os", parse_os_args);

    // setting private method to be available
    parse_os.setAccessible(true);
    Object test_map = parse_os.invoke(os_family, test_value);

    // setting them back to private for the instance
    parse_os.setAccessible(false);


    // checking result
    Assert.assertNotNull(test_map);
    Assert.assertEquals(expected_map.getClass().getName(), test_map.getClass().getName());
    Assert.assertEquals(expected_map, test_map);
  }

  @Test
  public void testFindTypes() throws Exception{
    Set<String> expected_set = new HashSet<>(Arrays.asList(
      "ubuntu12",
      "debian12"
    ));

    Set<String> actual_set = os_family.findTypes("ubuntu12");
    Assert.assertNotNull(actual_set);
    Assert.assertEquals(expected_set, actual_set);
  }

  @Test
  public void testFind() throws Exception{
    String expected_result = "ubuntu12";
    String actual_result = os_family.find("debian12");

    Assert.assertNotNull(actual_result);
    Assert.assertEquals(expected_result, actual_result);

    // for windows
    expected_result = "winsrv6";
    actual_result = os_family.find("win2012server6");

    Assert.assertNotNull(actual_result);
    Assert.assertEquals(expected_result, actual_result);
  }
}

