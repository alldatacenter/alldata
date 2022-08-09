"""
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
"""

import os
from unittest import TestCase
from mock.mock import patch
from resource_management.libraries.functions.mpack_version import MpackVersion
from resource_management.libraries.functions.module_version import ModuleVersion


class TestVersion(TestCase):
  """
  Class that tests the method of the version.py file used to format and compare version numbers
  of both Ambari (which use 3 digits separated by dots) and stacks (which use 4 digits separated by dots).
  """
  def setUp(self):
    import imp

    self.test_directory = os.path.dirname(os.path.abspath(__file__))
    test_file_path = os.path.join(self.test_directory, '../../../../ambari-common/src/main/python/resource_management/libraries/functions/version.py')
    with open(test_file_path, 'rb') as fp:
        self.version_module = imp.load_module('version', fp, test_file_path, ('.py', 'rb', imp.PY_SOURCE))

  def test_format(self):
    l = [("2.2",   "2.2.0.0"),
         ("2.2.1", "2.2.1.0"),
         ("2.2.1.3", "2.2.1.3")]
    
    for input, expected in l:
      actual = self.version_module.format_stack_version(input)
      self.assertEqual(expected, actual)

    gluster_fs_actual = self.version_module.format_stack_version("GlusterFS")
    self.assertEqual("", gluster_fs_actual)


  def test_format_with_hyphens(self):
    actual = self.version_module.format_stack_version("FOO-1.0")
    self.assertEqual("1.0.0.0", actual)

    actual = self.version_module.format_stack_version("1.0.0-1234")
    self.assertEqual("1.0.0.0", actual)

    actual = self.version_module.format_stack_version("FOO-1.0-9999")
    self.assertEqual("1.0.0.0", actual)


  def test_comparison(self):
    # All versions to compare, from 1.0.0.0 to 3.0.0.0, and only include elements that are a multiple of 7.
    versions = range(1000, 3000, 7)
    versions = [".".join(list(str(elem))) for elem in versions]

    for idx, x in enumerate(versions):
      for idy, y in enumerate(versions):
        # Expected value will either be -1, 0, 1, and it relies on the fact
        # that an increasing index implies a greater version number.
        expected_value = cmp(idx, idy)
        actual_value = self.version_module.compare_versions(x, y)
        self.assertEqual(expected_value, actual_value)

    # Try something fancier
    self.assertEqual(0, self.version_module.compare_versions("2.10", "2.10.0"))
    self.assertEqual(0, self.version_module.compare_versions("2.10", "2.10.0.0"))
    self.assertEqual(0, self.version_module.compare_versions("2.10.0", "2.10.0.0"))

    try:
      self.version_module.compare_versions("", "GlusterFS")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")


  def test_mpack_version(self):
    try:
      MpackVersion.parse("")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      MpackVersion.parse(None)
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      MpackVersion.parse_stack_version("")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      MpackVersion.parse_stack_version(None)
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      ModuleVersion.parse("")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      ModuleVersion.parse(None)
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")


    try:
      MpackVersion.parse("1.2.3.4-h1-b1")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    try:
      MpackVersion.parse_stack_version("1.1.1.1.1-1")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")


    try:
      ModuleVersion.parse("1.1.1.1-h1")
    except ValueError:
      pass
    else:
      self.fail("Did not raise exception")

    m1 = MpackVersion.parse("1.2.3-h1-b2")
    m2 = MpackVersion.parse("1.2.3-b2")
    self.assertTrue(m1 > m2)

    m1 = MpackVersion.parse("1.2.3-h1-b2")
    m2 = MpackVersion.parse_stack_version("1.2.3.4-33")
    self.assertTrue(m1 < m2)

    m1 = MpackVersion.parse("1.2.3-h0-b10")
    m2 = MpackVersion.parse("1.2.3-b10")
    self.assertTrue(m1 == m2)

    m1 = ModuleVersion.parse("1.2.3.4-h10-b10")
    m2 = ModuleVersion.parse("1.2.3.4-b888")
    self.assertTrue(m1 > m2)

    m1 = ModuleVersion.parse("1.2.3.5-h10-b10")
    m2 = ModuleVersion.parse("1.2.3.4-b888")
    self.assertTrue(m1 > m2)

    m1 = ModuleVersion.parse("1.2.3.4-h0-b10")
    m2 = ModuleVersion.parse("1.2.3.4-b10")
    self.assertTrue(m1 == m2)

  @patch("resource_management.libraries.functions.stack_select.get_role_component_current_stack_version")
  @patch("resource_management.libraries.script.Script.get_config")
  def test_get_current_component_version(self, get_config_mock, get_role_component_current_stack_version_mock):
    ver1 = "1.0.0.0"
    ver2 = "2.0.0.0"

    get_config_mock.return_value = {
      "commandParams": {
        "version": ver1
      },
      "repositoryFile": {
        "resolved": True,
        "repoVersion": ver2,
        "repositories": [],
        "feature": {}
      }
    }

    # case 1. version come with command params
    version = self.version_module.get_current_component_version()

    self.assertFalse(get_role_component_current_stack_version_mock.called)
    self.assertEquals(ver1, version)

    # case 2. version not come with commands params but repository is resolved
    get_role_component_current_stack_version_mock.reset_mock()
    del get_config_mock.return_value["commandParams"]["version"]
    version = self.version_module.get_current_component_version()

    self.assertFalse(get_role_component_current_stack_version_mock.called)
    self.assertEquals(ver2, version)

    # case 3. same as case 2 but repository is not resolved
    get_role_component_current_stack_version_mock.reset_mock()
    get_config_mock.return_value["repositoryFile"]["resolved"] = False
    self.version_module.get_current_component_version()

    self.assertTrue(get_role_component_current_stack_version_mock.called)

