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
import os
import sys

import atlas_config as mc

DEFAULT_JVM_OPTS="-Xmx1024m"

def main():

    atlas_home = mc.atlasDir()
    confdir = mc.dirMustExist(mc.confDir(atlas_home))
    logdir = mc.dirMustExist(mc.logDir(atlas_home))
    mc.executeEnvSh(confdir)

    jvm_opts_list = []

    default_jvm_opts = DEFAULT_JVM_OPTS
    atlas_jvm_opts = os.environ.get(mc.ATLAS_OPTS, default_jvm_opts)
    jvm_opts_list.extend(atlas_jvm_opts.split())

    #expand web app dir
    web_app_dir = mc.webAppDir(atlas_home)
    mc.expandWebApp(atlas_home)

    p = os.pathsep
    atlas_classpath = confdir + p \
                       + os.path.join(web_app_dir, "atlas", "WEB-INF", "classes" ) + p \
                       + os.path.join(web_app_dir, "atlas", "WEB-INF", "lib", "*" )  + p \
                       + os.path.join(atlas_home, "libext", "*")

    process = mc.java("org.apache.atlas.util.CredentialProviderUtility", sys.argv[1:], atlas_classpath, jvm_opts_list)
    process.wait()

if __name__ == '__main__':
    try:
        returncode = main()
    except Exception as e:
        print("Exception: %s " % str(e))
        returncode = -1

    sys.exit(returncode)
