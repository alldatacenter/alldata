#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys

import atlas_config as mc
import atlas_client_cmdline as cmdline

def main():

    conf_dir = cmdline.setup_conf_dir()
    jvm_opts_list = cmdline.setup_jvm_opts_list(conf_dir, 'quick_start_v1.log')
    atlas_classpath = cmdline.get_atlas_classpath(conf_dir)

    process = mc.java("org.apache.atlas.examples.QuickStart", sys.argv[1:], atlas_classpath, jvm_opts_list)
    return process.wait()

if __name__ == '__main__':
    try:
        returncode = main()
        if returncode == 0:
            print("Example data added to Apache Atlas Server!!!\n")
        else:
            print("No data was added to the Apache Atlas Server.\n")
    except Exception as e:
        print("Exception: %s " % str(e))
        returncode = -1

    sys.exit(returncode)
