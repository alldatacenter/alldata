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

import os
import shlex
from ambari_commons import subprocess32
import argparse

JAR_FILE_PERMISSIONS = 644
DIRECTORY_PERMISSIONS = 755
FILE_PERMISSIONS = 755
SECURE_DIRECTORY_PERMISSIONS = 700
SECURE_FILE_PERMISSIONS = 700

# List of directories with jar files or path to jar file. If "directory", then we will check all jar files in it and in all subdirectories. If jar "file" then we will check only this file.
jar_files_to_check = ["/var/lib/ambari-server/", "/usr/lib/ambari-server/", "/var/lib/ambari-agent/"]

# List of directories. For this list we are only checking permissions for directory.
directories_to_check = ["/etc/ambari-server/conf", "/usr/lib/ambari-server", "/usr/lib/ambari-server/lib/ambari_server",
                        "/var/lib/ambari-server", "/usr/lib/ambari-agent", "/usr/lib/ambari-agent/lib/ambari_agent",
                        "/var/lib/ambari-agent/cache", "/var/lib/ambari-agent/cred", "/var/lib/ambari-agent/data",
                        "/var/lib/ambari-agent/tools", "/var/lib/ambari-agent/lib", "/etc/ambari-agent/conf"]

# List of directories/files. If "directory", then we will check all files in it and in all subdirectories. If "file" then we will check only this file.
files_to_check = ["/etc/ambari-server/conf/", "/etc/init/ambari-server.conf", "/etc/init.d/ambari-server",
                  "/usr/lib/ambari-server", "/usr/lib/ambari-server/lib/ambari_server", "/usr/sbin/ambari_server_main.py",
                  "/usr/sbin/ambari-server.py", "/var/lib/ambari-server", "/usr/lib/ambari-agent",
                  "/usr/lib/ambari-agent/lib/ambari_agent", "/var/lib/ambari-agent"]


# List of secure directories. For this list we are only checking permissions for directory.
secure_directories_to_check = ["/var/lib/ambari-server/keys","/var/lib/ambari-agent/keys"]

# List of secure directories/files. If "directory", then we will check all files in it and in all subdirectories. If "file" then we will check only this file.
secure_files_to_check = ["/var/lib/ambari-server/keys", "/var/lib/ambari-agent/keys"]



def main():
  parser = argparse.ArgumentParser(
    description='This script search for ambari files with incorrect permissions.',
    epilog='Only for ambari!'
  )

  # options
  parser.add_argument('--ambari-root-dir', type=str, default='/',
                      action='store', help='Ambari server root directory. By default it is "/".')

  args = parser.parse_args()
  do_work(args)


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "":  # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)

def check_directory_permissions(dir_path, perm):
  print "Checking directory " + dir_path + ":"
  directories_with_wrong_permissions = []
  # check directory permissions
  directories_with_wrong_permissions = []
  if os.path.exists(dir_path):
    (retcode, stdout, stderr) = os_run_os_command("find " + str(dir_path) + " -type d -perm " + str(perm))
    if retcode > 0:
      print "ERROR: failed to check permissions for directory " + str(dir_path) + ": " + str(stderr) + "\n"

    if stdout and not stdout == "":
      directories_with_wrong_permissions = directories_with_wrong_permissions + stdout.splitlines()
  else:
    print "ERROR: directory " + dir_path + " doesn't exist!\n"

  return directories_with_wrong_permissions


def check_files_in_directory_or_file_for_permissions(path, perm):
  files_with_wrong_permissions = []
  if os.path.exists(path):
    if os.path.isdir(path):
      # check files in directory
      print "Checking files in directory " + path
      (retcode, stdout, stderr) = os_run_os_command("find " + str(path) + " -type f -perm " + str(perm))
      if retcode > 0:
        print "ERROR: failed to check permissions for files in " + str(path) + ": " + str(stderr) + "\n"

    elif os.path.isfile(path):
      # check file for permissions
      print "Checking file " + path + ":"
      (retcode, stdout, stderr) = os_run_os_command("find " + str(path) + " -type f -perm " + str(perm))
      if retcode > 0:
        print "ERROR: failed to check permissions for directory " + str(path) + ": " + str(stderr) + "\n"

    if stdout and not stdout == "":
      files_with_wrong_permissions = files_with_wrong_permissions + stdout.splitlines()
  else:
    print "ERROR: directory/file " + path + " doesn't exist!\n"

  return files_with_wrong_permissions


def update_permissions(list_of_paths, permissions, ask_msg):
  if list_of_paths:
    fix_permissions = get_YN_input(ask_msg + " [y/n] (y)? ", True)
    if fix_permissions:
      for path in list_of_paths:
        (retcode, stdout, stderr) = os_run_os_command("chmod " + str(permissions) + " " + str(path))
        if retcode > 0:
          print "ERROR: failed to update permissions" + str(permissions) + " for " + str(path) + ": " + str(stderr) + "\n"


def print_paths_with_wrong_permissions(list_of_paths):
  for path in list_of_paths:
    (retcode, stdout, stderr) = os_run_os_command("stat -c \"%A %a %n\" " + str(path))
    if retcode > 0:
      print "ERROR: failed to get permissions for path " + str(path) + ": " + str(stderr) + "\n"
    else:
      print  str(stdout).rstrip("\n")


def do_work(args):
  print "\n*****Check file, or files in directory for valid permissions (without w for group and other)*****"
  files_with_wrong_permissions = []
  for path in files_to_check:
    path = os.path.join(args.ambari_root_dir, path.lstrip('/'))
    files_with_wrong_permissions = files_with_wrong_permissions + check_files_in_directory_or_file_for_permissions(path, "/g=w,o=w")

  if files_with_wrong_permissions:
    print "\nFiles with wrong permissions:"
    print_paths_with_wrong_permissions(files_with_wrong_permissions)
    update_permissions(files_with_wrong_permissions, FILE_PERMISSIONS, "Fix permissions for files to " + str(FILE_PERMISSIONS) + " (recommended) ")

  print "\n*****Check ambari jar file, or files in directory, for valid permissions (without w+x for group and other)*****"
  jar_files_with_wrong_permissions = []
  for jar_path in jar_files_to_check:
    jar_path = os.path.join(args.ambari_root_dir, jar_path.lstrip('/'))
    if os.path.exists(jar_path):
      if os.path.isdir(jar_path):
        # check files in directory for permissions
        print "Checking jars in " + str(jar_path)
        (retcode, stdout, stderr) = os_run_os_command("find " + str(jar_path) + " -type f -name *.jar -perm /g=w+x,o=w+x")
        if retcode > 0:
          print "ERROR: failed to check permissions for jar files in " + str(jar_path) + ": " + str(stderr) + "\n"

      elif os.path.isfile(jar_path):
        # check file for permissions
        print "Checking jar " + str(jar_path)
        (retcode, stdout, stderr) = os_run_os_command("find " + str(jar_path) + " -type f -perm /g=w+x,o=w+x")
        if retcode > 0:
          print "ERROR: failed to check permissions for file " + str(jar_path) + ": " + str(stderr) + "\n"

      if stdout and not stdout == "":
        jar_files_with_wrong_permissions = jar_files_with_wrong_permissions + stdout.splitlines()
    else:
      print "ERROR: directory " + jar_path + " doesn't exist!\n"

  if jar_files_with_wrong_permissions:
    print "\nJar files with wrong permissions:"
    print_paths_with_wrong_permissions(jar_files_with_wrong_permissions)
    update_permissions(jar_files_with_wrong_permissions, JAR_FILE_PERMISSIONS, "Fix permissions for jar files to " + str(JAR_FILE_PERMISSIONS) + " (recommended) ")


  print "\n*****Check directories for valid permissions (without w for group and other)*****"
  directories_with_wrong_permissions = []
  for dir_path in directories_to_check:
    dir_path = os.path.join(args.ambari_root_dir, dir_path.lstrip('/'))
    directories_with_wrong_permissions = directories_with_wrong_permissions + check_directory_permissions(dir_path, "/g=w,o=w")

  if directories_with_wrong_permissions:
    print "\nDirectories with wrong permissions:"
    print_paths_with_wrong_permissions(directories_with_wrong_permissions)
    update_permissions(directories_with_wrong_permissions, DIRECTORY_PERMISSIONS, "Fix permissions for directories to " + str(DIRECTORY_PERMISSIONS) + " (recommended) ")

  print "\n*****Check secure directories for valid permissions (without r+w+x for group and other)*****"
  secure_directories_with_wrong_permissions = []
  for dir_path in secure_directories_to_check:
    dir_path = os.path.join(args.ambari_root_dir, dir_path.lstrip('/'))
    secure_directories_with_wrong_permissions = secure_directories_with_wrong_permissions + check_directory_permissions(dir_path, "/g=r+w+x,o=r+w+x")

  if secure_directories_with_wrong_permissions:
    print "\nSecure directories with wrong permissions:"
    print_paths_with_wrong_permissions(secure_directories_with_wrong_permissions)
    update_permissions(secure_directories_with_wrong_permissions, SECURE_DIRECTORY_PERMISSIONS, "Fix permissions for secure directories to " + str(SECURE_DIRECTORY_PERMISSIONS) + " (recommended) ")

  print "\n*****Check secure file, or files in directory for valid permissions (without r+w+x for group and other)*****"
  secure_files_with_wrong_permissions = []
  for path in secure_files_to_check:
    path = os.path.join(args.ambari_root_dir, path.lstrip('/'))
    secure_files_with_wrong_permissions = secure_files_with_wrong_permissions + check_files_in_directory_or_file_for_permissions(path, "/g=r+w+x,o=r+w+x")

  if secure_files_with_wrong_permissions:
    print "\nSecure files with wrong permissions:"
    print_paths_with_wrong_permissions(secure_files_with_wrong_permissions)
    update_permissions(secure_files_with_wrong_permissions, SECURE_FILE_PERMISSIONS, "Fix permissions for secure files to " + str(SECURE_FILE_PERMISSIONS) + " (recommended) ")

  print "\nCheck completed."


def os_run_os_command(cmd, env=None, shell=False, cwd=None):
  if type(cmd) == str:
    cmd = shlex.split(cmd)
  process = subprocess32.Popen(cmd,
                             stdout=subprocess32.PIPE,
                             stdin=subprocess32.PIPE,
                             stderr=subprocess32.PIPE,
                             env=env,
                             cwd=cwd,
                             shell=shell
  )

  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata






if __name__ == "__main__":
  main()
