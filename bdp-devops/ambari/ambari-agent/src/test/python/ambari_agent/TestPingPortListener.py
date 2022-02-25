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

import unittest
from mock.mock import patch, MagicMock, call, Mock
from ambari_agent import PingPortListener
from ambari_commons import subprocess32
import socket
import sys

class TestPingPortListener(unittest.TestCase):

  def setUp(self):
    self.config = MagicMock()
    self.config.get.return_value = 55000
    PingPortListener.logger = MagicMock()

  @patch.object(subprocess32, "Popen")
  @patch("socket.socket")
  def test_init_success(self,socketMock,popen_mock):
    procObj = MagicMock()
    procObj.communicate = MagicMock()
    procObj.communicate.return_value = {"": 0, "log": "log"}
    popen_mock.return_value = procObj
    PingPortListener.logger.reset_mock()
    popen_mock.reset_mock()
    allive_daemon = PingPortListener.PingPortListener(self.config)
    self.assertTrue(popen_mock.called)
    self.assertFalse(PingPortListener.logger.warn.called)
    self.assertTrue(socketMock.call_args_list[0][0][0] == socket.AF_INET)
    self.assertTrue(socketMock.call_args_list[0][0][1] == socket.SOCK_STREAM)
    self.assertTrue(allive_daemon.socket.bind.call_args_list[0][0][0] == ('0.0.0.0',55000))
    self.assertTrue(allive_daemon.socket.listen.call_args_list[0][0][0] == 1)
    self.assertTrue(allive_daemon.config.set.call_args_list[0][0][0] == 'agent')
    self.assertTrue(allive_daemon.config.set.call_args_list[0][0][1] == 'current_ping_port')



  @patch.object(subprocess32, "Popen")
  @patch.object(socket.socket,"bind")
  @patch.object(socket.socket,"listen")
  def test_init_warn(self,socketListenMock,socketBindMock,popen_mock):
    procObj = MagicMock()
    procObj.communicate = MagicMock()
    procObj.communicate.return_value = {"mine.py": 0, "log": "log"}
    popen_mock.return_value = procObj
    PingPortListener.logger.reset_mock()
    try:
      PingPortListener.PingPortListener(self.config)
      self.fail("Should throw exception")
    except Exception as fe:
      # Expected
      self.assertEqual(1, procObj.communicate.call_count)
      pass

if __name__ == "__main__":
  suite = unittest.TestLoader().loadTestsFromTestCase(PingPortListener)
  unittest.TextTestRunner(verbosity=2).run(suite)
