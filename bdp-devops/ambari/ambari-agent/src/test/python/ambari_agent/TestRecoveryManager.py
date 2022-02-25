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
import copy
import tempfile
from ambari_agent.RecoveryManager import RecoveryManager
from mock.mock import patch, MagicMock, call


class _TestRecoveryManager(TestCase):
  command = {
    "commandType": "STATUS_COMMAND",
    "payloadLevel": "EXECUTION_COMMAND",
    "componentName": "NODEMANAGER",
    "desiredState": "STARTED",
    "hasStaleConfigs": False,
    "executionCommandDetails": {
      "commandType": "EXECUTION_COMMAND",
      "roleCommand": "INSTALL",
      "role": "NODEMANAGER",
      "hostLevelParams": {
        "custom_command":""},
      "configurations": {
        "capacity-scheduler": {
          "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
        "capacity-calculator": {
          "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
        "commandParams": {
          "service_package_folder": "common-services/YARN/2.1.0.2.0/package"
        }
      }
    }
  }

  exec_command1 = {
    "commandType": "EXECUTION_COMMAND",
    "roleCommand": "INSTALL",
    "role": "NODEMANAGER",
    "configurations": {
      "capacity-scheduler": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "capacity-calculator": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "commandParams": {
        "service_package_folder": "common-services/YARN/2.1.0.2.0/package"
      }
    },
    "hostLevelParams": {}
  }

  exec_command2 = {
    "commandType": "EXECUTION_COMMAND",
    "roleCommand": "START",
    "role": "NODEMANAGER",
    "configurations": {
      "capacity-scheduler": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "capacity-calculator": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "commandParams": {
        "service_package_folder": "common-services/YARN/2.1.0.2.0/package"
      }
    },
    "hostLevelParams": {}
  }

  exec_command3 = {
    "commandType": "EXECUTION_COMMAND",
    "roleCommand": "SERVICE_CHECK",
    "role": "NODEMANAGER",
    "configurations": {
      "capacity-scheduler": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "capacity-calculator": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "commandParams": {
        "service_package_folder": "common-services/YARN/2.1.0.2.0/package"
      }
    },
    "hostLevelParams": {}
  }

  exec_command4 = {
    "commandType": "EXECUTION_COMMAND",
    "roleCommand": "CUSTOM_COMMAND",
    "role": "NODEMANAGER",
    "configurations": {
      "capacity-scheduler": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "capacity-calculator": {
        "yarn.scheduler.capacity.default.minimum-user-limit-percent": "100"},
      "commandParams": {
        "service_package_folder": "common-services/YARN/2.1.0.2.0/package"
      }
    },
    "hostLevelParams": {
      "custom_command": "RESTART"
    }
  }

  def test_defaults(self):
    rm = RecoveryManager(MagicMock())
    self.assertFalse(rm.enabled())
    self.assertEqual(None, rm.get_install_command("NODEMANAGER"))
    self.assertEqual(None, rm.get_start_command("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

  @patch.object(RecoveryManager, "_now_")
  def test_sliding_window(self, time_mock):
    time_mock.side_effect = \
      [1000, 1001, 1002, 1003, 1004, 1071, 1150, 1151, 1152, 1153, 1400, 1401,
       1500, 1571, 1572, 1653, 1900, 1971, 2300, 2301]

    rm = RecoveryManager(MagicMock(), True, False)
    self.assertTrue(rm.enabled())

    config = rm.update_config(0, 60, 5, 12, True, False, False)
    self.assertFalse(rm.enabled())

    rm.update_config(6, 60, 5, 12, True, False, False)
    self.assertTrue(rm.enabled())

    rm.update_config(6, 0, 5, 12, True, False, False)
    self.assertFalse(rm.enabled())

    rm.update_config(6, 60, 0, 12, True, False, False)
    self.assertFalse(rm.enabled())

    rm.update_config(6, 60, 1, 12, True, False, False)
    self.assertTrue(rm.enabled())

    rm.update_config(6, 60, 61, 12, True, False, False)
    self.assertFalse(rm.enabled())

    rm.update_config(6, 60, 5, 4, True, False, False)
    self.assertFalse(rm.enabled())

    # maximum 2 in 2 minutes and at least 1 minute wait
    rm.update_config(2, 5, 1, 4, True, False, False)
    self.assertTrue(rm.enabled())

    # T = 1000-2
    self.assertTrue(rm.may_execute("NODEMANAGER"))
    self.assertTrue(rm.may_execute("NODEMANAGER"))
    self.assertTrue(rm.may_execute("NODEMANAGER"))

    # T = 1003-4
    self.assertTrue(rm.execute("NODEMANAGER"))
    self.assertFalse(rm.execute("NODEMANAGER"))  # too soon

    # T = 1071
    self.assertTrue(rm.execute("NODEMANAGER"))  # 60+ seconds passed

    # T = 1150-3
    self.assertFalse(rm.execute("NODEMANAGER"))  # limit 2 exceeded
    self.assertFalse(rm.may_execute("NODEMANAGER"))
    self.assertTrue(rm.execute("DATANODE"))
    self.assertTrue(rm.may_execute("NAMENODE"))

    # T = 1400-1
    self.assertTrue(rm.execute("NODEMANAGER"))  # windows reset
    self.assertFalse(rm.may_execute("NODEMANAGER"))  # too soon

    # maximum 2 in 2 minutes and no min wait
    rm.update_config(2, 5, 1, 5, True, True, False)

    # T = 1500-3
    self.assertTrue(rm.execute("NODEMANAGER2"))
    self.assertTrue(rm.may_execute("NODEMANAGER2"))
    self.assertTrue(rm.execute("NODEMANAGER2"))
    self.assertFalse(rm.execute("NODEMANAGER2"))  # max limit

    # T = 1900-2
    self.assertTrue(rm.execute("NODEMANAGER2"))
    self.assertTrue(rm.execute("NODEMANAGER2"))

    # T = 2300-2
    # lifetime max reached
    self.assertTrue(rm.execute("NODEMANAGER2"))
    self.assertFalse(rm.execute("NODEMANAGER2"))

  def test_recovery_required(self):
    rm = RecoveryManager(MagicMock(), False)
    rm.update_config(12, 5, 1, 15, True, False, False, )
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'NODEMANAGER', 'service_name': 'YARN', 'desired_state': 'INSTALLED'}
    ]}})
    
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "INSTALLED")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "STARTED")
    rm.update_desired_status("NODEMANAGER", "INSTALLED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "XYS")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_desired_status("NODEMANAGER", "")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "INIT")
    rm.update_desired_status("NODEMANAGER", "INSTALLED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm = RecoveryManager(MagicMock(), True)

    rm.update_current_status("NODEMANAGER", "INIT")
    rm.update_desired_status("NODEMANAGER", "INSTALLED")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "INIT")
    rm.update_desired_status("NODEMANAGER", "START")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "START")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

  def test_recovery_required2(self):

    rm = RecoveryManager(MagicMock(), True)
    rm.update_config(15, 5, 1, 16, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'NODEMANAGER', 'service_name': 'YARN', 'desired_state': 'INSTALLED'}
    ]}})
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm = RecoveryManager( MagicMock(), True)
    rm.update_config(15, 5, 1, 16, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'NODEMANAGER', 'service_name': 'YARN', 'desired_state': 'INSTALLED'}
    ]}})
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("DATANODE", "INSTALLED")
    rm.update_desired_status("DATANODE", "STARTED")
    self.assertFalse(rm.requires_recovery("DATANODE"))

    rm = RecoveryManager(MagicMock(), True)
    rm.update_config(15, 5, 1, 16, True, False, False)
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertFalse(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("DATANODE", "INSTALLED")
    rm.update_desired_status("DATANODE", "STARTED")
    self.assertFalse(rm.requires_recovery("DATANODE"))

    rm.update_config(15, 5, 1, 16, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'NODEMANAGER', 'service_name': 'YARN', 'desired_state': 'INSTALLED'}
    ]}})
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")
    self.assertTrue(rm.requires_recovery("NODEMANAGER"))

    rm.update_current_status("DATANODE", "INSTALLED")
    rm.update_desired_status("DATANODE", "STARTED")
    self.assertFalse(rm.requires_recovery("DATANODE"))

  @patch.object(RecoveryManager, "_now_")
  def test_recovery_report(self, time_mock):
    time_mock.side_effect = \
      [1000, 1071, 1072, 1470, 1471, 1472, 1543, 1644, 1815]

    rm = RecoveryManager(MagicMock())
    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "DISABLED"})

    rm.update_config(2, 5, 1, 4, True, True, False)
    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "RECOVERABLE", "componentReports": []})

    rm.execute("PUMA")
    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "RECOVERABLE",
                               "componentReports": [{"name": "PUMA", "numAttempts": 1, "limitReached": False}]})
    rm.execute("PUMA")
    rm.execute("LION")

    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "RECOVERABLE",
                               "componentReports": [
                                 {"name": "LION", "numAttempts": 1, "limitReached": False},
                                 {"name": "PUMA", "numAttempts": 2, "limitReached": False}
                               ]})
    rm.execute("PUMA")
    rm.execute("LION")
    rm.execute("PUMA")
    rm.execute("PUMA")
    rm.execute("LION")
    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "PARTIALLY_RECOVERABLE",
                               "componentReports": [
                                 {"name": "LION", "numAttempts": 3, "limitReached": False},
                                 {"name": "PUMA", "numAttempts": 4, "limitReached": True}
                               ]})

    rm.execute("LION")
    rec_st = rm.get_recovery_status()
    self.assertEquals(rec_st, {"summary": "UNRECOVERABLE",
                               "componentReports": [
                                 {"name": "LION", "numAttempts": 4, "limitReached": True},
                                 {"name": "PUMA", "numAttempts": 4, "limitReached": True}
                               ]})

  @patch.object(RecoveryManager, "_now_")
  def test_command_expiry(self, time_mock):
    time_mock.side_effect = \
      [1000, 1001, 1104, 1105, 1106, 1807, 1808, 1809, 1810, 1811, 1812]

    rm = RecoveryManager(MagicMock())
    rm.update_config(5, 5, 0, 11, True, False, False)

    command1 = copy.deepcopy(self.command)

    #rm.store_or_update_command(command1)
    rm.update_config(12, 5, 1, 15, True, False, False)
    
    rm.update_recovery_config({'recoveryConfig':{'components':[{'component_name': 'NODEMANAGER', 'service_name': 'YARN', 'desired_state': 'INSTALLED'}]}})
    
    rm.update_current_status("NODEMANAGER", "INSTALLED")
    rm.update_desired_status("NODEMANAGER", "STARTED")

    commands = rm.get_recovery_commands()
    self.assertEqual(1, len(commands))
    self.assertEqual("START", commands[0]["roleCommand"])

    commands = rm.get_recovery_commands()
    self.assertEqual(1, len(commands))
    self.assertEqual("START", commands[0]["roleCommand"])
    
    rm.retry_gap_in_sec = 60

    #1807 command is stale
    commands = rm.get_recovery_commands()
    self.assertEqual(0, len(commands))

    commands = rm.get_recovery_commands()
    self.assertEqual(1, len(commands))
    self.assertEqual("START", commands[0]["roleCommand"])

  def test_configured_for_recovery(self):
    rm = RecoveryManager(MagicMock(), True)
    rm.update_config(12, 5, 1, 15, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'A', 'service_name': 'A', 'desired_state': 'INSTALLED'},
      {'component_name': 'B', 'service_name': 'B', 'desired_state': 'INSTALLED'},
    ]}})
    
    self.assertTrue(rm.configured_for_recovery("A"))
    self.assertTrue(rm.configured_for_recovery("B"))

    rm.update_config(5, 5, 1, 11, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[]}})
    
    self.assertFalse(rm.configured_for_recovery("A"))
    self.assertFalse(rm.configured_for_recovery("B"))

    rm.update_config(5, 5, 1, 11, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'A', 'service_name': 'A', 'desired_state': 'INSTALLED'}
    ]}})
    
    self.assertTrue(rm.configured_for_recovery("A"))
    self.assertFalse(rm.configured_for_recovery("B"))

    rm.update_config(5, 5, 1, 11, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components': [
      {'component_name': 'A', 'service_name': 'A', 'desired_state': 'INSTALLED'}
    ]}})
    
    self.assertTrue(rm.configured_for_recovery("A"))
    self.assertFalse(rm.configured_for_recovery("B"))
    self.assertFalse(rm.configured_for_recovery("C"))

    rm.update_config(5, 5, 1, 11, True, False, False)
    rm.update_recovery_config({'recoveryConfig':{'components':[
      {'component_name': 'A', 'service_name': 'A', 'desired_state': 'INSTALLED'},
      {'component_name': 'D', 'service_name': 'D', 'desired_state': 'INSTALLED'},
      {'component_name': 'F', 'service_name': 'F', 'desired_state': 'INSTALLED'}
    ]}})
    self.assertTrue(rm.configured_for_recovery("A"))
    self.assertFalse(rm.configured_for_recovery("B"))
    self.assertFalse(rm.configured_for_recovery("C"))
    self.assertTrue(rm.configured_for_recovery("D"))
    self.assertFalse(rm.configured_for_recovery("E"))
    self.assertTrue(rm.configured_for_recovery("F"))

  @patch.object(RecoveryManager, "_now_")
  def test_reset_if_window_passed_since_last_attempt(self, time_mock):
    time_mock.side_effect = \
      [1000, 1071, 1372]
    rm = RecoveryManager(MagicMock())

    rm.update_config(2, 5, 1, 4, True, True, False)

    rm.execute("COMPONENT")
    actions = rm.get_actions_copy()["COMPONENT"]
    self.assertEquals(actions['lastReset'], 1000)
    rm.execute("COMPONENT")
    actions = rm.get_actions_copy()["COMPONENT"]
    self.assertEquals(actions['lastReset'], 1000)
    #reset if window_in_sec seconds passed since last attempt
    rm.execute("COMPONENT")
    actions = rm.get_actions_copy()["COMPONENT"]
    self.assertEquals(actions['lastReset'], 1372)


  @patch.object(RecoveryManager, "_now_")
  def test_is_action_info_stale(self, time_mock):

    rm = RecoveryManager(MagicMock())
    rm.update_config(5, 60, 5, 16, True, False, False)

    time_mock.return_value = 0
    self.assertFalse(rm.is_action_info_stale("COMPONENT_NAME"))

    rm.actions["COMPONENT_NAME"] = {
      "lastAttempt": 0,
      "count": 0,
      "lastReset": 0,
      "lifetimeCount": 0,
      "warnedLastAttempt": False,
      "warnedLastReset": False,
      "warnedThresholdReached": False
    }
    time_mock.return_value = 3600
    self.assertFalse(rm.is_action_info_stale("COMPONENT_NAME"))

    rm.actions["COMPONENT_NAME"] = {
      "lastAttempt": 1,
      "count": 1,
      "lastReset": 0,
      "lifetimeCount": 1,
      "warnedLastAttempt": False,
      "warnedLastReset": False,
      "warnedThresholdReached": False
    }
    time_mock.return_value = 3601
    self.assertFalse(rm.is_action_info_stale("COMPONENT_NAME"))

    time_mock.return_value = 3602
    self.assertTrue(rm.is_action_info_stale("COMPONENT_NAME"))
