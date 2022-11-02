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

import sys, os
try:
  # Python 2
  from string import join
  import ConfigParser
except ImportError:
  # Python 3
  join = ' '.join
  import configparser as ConfigParser

DEFAULT_RACK = "/default-rack"
DATA_FILE_NAME =  os.path.dirname(os.path.abspath(__file__)) + "/topology_mappings.data"
SECTION_NAME = "network_topology"

class TopologyScript():

  def load_rack_map(self):
    try:
      #RACK_MAP contains both host name vs rack and ip vs rack mappings
      mappings = ConfigParser.ConfigParser()
      mappings.read(DATA_FILE_NAME)
      return dict(mappings.items(SECTION_NAME))
    except ConfigParser.NoSectionError:
      return {}

  def get_racks(self, rack_map, args):
    if len(args) == 1:
      return DEFAULT_RACK
    else:
      return join([self.lookup_by_hostname_or_ip(input_argument, rack_map) for input_argument in args[1:]],)

  def lookup_by_hostname_or_ip(self, hostname_or_ip, rack_map):
    #try looking up by hostname
    rack = rack_map.get(hostname_or_ip)
    if rack is not None:
      return rack
    #try looking up by ip
    rack = rack_map.get(self.extract_ip(hostname_or_ip))
    #try by localhost since hadoop could be passing in 127.0.0.1 which might not be mapped
    return rack if rack is not None else rack_map.get("localhost.localdomain", DEFAULT_RACK)

  #strips out port and slashes in case hadoop passes in something like 127.0.0.1/127.0.0.1:50010
  def extract_ip(self, container_string):
    return container_string.split("/")[0].split(":")[0]

  def execute(self, args):
    rack_map = self.load_rack_map()
    rack = self.get_racks(rack_map, args)
    print(rack)

if __name__ == "__main__":
  TopologyScript().execute(sys.argv)
