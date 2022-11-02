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

import sys
import logging
import threading
import socket
from ambari_commons import subprocess32

logger = logging.getLogger(__name__)
FUSER_CMD = "timeout 10 fuser {0}/tcp 2>/dev/null | awk '{1}'"
PSPF_CMD = "ps -fp {0}"
PORT_IN_USE_MESSAGE = "Could not open port {0} because port already used by another process:\n{1}"

class PingPortListener(threading.Thread):

  def __init__(self, config):
    threading.Thread.__init__(self)
    self.daemon = True
    self.running = True
    self.config = config
    self.host = '0.0.0.0'
    self.port = int(self.config.get('agent','ping_port'))

    logger.debug("Checking Ping port listener port {0}".format(self.port))

    if not self.port == None and not self.port == 0:
      (stdoutdata, stderrdata) = self.run_os_command_in_shell(FUSER_CMD.format(str(self.port), "{print $1}"))
      if stdoutdata.strip() and stdoutdata.strip().isdigit():
        (stdoutdata, stderrdata) = self.run_os_command_in_shell(PSPF_CMD.format(stdoutdata.strip()))
        raise Exception(PORT_IN_USE_MESSAGE.format(str(self.port), stdoutdata))      
    self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.socket.bind((self.host, self.port))
    self.socket.listen(1)
    config.set('agent','current_ping_port',str(self.socket.getsockname()[1]))
    logger.info("Ping port listener started on port: " + str(self.socket.getsockname()[1]))


  def run_os_command_in_shell(self, command):
    process = subprocess32.Popen(command, stdout=subprocess32.PIPE,
              stdin=subprocess32.PIPE,
              stderr=subprocess32.PIPE,
              shell=True)
    return process.communicate()

  def __del__(self):
    logger.info("Ping port listener killed")


  def run(self):
    while  self.running:
      try:
        conn, addr = self.socket.accept()
        conn.send("OK")
        conn.close()
      except Exception as ex:
        logger.error("Failed in Ping port listener because of:" + str(ex));
        sys.exit(1)
  pass
