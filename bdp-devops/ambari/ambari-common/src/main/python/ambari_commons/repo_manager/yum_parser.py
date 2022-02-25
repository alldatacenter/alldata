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

from .generic_parser import GenericParser, ANSI_ESCAPE, REMOVE_CHARS


class YumParser(GenericParser):
  @staticmethod
  def config_reader(stream):
    # ToDo: future feature
    raise NotImplementedError()

  @staticmethod
  def packages_installed_reader(stream):
    """
    For yum, the output content for `yum list available|all|installed` is the same

    :param stream collections.Iterable
    """
    return YumParser.packages_reader(stream)

  @staticmethod
  def list_all_select_tool_packages_reader(stream):
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
  def lookup_packages(lines, skip_till):
    """
    Deprecated and not used function, would be removed in later releases.

    :type lines str
    :type skip_till str|None
    """
    packages = []

    lines = [line.strip() for line in lines]
    items = []
    if skip_till:
      skip_index = 3
      for index in range(len(lines)):
        if skip_till in lines[index]:
          skip_index = index + 1
          break
    else:
      skip_index = 0

    for line in lines[skip_index:]:
      items = items + line.strip(' \t\n\r').split()

    items_count = len(items)

    for i in range(0, items_count, 3):

      # check if we reach the end
      if i+3 > items_count:
        break

      if '.' in items[i]:
        items[i] = items[i][:items[i].rindex('.')]
      if items[i + 2].find('@') == 0:
        items[i + 2] = items[i + 2][1:]
      packages.append(items[i:i + 3])

    return packages

  @staticmethod
  def packages_reader(stream):
    """
    :type stream collections.Iterable

    Notice:  yum contains bug, which makes it format output by using default term with 80 on non-pty terminal.
      It makes it to use "Work Wrap" for long lines. To avoid such behaviour, use execution mechanic which allow
      to set proper terminal geometry.


    Sample text:

    Loaded plugins: fastestmirror, ovl
    Loading mirror speeds from cached hostfile
     * base: centos.colocall.net
     * extras: centos.colocall.net
     * updates: centos.colocall.net
    Installed Packages
    MAKEDEV.x86_64                           3.24-6.el6                  @CentOS/6.9
    audit-libs.x86_64                        2.4.5-6.el6                 @CentOS/6.9
    basesystem.noarch                        10.0-4.el6                  @CentOS/6.9
    bash.x86_64                              4.1.2-48.el6                @CentOS/6.9
    bind-libs.x86_64                         32:9.8.2-0.62.rc1.el6_9.4   @Updates/6.9
    """

    for line in stream:
      line = ANSI_ESCAPE.sub('', line).translate(None, REMOVE_CHARS)  # strip sh control seq. and ascii control ch.
      pkg_name, _, line = line.lstrip().partition(" ")
      pkg_name, _, _ = str(pkg_name).rpartition(".")  # cut architecture from package name
      pkg_version, _, line = line.lstrip().partition(" ")
      if not pkg_version[:1].isdigit():
        continue
      repo_name, _, line = line.lstrip().partition(" ")

      if repo_name[:1] == "@":
        repo_name = repo_name[1:]

      if pkg_name and pkg_version and repo_name:
        yield pkg_name, pkg_version, repo_name
