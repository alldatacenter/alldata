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

import os
import sys

from ambari_agent.ClusterConfigurationCache import ClusterConfigurationCache

from mock.mock import MagicMock, patch, mock_open, ANY
from unittest import TestCase

class TestClusterConfigurationCache:#(TestCase):

  o_flags = os.O_WRONLY | os.O_CREAT
  perms = 0o600

  def setUp(self):
    # save original open() method for later use
    self.original_open = open

  def tearDown(self):
    sys.stdout == sys.__stdout__


  @patch("json.load")
  @patch("os.path.exists", new = MagicMock(return_value=True))
  @patch("os.path.isfile", new = MagicMock(return_value=True))
  def test_cluster_configuration_cache_initialization(self, json_load_mock):
    configuration_json = { "0" : { "foo-site" : { "foo" : "bar", "foobar" : "baz" } } }
    
    json_load_mock.return_value = configuration_json
    cluster_configuration = ClusterConfigurationCache(os.path.join(os.sep, "tmp", "bar", "baz"))
    cluster_configuration.rewrite_cache(configuration_json, 'abc')

    self.assertEqual('bar', cluster_configuration['0']['foo-site']['foo'] )
    self.assertEqual('baz', cluster_configuration['0']['foo-site']['foobar'] )


  @patch("ambari_simplejson.dump")
  def test_cluster_configuration_update(self, json_dump_mock):
    cluster_configuration = self.__get_cluster_configuration()

    configuration = {'foo-site' :
      { 'bar': 'rendered-bar', 'baz' : 'rendered-baz' }
    }

    osopen_mock, osfdopen_mock = self.__update_cluster_configuration(cluster_configuration, configuration)

    json_dump_mock.assert_called_with({'0': {'foo-site': {'baz': 'rendered-baz', 'bar': 'rendered-bar'}}}, ANY, indent=2)
    pass

  def __get_cluster_configuration(self):
    """
    Gets an instance of the cluster cache where the file read and write
    operations have been mocked out
    :return:
    """
    with patch("__builtin__.open") as open_mock:
      open_mock.side_effect = self.open_side_effect
      cluster_configuration = ClusterConfigurationCache(os.path.join(os.sep, "tmp", "bar", "baz"))
      return cluster_configuration


  @patch("os.open")
  @patch("os.fdopen")
  def __update_cluster_configuration(self, cluster_configuration, configuration, osfdopen_mock, osopen_mock):
    """
    Updates the configuration cache, using as mock file as the disk based
    cache so that a file is not created during tests
    :return:
    """
    osopen_mock.return_value = 11
    cluster_configuration.rewrite_cache({"0":configuration},'test-hash')

    return osopen_mock, osfdopen_mock

  def open_side_effect(self, file, mode):
    if mode == 'w':
      file_mock = MagicMock()
      return file_mock
    else:
      return self.original_open(file, mode)
