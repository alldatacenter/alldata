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

from .generic_parser import GenericParser


class AptParser(GenericParser):
  @staticmethod
  def config_reader(stream):
    """
    apt-config dump command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :rtype collections.Iterable
    :return tuple(key, value)

    Usage:
      for key, value in __config_reader(text_stream):
        ...

    Parsing subject:

       PROPERTY "";
       PROPERTY::ITEM1:: "value";
       .....

    """
    for line in stream:
      key, value = line.strip().split(" ", 1)
      key = key.strip("::")
      value = value.strip(";").strip("\"").strip()
      if not value:
        continue

      yield key, value

  @staticmethod
  def packages_reader(stream):
    """
    apt-cache dump command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :rtype collections.Iterable
    :return tuple(package name, version, parsed repo list file)

    Usage:
      for package, version, repo_file_path in __packages_reader(text_stream):
        ...

    Parsing subject:

        Package: test_package
     Version: 0.1.1-0
         File: /var/lib/apt/lists/some_site_dists_apt_main_binary-amd64_Packages.gz
     Description Language:
                     File: /var/lib/apt/lists/some_site_dists_apt_main_binary-amd64_Packages.gz
                      MD5: 000000000000000000000000000
    """
    fields = {"Package": 0, "Version": 1, "File": 2}
    field_names = fields.keys()
    field_count = len(field_names)
    item_set = [None] * field_count

    for line in stream:
      line = line.strip()

      if not line:
        continue

      values = line.split(":", 1)
      if len(values) != 2:
        continue

      field, value = values
      value = value[1:]

      if field in field_names:
        if field == "File":
          value = value.rpartition("/")[2]
        elif field == "Package":
          item_set = [None] * field_count  # reset fields which were parsed before new block
        item_set[fields[field]] = value
      else:
        continue

      if None not in item_set:
        yield item_set
        item_set = [None] * field_count

  @staticmethod
  def packages_installed_reader(stream):
    """
    dpkg -l command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :rtype collections.Iterable
    :return tuple(package name, version)

    Usage:
      for package, version in __packages_installed_reader(text_stream):
        ...

    Parsing subject:

      ||/ Name                              Version               Architecture          Description
      +++-=================================-=====================-=====================-======================
      ii  package1                           version1                all                   description1
      ii  package2                           version2                all                   description2
    """
    for line in stream:
      line = line.lstrip()

      if line[:2] != "ii":
        continue

      line = line[2:].lstrip()
      data = line.partition(" ")
      pkg_name = data[0].partition(":")[0]  # for system packages in format "libuuid1:amd64"
      version = data[2].strip().partition(" ")[0]

      if pkg_name and version:
        yield pkg_name, version
