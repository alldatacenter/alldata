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

"""
Run this file to interrupt a running python process and open an interactive shell. 
"""

import os
import signal
from RemoteDebugUtils import NamedPipe
from RemoteDebugUtils import pipename

def debug_process(pid):
  """Interrupt a running process and debug it."""
  os.kill(pid, signal.SIGUSR2)  # Signal process.
  pipe = NamedPipe(pipename(pid), 1)
  try:
    while pipe.is_open():
      txt=raw_input(pipe.get()) + '\n'
      pipe.put(txt)
  except EOFError:
    pass # Exit.
  pipe.close()
    
def main():
  with open("/var/run/ambari-agent/ambari-agent.pid") as f:
    pid_str = f.read().strip()
    pid = int(pid_str)
    
  debug_process(pid)
  
if __name__=='__main__':
  main()
        