#!/bin/bash
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

PYTHON_WRAPER_DIR="${ROOT}/usr/bin/"
PYTHON_WRAPER_TARGET="${PYTHON_WRAPER_DIR}/ambari-python-wrap"

# remove old python wrapper
rm -f "$PYTHON_WRAPER_TARGET"

AMBARI_PYTHON=""
python_binaries=( "/usr/bin/python" "/usr/bin/python2" "/usr/bin/python2.7" "/usr/bin/python2.6" )
for python_binary in "${python_binaries[@]}"
do
  $python_binary -c "import sys ; ver = sys.version_info ; sys.exit(not (ver >= (2,6) and ver<(3,0)))" 1>/dev/null 2>/dev/null

  if [ $? -eq 0 ] ; then
    AMBARI_PYTHON="$python_binary"
    break;
  fi
done

if [ -z "$AMBARI_PYTHON" ] ; then
  >&2 echo "Cannot detect python for ambari to use. Please manually set $PYTHON_WRAPER link to point to correct python binary"
else
  mkdir -p "$PYTHON_WRAPER_DIR"
  ln -s "$AMBARI_PYTHON" "$PYTHON_WRAPER_TARGET"
fi