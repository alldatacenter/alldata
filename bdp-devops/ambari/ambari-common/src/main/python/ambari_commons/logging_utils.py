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

_VERBOSE = False
_SILENT = False
_DEBUG_MODE = 0

# terminal styles
BOLD_ON = '\033[1m'
BOLD_OFF = '\033[0m'

def get_verbose():
  global _VERBOSE
  return _VERBOSE

def set_verbose(newVal):
  global _VERBOSE
  _VERBOSE = newVal

def get_silent():
  global _SILENT
  return _SILENT

def set_silent(newVal):
  global _SILENT
  _SILENT = newVal

def get_debug_mode():
  global _DEBUG_MODE
  return _DEBUG_MODE

def set_debug_mode(newVal):
  global _DEBUG_MODE
  _DEBUG_MODE = newVal

def set_debug_mode_from_options(options):
  debug_mode = 0
  try:
    if options.debug:
      debug_mode = 1
  except AttributeError:
    pass
  try:
    if options.suspend_start:
      debug_mode = debug_mode | 2
  except AttributeError:
    pass
  set_debug_mode(debug_mode)

#
# Prints an "info" messsage.
#
def print_info_msg(msg, forced=False):
  if forced:
    print("INFO: " + msg)
    return
  if _VERBOSE:
    print("INFO: " + msg)

#
# Prints an "error" messsage.
#
def print_error_msg(msg):
  print("ERROR: " + msg)

#
# Prints a "warning" messsage.
#
def print_warning_msg(msg, bold=False):
  if bold:
    print(BOLD_ON + "WARNING: " + msg + BOLD_OFF)
  else:
    print("WARNING: " + msg)
