# !/usr/bin/env python

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


from resource_management.core.logger import Logger
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.script import Script
from unittest import TestCase

Logger.initialize_logger()

class TestUpgradeSummary(TestCase):

  def test_get_stack_feature_version_missing_params(self):
    """
    Tests that simple upgrade information can be extracted from JSON
    :return:
    """
    command_json = TestUpgradeSummary._get_cluster_simple_upgrade_json()
    Script.config = command_json

    summary = upgrade_summary.get_upgrade_summary()
    self.assertEqual(False, summary.is_revert)
    self.assertEqual("UPGRADE", summary.direction)
    self.assertEqual("STANDARD", summary.orchestration)
    self.assertEqual("rolling_upgrade", summary.type)

    services = summary.services
    self.assertEqual("2.4.0.0-1234", services["HDFS"].source_version)
    self.assertEqual("2.5.9.9-9999", services["HDFS"].target_version)

    self.assertEqual("2.4.0.0-1234", upgrade_summary.get_source_version("HDFS"))
    self.assertEqual("2.5.9.9-9999", upgrade_summary.get_target_version("HDFS"))

    self.assertTrue(upgrade_summary.get_downgrade_from_version("HDFS") is None)


  def test_get_downgrade_from_version(self):
    """
    Tests that simple downgrade returns the correct version
    :return:
    """
    command_json = TestUpgradeSummary._get_cluster_simple_downgrade_json()
    Script.config = command_json

    self.assertTrue(upgrade_summary.get_downgrade_from_version("FOO") is None)
    self.assertEqual("2.5.9.9-9999", upgrade_summary.get_downgrade_from_version("HDFS"))


  @staticmethod
  def _get_cluster_simple_upgrade_json():
    """
    A restart command during an upgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "hostLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "commandParams": {
        "source_stack": "2.4",
        "target_stack": "2.5",
        "upgrade_direction": "upgrade",
        "version": "2.5.9.9-9999"
      },
      "upgradeSummary": {
        "services":{
          "HDFS":{
            "sourceRepositoryId":1,
            "sourceStackId":"HDP-2.4",
            "sourceVersion":"2.4.0.0-1234",
            "targetRepositoryId":2,
            "targetStackId":"HDP-2.5",
            "targetVersion":"2.5.9.9-9999"
          }
        },
        "direction":"UPGRADE",
        "type":"rolling_upgrade",
        "isRevert":False,
        "orchestration":"STANDARD",
        "associatedStackId":"HDP-2.5",
        "associatedVersion":"2.5.9.9-9999",
        "isDowngradeAllowed": True,
        "isSwitchBits": False
      }
    }

  @staticmethod
  def _get_cluster_simple_downgrade_json():
    """
    A restart command during a downgrade.
    :return:
    """
    return {
      "roleCommand":"ACTIONEXECUTE",
      "hostLevelParams": {
        "stack_name": "HDP",
        "stack_version": "2.4",
      },
      "commandParams": {
        "source_stack": "2.5",
        "target_stack": "2.4",
        "upgrade_direction": "downgrade",
        "version": "2.4.0.0-1234"
      },
      "upgradeSummary": {
        "services":{
          "HDFS":{
            "sourceRepositoryId":2,
            "sourceStackId":"HDP-2.5",
            "sourceVersion":"2.5.9.9-9999",
            "targetRepositoryId":1,
            "targetStackId":"HDP-2.4",
            "targetVersion":"2.4.0.0-1234"
          }
        },
        "direction":"DOWNGRADE",
        "type":"rolling_upgrade",
        "isRevert":False,
        "orchestration":"STANDARD",
        "associatedStackId":"HDP-2.5",
        "associatedVersion":"2.5.9.9-9999",
        "isDowngradeAllowed": True,
        "isSwitchBits": False
      }
    }
