rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information rega4rding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem ##################################################################
rem #                      SERVER INSTALL HELPER                     #
rem ##################################################################

set COMMON_DIR="/usr/lib/python2.6/site-packages/common_functions"
set INSTALL_HELPER_AGENT="/var/lib/ambari-agent/install-helper.sh"
set COMMON_DIR_SERVER="/usr/lib/ambari-server/lib/common_functions"

set PYTHON_WRAPER_TARGET="/usr/bin/ambari-python-wrap"
set PYTHON_WRAPER_SOURCE="/var/lib/ambari-server/ambari-python-wrap"

do_install(){
  # setting common_functions shared resource
  if [ ! -d "$COMMON_DIR" ]; then
    ln -s "$COMMON_DIR_SERVER" "$COMMON_DIR"
  fi
  # setting python-wrapper script
  if [ ! -f "$PYTHON_WRAPER_TARGET" ]; then
    ln -s "$PYTHON_WRAPER_SOURCE" "$PYTHON_WRAPER_TARGET"
  fi
}

do_remove(){
  if [ -d "$COMMON_DIR" ]; then  # common dir exists
    rm -f "$COMMON_DIR"
  fi

  if [ -f "$PYTHON_WRAPER_TARGET" ]; then
    rm -f "$PYTHON_WRAPER_TARGET"
  fi

  # if server package exists, restore their settings
  if [ -f "$INSTALL_HELPER_AGENT" ]; then  #  call agent shared files installer
      $INSTALL_HELPER_AGENT install
  fi
}


case "$1" in
install)
  do_install
  ;;
remove)
  do_remove
  ;;
esac
