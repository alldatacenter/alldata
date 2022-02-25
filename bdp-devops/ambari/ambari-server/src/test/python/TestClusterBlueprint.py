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
from mock.mock import patch, call
import json
cluster_blueprint = __import__('cluster_blueprint')
from cluster_blueprint import AmbariBlueprint
import logging
import pprint

class TestClusterBlueprint(TestCase):

  TEST_BLUEPRINT = '../resources/TestAmbaryServer.samples/multinode-default.json'
  BLUEPRINT_HOSTS = '../resources/TestAmbaryServer.samples/blueprint_hosts.json'

  @patch.object(AmbariBlueprint, "performPostOperation")
  @patch.object(cluster_blueprint, "get_server_info")
  def test_importBlueprint(self, get_server_info_mock, performPostOperationMock):

    performPostOperationMock.side_effect = ['201', '202']

    BLUEPRINT_POST_JSON = open(self.TEST_BLUEPRINT).read()
    BLUEPRINT_HOST_JSON = open(self.BLUEPRINT_HOSTS).read()

    blueprintUrl = 'http://localhost:8080/api/v1/blueprints/blueprint-multinode-default'
    hostCreateUrl = 'http://localhost:8080/api/v1/clusters/c1'

    AmbariBlueprint.SILENT = True

    ambariBlueprint = AmbariBlueprint()
    ambariBlueprint.importBlueprint(self.TEST_BLUEPRINT, self.BLUEPRINT_HOSTS, "c1")

    get_server_info_mock.assertCalled()
    performPostOperationMock.assert_has_calls([
      call(blueprintUrl, BLUEPRINT_POST_JSON),
      call(hostCreateUrl, BLUEPRINT_HOST_JSON)
    ])

    pass


  @patch("__builtin__.open")
  @patch.object(AmbariBlueprint, "performGetOperation")
  @patch.object(cluster_blueprint, "get_server_info")
  def test_exportBlueprint(self, get_server_info_mock,
                           performGetOperationMock, openMock):
    performGetOperationMock.return_value = '200'

    blueprintUrl = 'http://localhost:8080/api/v1/clusters/blueprint' +\
                   '-multinode-default?format=blueprint'

    AmbariBlueprint.SILENT = True

    ambariBlueprint = AmbariBlueprint()
    ambariBlueprint.exportBlueprint('blueprint-multinode-default', '/tmp/test')

    openMock.assertCalled()
    get_server_info_mock.assertCalled()
    performGetOperationMock.assert_called_with(blueprintUrl)

    pass