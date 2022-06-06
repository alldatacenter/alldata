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
# limitations under the License


do_backups(){
  local etc_dir="/etc/ambari-agent"
  local var_dir="/var/lib/ambari-agent"
  local sudoers_dir="/etc/sudoers.d"

  # format: title note source target
  local backup_folders="stack folders::${var_dir}/cache/stacks:${var_dir}/cache/stacks_$(date '+%d_%m_%y_%H_%M').old
common services folder::${var_dir}/cache/common-services:${var_dir}/cache/common-services_$(date '+%d_%m_%y_%H_%M').old
ambari-agent.ini::${etc_dir}/conf/ambari-agent.ini:${etc_dir}/conf/ambari-agent.ini.old
sudoers:Please restore the file if you were using it for ambari-agent non-root functionality:${sudoers_dir}/ambari-agent:${sudoers_dir}/ambari-agent.bak"

  echo "${backup_folders}" | while IFS=: read title notes source target; do
    if [ -e "${source}" ]; then
      echo -n "Moving ${title}: ${source} -> ${target}"

      if [ ! -z ${notes} ]; then
        echo ", ${notes}"
      else
        echo ""
      fi

      mv -f "${source}" "${target}"
    fi
  done
}

do_backups

exit 0
