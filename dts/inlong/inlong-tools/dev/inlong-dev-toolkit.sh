#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# Initialize the configuration files of inlong components

script_dir=$(dirname "$0")
script_file_name=$(basename "$0")

# the absolute dir of project
base_dir=$(
  cd $script_dir
  cd ../..
  pwd
)

help_action=(
  'help'
  'h'
  'help: show all actions'
  'welcome'
)

manager_action=(
  'manager'
  'm'
  'build manager local debugging environment'
  'manager'
)

actions=(
  help_action
  manager_action
)

function welcome() {
  local_date_time=$(date +"%Y-%m-%d %H:%M:%S")
  echo '####################################################################################'
  echo '#                 Welcome to use Apache InLong dev toolkit !                       #'
  echo '#                                        @'$local_date_time'                      #'
  echo '####################################################################################'
  echo ''

  # shellcheck disable=SC2068
  for action in ${actions[@]}; do
    # shellcheck disable=SC1087
    TMP=$action[@]
    TempB=("${!TMP}")
    name=${TempB[0]}
    simple_name=${TempB[1]}
    desc=${TempB[2]}

    echo $script_dir'/'$script_file_name' '$name' | '$simple_name
    echo '      :'$desc
  done
  echo ''
}

function manager() {
  echo '# start build manager local debugging environment ...'

  project_version=$(mvn -q \
    -Dexec.executable=echo \
    -Dexec.args='${project.version}' \
    --non-recursive \
    exec:exec)

  echo 'current_version: '"$project_version"
  #
  echo 'associate plugins directory: inlong-manager/manager-plugins/target/plugins'
  # plugins -> manager-plugins/target/plugins
  cd "$base_dir"
  rm -rf plugins
  ln -s inlong-manager/manager-plugins/target/plugins plugins
  #
  echo 'associate sort dist: inlong-sort/sort-dist/target/sort-dist'
  cd "$base_dir"/inlong-sort
  rm -rf sort-dist.jar
  ln -s sort-dist/target/sort-dist-"$project_version".jar sort-dist.jar
  # inlong-manager:    plugins -> manager-plugins/target/plugins
  cd "$base_dir"/inlong-manager
  rm -rf plugins
  ln -s manager-plugins/target/plugins plugins
  #   mkdir inlong-sort/connectors
  sort_connector_dir=$base_dir/inlong-sort/connectors
  echo 'recreate connector dir: '"$sort_connector_dir"
  # shellcheck disable=SC2086
  rm -rf $sort_connector_dir
  mkdir "$sort_connector_dir"
  cd "$sort_connector_dir"
  connector_names=$(grep '<module>' "$base_dir"/inlong-sort/sort-connectors/pom.xml | sed 's/<module>//g' | sed 's/<\/module>//g' | grep -v base)

  echo 'All connector names: '
  echo $connector_names | tr -d '\n'

  for connector_name in $connector_names; do
    echo 'associate connector: '"$connector_name"
    connector_suffix_name=$(echo "$connector_name" | sed 's/elasticsearch-6/elasticsearch6/g' | sed 's/elasticsearch-7/elasticsearch7/g')
    ln -s ../sort-connectors/"${connector_name}"/target/sort-connector-"${connector_suffix_name}"-"$project_version".jar sort-connector-"${connector_name}".jar
  done

  echo 'build dev env of manager finished .'
}

function main() {
  if [[ "$OSTYPE" != "darwin"* ]] && [[ "$OSTYPE" != "linux-gnu"* ]]; then
    echo "This script only supports macOS or Linux, current OS is $OSTYPE"
    exit 1
  fi

  action=$1

  if [ ! -n "$action" ]; then
    welcome
  fi

  # shellcheck disable=SC2068
  for one_action in ${actions[@]}; do
    # shellcheck disable=SC1087
    TMP=$one_action[@]
    TempB=("${!TMP}")
    name=${TempB[0]}
    simple_name=${TempB[1]}
    function_name=${TempB[3]}
    desc=${TempB[4]}

    if [[ X"$name" == X"$action" ]] || [[ X"$simple_name" == X"$action" ]]; then
      echo 'Execute action: '"$function_name"
      $function_name
    fi
  done

  echo 'Have a nice day, bye!'
}

main $1
