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
from ambari_commons import subprocess32
import os
import sys
import AmbariConfig
from mock.mock import MagicMock, patch, ANY
with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent import AmbariAgent


class TestAmbariAgent(unittest.TestCase):

  @patch.object(subprocess32, "Popen")
  @patch("os.path.isfile")
  @patch("os.remove")
  def test_main(self, os_remove_mock,
                os_path_isfile_mock, subprocess32_popen_mock):
    facter1 = MagicMock()
    facter2 = MagicMock()
    subprocess32_popen_mock.side_effect = [facter1, facter2]
    facter1.returncode = 77
    facter2.returncode = 55
    os_path_isfile_mock.return_value = True
    if not (os.environ.has_key("PYTHON")):
      os.environ['PYTHON'] = "test/python/path"
    sys.argv[0] = "test data"
    AmbariAgent.main()

    self.assertTrue(subprocess32_popen_mock.called)
    self.assertTrue(subprocess32_popen_mock.call_count == 2)
    self.assertTrue(facter1.communicate.called)
    self.assertTrue(facter2.communicate.called)
    self.assertTrue(os_path_isfile_mock.called)
    self.assertTrue(os_path_isfile_mock.call_count == 2)
    self.assertTrue(os_remove_mock.called)

  #
  # Test AmbariConfig.getLogFile() for ambari-agent
  #
  def test_logfile_location(self):
    #
    # Test without $AMBARI_AGENT_LOG_DIR
    #
    log_folder = '/var/log/ambari-agent'
    log_file = 'ambari-agent.log'
    with patch.dict('os.environ', {}):
      self.assertEqual(os.path.join(log_folder, log_file), AmbariConfig.AmbariConfig.getLogFile())

    #
    # Test with $AMBARI_AGENT_LOG_DIR
    #
    log_folder = '/myloglocation/log'
    log_file = 'ambari-agent.log'
    with patch.dict('os.environ', {'AMBARI_AGENT_LOG_DIR': log_folder}):
      self.assertEqual(os.path.join(log_folder, log_file), AmbariConfig.AmbariConfig.getLogFile())
    pass

  #
  # Test AmbariConfig.getOutFile() for ambari-agent
  #
  def test_outfile_location(self):
    #
    # Test without $AMBARI_AGENT_OUT_DIR
    #
    out_folder = '/var/log/ambari-agent'
    out_file = 'ambari-agent.out'
    with patch.dict('os.environ', {}):
      self.assertEqual(os.path.join(out_folder, out_file), AmbariConfig.AmbariConfig.getOutFile())

    #
    # Test with $AMBARI_AGENT_OUT_DIR
    #
    out_folder = '/myoutlocation/out'
    out_file = 'ambari-agent.out'
    with patch.dict('os.environ', {'AMBARI_AGENT_LOG_DIR': out_folder}):
      self.assertEqual(os.path.join(out_folder, out_file), AmbariConfig.AmbariConfig.getOutFile())
    pass
