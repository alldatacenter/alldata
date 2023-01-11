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

from setuptools import setup, find_packages

# External dependencies
requirements = ['requests>=2.24']

long_description = ''
with open("README.md", "r") as fh:
    long_description = fh.read()

setup(
    name="apache-ranger",
    version="0.0.5",
    author="Apache Ranger",
    author_email="dev@ranger.apache.org",
    description="Apache Ranger Python client",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/apache/ranger/tree/master/intg/src/main/python",
    license='Apache LICENSE 2.0',
    classifiers=[
        "Programming Language :: Python :: 2.7",
        "License :: OSI Approved :: Apache Software License",
        "Operating System :: OS Independent",
    ],
    packages=find_packages(),
    install_requires=requirements,
    include_package_data=True,
    zip_safe=False,
    keywords='ranger client, apache ranger',
    python_requires='>=2.7',
)
