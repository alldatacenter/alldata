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

from ambari_agent import Controller
import pprint, json, os, time, sys
import tempfile
from urllib2 import Request, urlopen, URLError
from mock.mock import patch, MagicMock, call
from ambari_agent.AmbariConfig  import AmbariConfig
import Queue
import logging
from ambari_agent import PythonExecutor

logger=logging.getLogger()

queue = Queue.Queue()

# Set to True to replace python calls with mockups
disable_python = True

agent_version = "1.3.0"

# Values from the list below are returned in responce to agent requests (one per
# request). When every value has been returned, the last element of list is
# returned on every subsequent request.
responces = [
  """{"responseId":"n",
  "response":"OK"}""",

  """
  {
    "responseId":"n",
    "restartAgent": false,
    "executionCommands":
      [{
        "commandId": "31-1",
        "role" : "DATANODE",
        "taskId" : 2,
        "clusterName" : "clusterName",
        "serviceName" : "HDFS",
        "roleCommand" : "UPGRADE",
        "hostname" : "localhost.localdomain",
        "hostLevelParams": {},
        "clusterHostInfo": "clusterHostInfo",
        "configurations": {},
        "commandType": "EXECUTION_COMMAND",
        "configurations": {"global" : {}},
        "roleParams": {},
        "commandParams" :	{
          "source_stack_version": "{\\"stackName\\":\\"HDP\\",\\"stackVersion\\":\\"1.2.2\\"}",
          "target_stack_version": "{\\"stackName\\":\\"HDP\\",\\"stackVersion\\":\\"1.3.0\\"}"
        },
        "clusterHostInfo": {
          "ambari_db_server_host": [
              "dev.hortonworks.com"
          ],
          "ganglia_server_host": [
              "dev.hortonworks.com"
          ],
          "namenode_host": [
              "dev.hortonworks.com"
          ],
          "slave_hosts": [
              "dev.hortonworks.com"
          ]
        }
      }],
    "statusCommands":[]
  }
  """,

  """
  {
    "responseId":"n",
    "restartAgent": false,
    "executionCommands": [],
    "statusCommands":[]
  }
  """
]

class Int(object):
  def __init__(self, value):
    self.value = value

  def inc(self):
    self.value += 1

  def val(self):
    return self.value

responseId = Int(0)

def main():

  if disable_python:
    with patch.object(PythonExecutor.PythonExecutor, 'run_file') \
                                          as run_file_py_method:
      run_file_py_method.side_effect = \
            lambda command, file, tmpoutfile, tmperrfile: {
        'exitcode' : 0,
        'stdout'   : "Simulated run of py %s" % file,
        'stderr'   : 'None'
      }
      run_simulation()
  else:
    run_simulation()



def run_simulation():
  Controller.logger = MagicMock()
  sendRequest_method = MagicMock()

  tmpfile = tempfile.gettempdir()

  config = AmbariConfig().getConfig()
  config.set('agent', 'prefix', tmpfile)

  ver_file = os.path.join(tmpfile, "version")

  with open(ver_file, "w") as text_file:
      text_file.write(agent_version)

  controller = Controller.Controller(config)
  controller.sendRequest = sendRequest_method
  controller.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC = 0.1
  controller.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC = 0.1
  controller.range = 1

  for responce in responces:
    queue.put(responce)

  def send_stub(url, data):
    logger.info("Controller sends data to %s :" % url)
    logger.info(pprint.pformat(data))
    if not queue.empty():
      responce = queue.get()
    else:
      responce = responces[-1]
      logger.info("There is no predefined responce available, sleeping for 30 sec")
      time.sleep(30)
    responce = json.loads(responce)
    responseId.inc()
    responce["responseId"] = responseId.val()
    responce = json.dumps(responce)
    logger.info("Returning data to Controller:" + responce)
    return responce

  sendRequest_method.side_effect = send_stub

  logger.setLevel(logging.DEBUG)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - \
        %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)
  logger.info("Starting")

  controller.start()
  controller.actionQueue.IDLE_SLEEP_TIME = 0.1
  controller.run()


if __name__ == '__main__':
#  s =   """
#  {
#    "responseId":"n",
#    "restartAgent": false,
#    "executionCommands":
#      [{
#        "commandId": "31-1",
#        "role" : "DATANODE",
#        "taskId" : 2,
#        "clusterName" : "clusterName",
#        "serviceName" : "HDFS",
#        "roleCommand" : "UPGRADE",
#        "hostname" : "localhost.localdomain",
#        "hostLevelParams": {},
#        "clusterHostInfo": "clusterHostInfo",
#        "configurations": {},
#        "commandType": "EXECUTION_COMMAND",
#        "configurations": {"global" : {}},
#        "roleParams": {},
#        "commandParams" :	{
#          "commandParams": {"source_stack_version": "{\\"stackName\\":\\"HDP\\",\\"stackVersion\\":\\"1.2.0\\"}", "target_stack_version": "{\\"stackName\\":\\"HDP\\",\\"stackVersion\\":\\"1.2.2\\"}"}
#        },
#        "clusterHostInfo": {
#          "ambari_db_server_host": [
#              "dev.hortonworks.com"
#          ],
#          "ganglia_server_host": [
#              "dev.hortonworks.com"
#          ],
#          "namenode_host": [
#              "dev.hortonworks.com"
#          ],
#          "slave_hosts": [
#              "dev.hortonworks.com"
#          ]
#        }
#      }],
#    "statusCommands":[]
#  }
#  """
#  t = json.loads(s)
#  pprint.pprint(t)

  main()



