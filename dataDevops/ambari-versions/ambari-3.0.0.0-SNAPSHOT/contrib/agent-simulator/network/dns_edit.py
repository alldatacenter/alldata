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

# This script will modify the /etc/resolv.conf
# A new DNS is added as the first entry of "nameserver"
# the domain is set to be Weave local domain, which is "weave.local"
# set the Weave local domain as the first entry of "search"

import sys

local_Weave_DNS_IP = sys.argv[1]
nameserver_file_name = "/etc/resolv.conf"

lines = []
with open(nameserver_file_name) as f_read:
    lines = f_read.readlines()

add_nameserver = False
with open(nameserver_file_name, "w") as f:
    for line in lines:
        if "search" in line:
            tokens = line.split()
            f.write("search weave.local")
            for token in tokens[1:]:
                f.write(" ")
                f.write(token)
            f.write("\n")
        elif "nameserver" in line:
            if add_nameserver == False:
                f.write("nameserver ")
                f.write(local_Weave_DNS_IP)
                f.write("\n")
                add_nameserver = True
            f.write(line)
        elif "domain" in line:
            f.write("domain weave.local\n")
        else:
            f.write(line)
