# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from setuptools import setup

setup(
    name = "ambari-agent",
    version = "1.0.3-SNAPSHOT",
    packages = ['ambari_agent'],
    # metadata for upload to PyPI
    author = "Apache Software Foundation",
    author_email = "ambari-dev@incubator.apache.org",
    description = "Ambari agent",
    license = "Apache License v2.0",
    keywords = "hadoop, ambari",
    url = "http://incubator.apache.org/ambari",
    long_description = "This package implements the Ambari agent for installing Hadoop on large clusters.",
    platforms=["any"],
    entry_points = {
        "console_scripts": [
            "ambari-agent = ambari_agent.main:main",
        ],
    }
)
