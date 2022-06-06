"""
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
"""

# configure the VM to run Ambari-server and set Weave network
# argv 1: the name of cluster

from config import Config
from cluster import Cluster
import sys


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print "configure the VM to run Ambari-server and set Weave network"
        print "Arg: <the name of cluster>"
        exit(1)

    Config.load()

    cluster_name = sys.argv[1]
    cluster = Cluster.load_from_json(cluster_name)

    vm = cluster.get_ambari_server_vm()
    vm.run_ambari_server()
