#!/usr/bin/env python

'''
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
'''
import hashlib

import os, sys
import zipfile
import glob
import pprint


class KeeperException(Exception):
  pass

class ResourceFilesKeeper():
  """
  This class encapsulates all utility methods for resource files maintenance.
  """

  STACK_HOOKS_DIR= "stack-hooks"
  PACKAGE_DIR="package"
  STACKS_DIR="stacks"
  COMMON_SERVICES_DIR="common-services"
  CUSTOM_ACTIONS_DIR="custom_actions"
  HOST_SCRIPTS_DIR="host_scripts"
  DASHBOARDS_DIR="dashboards"
  EXTENSIONS_DIR="extensions"

  # For these directories archives are created
  ARCHIVABLE_DIRS = [PACKAGE_DIR]

  HASH_SUM_FILE=".hash"
  ARCHIVE_NAME="archive.zip"

  PYC_EXT=".pyc"
  METAINFO_XML = "metainfo.xml"

  BUFFER = 1024 * 32

  # Change that to True to see debug output at stderr
  DEBUG=False

  def __init__(self, resources_dir, stacks_dir, verbose=False, nozip=False):
    """
      nozip = create only hash files and skip creating zip archives
    """
    self.resources_dir = resources_dir
    self.stacks_root = stacks_dir
    self.verbose = verbose
    self.nozip = nozip


  def perform_housekeeping(self):
    """
    Performs housekeeping operations on resource files
    """
    self.update_directory_archives()
    # probably, later we will need some additional operations


  def _iter_update_directory_archive(self, subdirs_list):
    for subdir in subdirs_list:
      for root, dirs, _ in os.walk(subdir, followlinks=True):
        for d in dirs:
          if d in self.ARCHIVABLE_DIRS:
            full_path = os.path.abspath(os.path.join(root, d))
            self.update_directory_archive(full_path)

  def _update_resources_subdir_archive(self, subdir):
    archive_root = os.path.join(self.resources_dir, subdir)
    self.dbg_out("Updating archive for {0} dir at {1}...".format(subdir, archive_root))

    # update the directories so that the .hash is generated
    self.update_directory_archive(archive_root)

  def update_directory_archives(self):
    """
    Please see AMBARI-4481 for more details
    """
    # archive stacks
    self.dbg_out("Updating archives for stack dirs at {0}...".format(self.stacks_root))
    valid_stacks = self.list_stacks(self.stacks_root)
    self.dbg_out("Stacks: {0}".format(pprint.pformat(valid_stacks)))
    # Iterate over stack directories
    self._iter_update_directory_archive(valid_stacks)

    # archive common services
    common_services_root = os.path.join(self.resources_dir, self.COMMON_SERVICES_DIR)
    self.dbg_out("Updating archives for common services dirs at {0}...".format(common_services_root))
    valid_common_services = self.list_common_services(common_services_root)
    self.dbg_out("Common Services: {0}".format(pprint.pformat(valid_common_services)))
    # Iterate over common services directories
    self._iter_update_directory_archive(valid_common_services)

    # archive extensions
    extensions_root = os.path.join(self.resources_dir, self.EXTENSIONS_DIR)
    self.dbg_out("Updating archives for extensions dirs at {0}...".format(extensions_root))
    valid_extensions = self.list_extensions(extensions_root)
    self.dbg_out("Extensions: {0}".format(pprint.pformat(valid_extensions)))
    # Iterate over extension directories
    self._iter_update_directory_archive(valid_extensions)

    # stack hooks
    self._update_resources_subdir_archive(self.STACK_HOOKS_DIR)

    # custom actions
    self._update_resources_subdir_archive(self.CUSTOM_ACTIONS_DIR)

    # agent host scripts
    self._update_resources_subdir_archive(self.HOST_SCRIPTS_DIR)

    # custom service dashboards
    self._update_resources_subdir_archive(self.DASHBOARDS_DIR)


  def _list_metainfo_dirs(self, root_dir):
    valid_items = []  # Format: <stack_dir, ignore(True|False)>
    glob_pattern = "{0}/*/*".format(root_dir)
    dirs = glob.glob(glob_pattern)
    for directory in dirs:
      metainfo_file = os.path.join(directory, self.METAINFO_XML)
      if os.path.exists(metainfo_file):
        valid_items.append(directory)
    return valid_items

  def list_stacks(self, root_dir):
    """
    Builds a list of stack directories
    """
    try:
      return self._list_metainfo_dirs(root_dir)
    except Exception, err:
      raise KeeperException("Can not list stacks: {0}".format(str(err)))

  def list_common_services(self, root_dir):
    """
    Builds a list of common services directories
    """
    try:
      return self._list_metainfo_dirs(root_dir)
    except Exception, err:
      raise KeeperException("Can not list common services: {0}".format(str(err)))

  def list_extensions(self, root_dir):
    """
    Builds a list of extension directories
    """
    try:
      return self._list_metainfo_dirs(root_dir)
    except Exception, err:
      raise KeeperException("Can not list extensions: {0}".format(str(err)))

  def update_directory_archive(self, directory):
    """
    If hash sum for directory is not present or differs from saved value,
    recalculates hash sum and creates directory archive. The archive is
    also created if the existing archive does not exist, even if the
    saved and current hash sums are matching.
    """
    skip_empty_directory = True

    cur_hash = self.count_hash_sum(directory)
    saved_hash = self.read_hash_sum(directory)

    directory_archive_name = os.path.join(directory, self.ARCHIVE_NAME)

    if cur_hash != saved_hash:
      if not self.nozip:
        self.zip_directory(directory, skip_empty_directory)
      # Skip generation of .hash file is directory is empty
      if (skip_empty_directory and (not os.path.exists(directory) or not os.listdir(directory))):
        self.dbg_out("Empty directory. Skipping generation of hash file for {0}".format(directory))
      else:
        self.write_hash_sum(directory, cur_hash)
      pass
    elif not os.path.isfile(directory_archive_name):
      self.zip_directory(directory, skip_empty_directory)

  def count_hash_sum(self, directory):
    """
    Recursively counts hash sum of all files in directory and subdirectories.
    Files and directories are processed in alphabetical order.
    Ignores previously created directory archives and files containing
    previously calculated hashes. Compiled pyc files are also ignored
    """
    try:
      sha1 = hashlib.sha1()
      file_list = []
      for root, dirs, files in os.walk(directory):
        for f in files:
          if not self.is_ignored(f):
            full_path = os.path.abspath(os.path.join(root, f))
            file_list.append(full_path)
      file_list.sort()
      for path in file_list:
        self.dbg_out("Counting hash of {0}".format(path))
        with open(path, 'rb') as fh:
          while True:
            data = fh.read(self.BUFFER)
            if not data:
              break
            sha1.update(data)
      return sha1.hexdigest()
    except Exception, err:
      raise KeeperException("Can not calculate directory "
                            "hash: {0}".format(str(err)))


  def read_hash_sum(self, directory):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    if os.path.isfile(hash_file):
      try:
        with open(hash_file) as fh:
          return fh.readline().strip()
      except Exception, err:
        raise KeeperException("Can not read file {0} : {1}".format(hash_file,
                                                                   str(err)))
    else:
      return None


  def write_hash_sum(self, directory, new_hash):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file, "w") as fh:
        fh.write(new_hash)
      os.chmod(hash_file, 0o644)
    except Exception, err:
      raise KeeperException("Can not write to file {0} : {1}".format(hash_file,
                                                                   str(err)))

  def zip_directory(self, directory, skip_if_empty = False):
    """
    Packs entire directory into zip file. Hash file is also packaged
    into archive
    """
    self.dbg_out("creating archive for directory {0}".format(directory))
    try:
      if skip_if_empty:
        if not os.path.exists(directory) or not os.listdir(directory):
          self.dbg_out("Empty directory. Skipping archive creation for {0}".format(directory))
          return

      zip_file_path = os.path.join(directory, self.ARCHIVE_NAME)
      zf = zipfile.ZipFile(zip_file_path, "w")
      abs_src = os.path.abspath(directory)
      for root, dirs, files in os.walk(directory):
        for filename in files:
          # Avoid zipping previous archive and hash file and binary pyc files
          if not self.is_ignored(filename):
            absname = os.path.abspath(os.path.join(root, filename))
            arcname = absname[len(abs_src) + 1:]
            self.dbg_out('zipping %s as %s' % (os.path.join(root, filename),
                                        arcname))
            zf.write(absname, arcname)
      zf.close()
      os.chmod(zip_file_path, 0o755)
    except Exception, err:
      raise KeeperException("Can not create zip archive of "
                            "directory {0} : {1}".format(directory, str(err)))


  def is_ignored(self, filename):
    """
    returns True if filename is ignored when calculating hashing or archiving
    """
    return filename in [self.HASH_SUM_FILE, self.ARCHIVE_NAME] or \
           filename.endswith(self.PYC_EXT)


  def dbg_out(self, text):
    if self.DEBUG:
      sys.stderr.write("{0}\n".format(text))
    if not self.DEBUG and self.verbose:
      print text


def main(argv=None):
  """
  This method is called by maven during rpm creation.
  Params:
    1: Path to resources root directory
  """
  res_path = argv[1]

  if len(argv) >= 3:
    stacks_path = argv[2]
  else:
    stacks_path = os.path.join(res_path, ResourceFilesKeeper.STACKS_DIR)

  resource_files_keeper = ResourceFilesKeeper(res_path, stacks_path, nozip=True)
  resource_files_keeper.perform_housekeeping()


if __name__ == '__main__':
  main(sys.argv)
