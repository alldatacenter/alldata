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
import os
from ambari_server.serverConfiguration import get_ambari_properties, get_resources_location
from resource_management.core import sudo

def main():
  properties = get_ambari_properties()
  if properties == -1:
    print >> sys.stderr, "Error getting ambari properties"
    return -1

  resources_location = get_resources_location(properties)
  views_dir = os.path.join(resources_location, "views")

  for file in os.listdir(views_dir):
    path = os.path.join(views_dir, file)
    if os.path.isfile(path):
      if "ambari-admin" in path or "storm-view" in path:
        print "Keeping views jar : " + path
      else:
        print "Deleting views jar : " + path
        sudo.unlink(path)
    else:
      print "Deleting views directory : " + path
      sudo.rmtree(path)
  return 0

if __name__ == "__main__":
    exit (main())
