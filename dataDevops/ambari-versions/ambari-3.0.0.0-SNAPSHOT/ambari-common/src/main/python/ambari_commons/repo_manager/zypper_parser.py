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


class ZypperParser(GenericParser):
  @staticmethod
  def config_reader(stream):
    # ToDo: future feature
    raise NotImplementedError()

  @staticmethod
  def packages_reader(stream):
    """
    :type stream collections.Iterable

    Sample text:

    Loading repository data...
    Reading installed packages...

    S | Name                 | Type    | Version                  | Arch   | Repository
    --+----------------------+---------+--------------------------+--------+-------------------------------------------------
    i | ConsoleKit           | package | 0.2.10-64.67.1           | x86_64 | SLES11-Pool
    i | ConsoleKit           | package | 0.2.10-64.67.1           | x86_64 | SUSE11.4.4-1.109
    i | PolicyKit            | package | 0.9-14.43.1              | x86_64 | SLES11-Pool
    i | PolicyKit            | package | 0.9-14.43.1              | x86_64 | SUSE11.4.4-1.109
    i | PolicyKit-doc        | package | 0.9-14.43.1              | x86_64 | SLES11-Pool
    i | PolicyKit-doc        | package | 0.9-14.43.1              | x86_64 | SUSE11 11.4.4-1.109
    """
    split_ch = "|"

    for line in stream:
      line = ANSI_ESCAPE.sub('', line).translate(None, REMOVE_CHARS)

      # reading second column instantly, first one is not interesting for us
      pkg_name, _, line = line.partition(split_ch)[2].partition(split_ch)

      pkg_version, _, line = line.lstrip().partition(split_ch)[2].partition(split_ch)
      if not pkg_version.strip()[:1].isdigit():
        continue

      repo_name, _, line = line.lstrip().partition(split_ch)[2].partition(split_ch)

      pkg_name = pkg_name.strip()
      repo_name = repo_name.strip()
      pkg_version = pkg_version.strip()

      if pkg_name and pkg_version and repo_name:
        yield pkg_name, pkg_version, repo_name

  @staticmethod
  def packages_installed_reader(stream):
    yield ZypperParser.packages_reader(stream)

  @staticmethod
  def lookup_packages(lines, skip_till="--+--"):
    """
    Deprecated and not used function, would be removed in later releases.

    :type lines str
    :type skip_till str|None
    """
    packages = []
    skip_index = None

    lines = [line.strip() for line in lines]
    for index in range(len(lines)):
      if skip_till in lines[index]:
        skip_index = index + 1
        break

    if skip_index:
      for line in lines[skip_index:]:
        items = line.strip(' \t\n\r').split('|')
        packages.append([items[1].strip(), items[3].strip(), items[5].strip()])

    return packages

  @staticmethod
  def repo_list_reader(stream):
    """
    :type stream collections.Iterable

    Sample text:

    Novel SuSe

    # | Alias                               | Name                                             | Enabled | Refresh
    --+-------------------------------------+--------------------------------------------------+---------+--------
    1 | SUSE-Linux                          | SUSE-Linux                                       | Yes     | No
    2 | SLE11-Public-Cloud-Module           | SLE11-Public-Cloud-Module                        | No      | Yes
    3 | SLE11-SP4-Debuginfo-Pool            | SLE11-SP4-Debuginfo-Pool                         | No      | Yes
    4 | SLE11-SP4-Debuginfo-Updates         | SLE11-SP4-Debuginfo-Updates                      | No      | Yes
    5 | SLE11-Security-Module               | SLE11-Security-Module                            | No      | Yes
    6 | SLES11-Extras                       | SLES11-Extras                                    | No      | Yes
    7 | SLES11-SP4-Pool                     | SLES11-SP4-Pool                                  | Yes     | Yes
    8 | SLES11-SP4-Updates                  | SLES11-SP4-Updates                               | Yes     | Yes


    OpenSuse (have extra columns)

    # | Alias          | Name           | Enabled | GPG Check | Refresh
    --+----------------+----------------+---------+-----------+--------
    1 | NON OSS        | NON OSS        | Yes     | (r ) Yes  | Yes
    2 | NON OSS Update | NON OSS Update | Yes     | (r ) Yes  | Yes
    3 | OSS            | OSS            | Yes     | (r ) Yes  | Yes
    4 | OSS Update     | OSS Update     | Yes     | (r ) Yes  | Yes
    """
    split_ch = "|"

    for line in stream:
      line = ANSI_ESCAPE.sub('', line).translate(None, REMOVE_CHARS)
      repo_seq, _, line = line.partition(split_ch)
      if not repo_seq.strip()[:1].isdigit():
        continue

      repo_alias, _, line = line.lstrip().partition(split_ch)
      repo_name, _, line = line.lstrip().partition(split_ch)
      repo_enabled, _, line = line.lstrip().partition(split_ch)
      repo_refresh, _, line = line.lstrip().partition(split_ch)

      if "(" in repo_refresh:  # skip "GPG Check column"
        repo_refresh, _, line = line.lstrip().partition(split_ch)

      repo_alias = repo_alias.strip()
      repo_name = repo_name.strip()
      repo_enabled = repo_enabled.lower() == "yes"
      repo_refresh = repo_refresh.lower() == "yes"

      if repo_alias and repo_name:
        yield repo_alias, repo_name, repo_enabled, repo_refresh
