#!/usr/bin/env python

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

Ambari Agent

"""
import os
import time
from unittest import TestCase
from mock.mock import patch, MagicMock, ANY

from only_for_platform import get_platform, not_for_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from resource_management.core import Environment
from resource_management.core.system import System
from resource_management.libraries import XmlConfig



@patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
class TestXmlConfigResource(TestCase):
  """
  XmlConfig="resource_management.libraries.providers.xml_config.XmlConfigProvider",
  Testing XmlConfig(XmlConfigProvider) with different 'resource configurations'
  """

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  @patch.object(time, "asctime")
  def test_action_create_empty_xml_config(self,
                                          time_asctime_mock,
                                          os_path_isdir_mock,
                                          os_path_exists_mock,
                                          create_file_mock,
                                          ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={},
                configuration_attributes={}
                )

    create_file_mock.assert_called_with('/dir/conf/file.xml', u'  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">\n    \n  </configuration>',
        encoding='UTF-8', on_file_created=ANY)


  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  @patch.object(time, "asctime")
  def test_action_create_simple_xml_config(self,
                                           time_asctime_mock,
                                           os_path_isdir_mock,
                                           os_path_exists_mock,
                                           create_file_mock,
                                           ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={"Some conf":"Some value"}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={'property1': 'value1'},
                configuration_attributes={'attr': {'property1': 'attr_value'}}
                )

    create_file_mock.assert_called_with('/dir/conf/file.xml', u'  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">\n    \n    <property>\n      <name>property1</name>\n      <value>value1</value>\n      <attr>attr_value</attr>\n    </property>\n    \n  </configuration>',
        encoding='UTF-8', on_file_created=ANY)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  @patch.object(time, "asctime")
  def test_action_create_simple_xml_config_with_inclusion(self,
                                           time_asctime_mock,
                                           os_path_isdir_mock,
                                           os_path_exists_mock,
                                           create_file_mock,
                                           ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={"Some conf":"Some value"}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={'property1': 'value1'},
                configuration_attributes={'attr': {'property1': 'attr_value'}},
                xml_include_file="/dif/conf/include_file.xml"
                )

    create_file_mock.assert_called_with('/dir/conf/file.xml', u'  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">\n    \n    <property>\n      <name>property1</name>\n      <value>value1</value>\n      <attr>attr_value</attr>\n    </property>\n    \n    <xi:include href="/dif/conf/include_file.xml"/>\n    \n  </configuration>',
        encoding='UTF-8', on_file_created=ANY)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  @patch.object(time, "asctime")
  def test_action_create_xml_config_with_metacharacters(self,
                                                        time_asctime_mock,
                                                        os_path_isdir_mock,
                                                        os_path_exists_mock,
                                                        create_file_mock,
                                                        ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and write proper data
    where configurations={"Some conf":"Some metacharacters"}
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={"": "",
                                "prop.1": "'.'yyyy-MM-dd-HH",
                                "prop.3": "%d{ISO8601} %5p %c{1}:%L - %m%n",
                                "prop.2": "INFO, openjpa",
                                "prop.4": "${oozie.log.dir}/oozie.log",
                                "prop.empty": "",
                                },
                configuration_attributes={
                    "": {
                        "prop.1": "should_not_be_printed",
                        "prop.2": "should_not_be_printed",
                    },
                    "attr1": {
                        "prop.1": "x",
                        "prop.8": "not_existed",
                    },
                    "attr2": {
                        "prop.4": "value4",
                        "prop.3": "value3"
                    },
                    "attr_empty": {
                    },
                    "attr_value_empty": {
                        "prop.4": "",
                        "prop.empty": ""
                    }
                })

    create_file_mock.assert_called_with('/dir/conf/file.xml', u'  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">\n    \n    <property>\n      <name></name>\n      <value></value>\n    </property>\n    \n    <property>\n      <name>prop.1</name>\n      <value>&#39;.&#39;yyyy-MM-dd-HH</value>\n      <attr1>x</attr1>\n    </property>\n    \n    <property>\n      <name>prop.2</name>\n      <value>INFO, openjpa</value>\n    </property>\n    \n    <property>\n      <name>prop.3</name>\n      <value>%d{ISO8601} %5p %c{1}:%L - %m%n</value>\n      <attr2>value3</attr2>\n    </property>\n    \n    <property>\n      <name>prop.4</name>\n      <value>${oozie.log.dir}/oozie.log</value>\n      <attr_value_empty></attr_value_empty>\n      <attr2>value4</attr2>\n    </property>\n    \n    <property>\n      <name>prop.empty</name>\n      <value></value>\n      <attr_value_empty></attr_value_empty>\n    </property>\n    \n  </configuration>',
        encoding='UTF-8', on_file_created=ANY)

  @patch("resource_management.core.providers.system._ensure_metadata")
  @patch("resource_management.core.sudo.create_file")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  @patch.object(time, "asctime")
  def test_action_create_xml_config_sorted_by_key(self,
                                                  time_asctime_mock,
                                                  os_path_isdir_mock,
                                                  os_path_exists_mock,
                                                  create_file_mock,
                                                  ensure_mock):
    """
    Tests if 'create' action - creates new non existent xml file and writes proper data
    where configurations={"Key":"Value"} are stored in sorted by key order
    """
    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False
    time_asctime_mock.return_value = 'Wed 2014-02'

    with Environment('/') as env:
      XmlConfig('file.xml',
                conf_dir='/dir/conf',
                configurations={"": "",
                                "third": "should be third",
                                "first": "should be first",
                                "z_last": "should be last",
                                "second": "should be second",
                                },
                configuration_attributes={}
                )

    create_file_mock.assert_called_with('/dir/conf/file.xml', u'  <configuration  xmlns:xi="http://www.w3.org/2001/XInclude">\n    \n    <property>\n      <name></name>\n      <value></value>\n    </property>\n    \n    <property>\n      <name>first</name>\n      <value>should be first</value>\n    </property>\n    \n    <property>\n      <name>second</name>\n      <value>should be second</value>\n    </property>\n    \n    <property>\n      <name>third</name>\n      <value>should be third</value>\n    </property>\n    \n    <property>\n      <name>z_last</name>\n      <value>should be last</value>\n    </property>\n    \n  </configuration>',
        encoding='UTF-8', on_file_created=ANY)

  @patch("resource_management.libraries.providers.xml_config.File")
  @patch("resource_management.core.sudo.path_exists")
  @patch("resource_management.core.sudo.path_isdir")
  def test_action_create_arguments(self, os_path_isdir_mock ,os_path_exists_mock, file_mock):

    os_path_isdir_mock.side_effect = [False, True]
    os_path_exists_mock.return_value = False

    with Environment() as env:
      XmlConfig('xmlFile.xml',
                conf_dir='/dir/conf',
                configurations={'property1': 'value1'},
                configuration_attributes={'attr': {'property1': 'attr_value'}},
                mode = 0755,
                owner = "hdfs",
                group = "hadoop",
                encoding = "Code"
      )

    self.assertEqual(file_mock.call_args[0][0],'/dir/conf/xmlFile.xml')
    call_args = file_mock.call_args[1].copy()
    del call_args['content']
    self.assertEqual(call_args,{'owner': 'hdfs', 'group': 'hadoop', 'mode': 0755, 'encoding' : 'Code'})
