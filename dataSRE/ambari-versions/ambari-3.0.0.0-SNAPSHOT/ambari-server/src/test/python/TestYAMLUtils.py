#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

from unittest import TestCase

from ambari_commons import yaml_utils

class TestYAMLUtils(TestCase):
  def setUp(self):
    pass

  def test_convert_yaml_array(self):
    expected_values = []

    expected_values.append("c6401.ambari.apache.org")
    values = yaml_utils.get_values_from_yaml_array("['c6401.ambari.apache.org']")
    self.assertEquals(expected_values, values)

    expected_values.append("c6402.ambari.apache.org")
    values = yaml_utils.get_values_from_yaml_array("['c6401.ambari.apache.org', 'c6402.ambari.apache.org']")
    self.assertEquals(expected_values, values)

    values = yaml_utils.get_values_from_yaml_array('["c6401.ambari.apache.org", "c6402.ambari.apache.org"]')
    self.assertEquals(expected_values, values)

    values = yaml_utils.get_values_from_yaml_array('[\'c6401.ambari.apache.org\', "c6402.ambari.apache.org"]')
    self.assertEquals(expected_values, values)


  def test_yaml_property_escaping(self):
    """
    Tests that YAML values are escaped with quotes properly when needed
    """
    self.assertEquals("True", yaml_utils.escape_yaml_property("True"))
    self.assertEquals("FALSE", yaml_utils.escape_yaml_property("FALSE"))
    self.assertEquals("yes", yaml_utils.escape_yaml_property("yes"))
    self.assertEquals("NO", yaml_utils.escape_yaml_property("NO"))
    self.assertEquals("28", yaml_utils.escape_yaml_property("28"))
    self.assertEquals("28.0", yaml_utils.escape_yaml_property("28.0"))
    self.assertEquals("[a,b,c]", yaml_utils.escape_yaml_property("[a,b,c]"))
    self.assertEquals("{ foo : bar }", yaml_utils.escape_yaml_property("{ foo : bar }"))

    # some strings which should be escaped
    self.assertEquals("'5f'", yaml_utils.escape_yaml_property("5f"))
    self.assertEquals("'28.O'", yaml_utils.escape_yaml_property("28.O"))
    self.assertEquals("'This is a test of a string'", yaml_utils.escape_yaml_property("This is a test of a string"))

    # test maps
    map = """
      storm-cluster:
        hosts:
          [c6401.ambari.apache.org, c6402.ambari.apache.org, c6403-master.ambari.apache.org]
        groups:
          [hadoop, hadoop-secure]
    """
    escaped_map = yaml_utils.escape_yaml_property(map)
    self.assertTrue(escaped_map.startswith("\n"))
    self.assertFalse("'" in escaped_map)

    # try some weird but valid formatting
    map = """


      storm-cluster    :
              hosts   :
[c6401.ambari.apache.org, c6402.ambari.apache.org, c6403-master.ambari.apache.org]
  groups   :
          [hadoop!!!, hadoop-secure!!!!-----]
    """
    escaped_map = yaml_utils.escape_yaml_property(map)
    self.assertTrue(escaped_map.startswith("\n"))
    self.assertFalse("'" in escaped_map)

    # try some bad formatting - this is not a map
    map = """ foo : bar :
      [baz]"""
    escaped_map = yaml_utils.escape_yaml_property(map)
    self.assertFalse(escaped_map.startswith("\n"))
    self.assertTrue("'" in escaped_map)
