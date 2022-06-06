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

import sys
import version_builder

def main(args):
  vb = version_builder.VersionBuilder("version_242-12345.xml")

  vb.set_release(type='STANDARD', stack="HDP-2.4", version="2.4.2.0", build="2468",
    notes="http://example.com", display="HDP-2.4.2.0-2468", compatible="2.4.[0-9]+")
  vb.set_os("redhat6", package_version="2_4_2_0_12345")

  vb.add_manifest("HDFS-271", "HDFS", "2.7.1.2.4.0")
  vb.add_manifest("YARN-271", "HDFS", "2.7.1.2.4.0", version_id = "1", release_version = "2.4.1.0")

  vb.add_repo("redhat6", "HDP-2.4", "HDP", "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.4.2.0", "true")
  vb.add_repo("redhat6", "HDP-UTILS-1.1.0.20", "HDP-UTILS", "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6", "false")

  vb.persist()
  vb.finalize("../../ambari-server/src/main/resources/version_definition.xsd")

if __name__ == "__main__":
  main(sys.argv)
