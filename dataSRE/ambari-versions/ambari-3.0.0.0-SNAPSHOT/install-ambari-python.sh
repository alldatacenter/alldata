#!/usr/bin/env bash
#
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

# requires: pip setuptools wheel

readlinkf(){
  # get real path on mac OSX
  perl -MCwd -e 'print Cwd::abs_path shift' "$1";
}

if [ "$(uname -s)" = 'Linux' ]; then
  SCRIPT_DIR="`dirname "$(readlink -f "$0")"`"
else
  SCRIPT_DIR="`dirname "$(readlinkf "$0")"`"
fi

function print_help() {
  cat << EOF
   Usage: ./install-ambari-python.sh [additional options]

   -c, --clean                  clean generated python distribution directories
   -d, --deploy                 deploy ambari-python artifact to maven remote repository
   -v, --version <version>      override ambari-python artifact versison
   -i, --repository-id <id>     repository id in settings.xml for remote repository
   -r, --repository-url <url>   repository url of remote repository
   -h, --help                   print help
EOF
}

function get_python_artifact_file() {
  local artifact_file=$(ls $SCRIPT_DIR/dist/ | head -n 1)
  echo $artifact_file
}

function get_version() {
  local artifact_file=$(get_python_artifact_file)
  local artifact_version=$(echo $artifact_file | perl -lne '/ambari-python-(.*?)\.tar\.gz/ && print $1')
  echo $artifact_version
}

function clean() {
  if [[ -d "$SCRIPT_DIR/dist" ]]; then
    echo "Removing '$SCRIPT_DIR/dist' directoy ..."
    rm -r "$SCRIPT_DIR/dist"
    echo "Directory '$SCRIPT_DIR/dist' successfully deleted."
  fi
  if [[ -d "$SCRIPT_DIR/ambari_python.egg-info" ]]; then
    echo "Removing '$SCRIPT_DIR/ambari_python.egg-info' directoy ..."
    rm -r "$SCRIPT_DIR/ambari_python.egg-info"
    echo "Directory '$SCRIPT_DIR/ambari_python.egg-info' successfully deleted."
  fi
  if [[ -d "$SCRIPT_DIR/target/ambari-python-dist" ]]; then
    echo "Removing '$SCRIPT_DIR/target/ambari-python' directoy ..."
    rm -r "$SCRIPT_DIR/target/ambari-python-dist"
    echo "Directory '$SCRIPT_DIR/target/ambari-python' successfully deleted."
  fi
}

function generate_site_packages() {
  local version="$1"
  pip install $SCRIPT_DIR/dist/ambari-python-$version.tar.gz -I --install-option="--prefix=$SCRIPT_DIR/target/ambari-python-dist"
}

function archive_python_dist() {
  local artifact="$1"
  local site_packages_dir=$(find $SCRIPT_DIR/target/ambari-python-dist -name "site-packages")
  local base_dir="`dirname $site_packages_dir`" # use this to make it work with different python versions
  if [[ -f "$SCRIPT_DIR/target/$artifact" ]]; then
    echo "Removing '$SCRIPT_DIR/target/$artifact' file ..."
    echo "File '$SCRIPT_DIR/target/$artifact' successfully deleted."
  fi
  tar -zcf $SCRIPT_DIR/target/$artifact -C $base_dir site-packages
}

function install() {
  local artifact_file="$1"
  local version="$2"
  mvn install:install-file -Dfile=$artifact_file -DgeneratePom=true -Dversion=$version -DartifactId=ambari-python -DgroupId=org.apache.ambari -Dpackaging=tar.gz
}

function deploy() {
  local artifact_file="$1"
  local version="$2"
  local repo_id="$3"
  local repo_url="$4"
  mvn gpg:sign-and-deploy-file -Dfile=$artifact_file -Dpackaging=tar.gz -DgeneratePom=true -Dversion=$version -DartifactId=ambari-python -DgroupId=org.apache.ambari -Durl="$repo_url" -DrepositoryId="$repo_url"
}

function run_setup_py() {
  local version="$1"
  if [[ ! -z "$version" ]]; then
    env AMBARI_VERSION="$version" python setup.py sdist
  else
    python setup.py sdist
  fi
}

function main() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -d|--deploy)
          local DEPLOY="true"
          shift 1
        ;;
        -c|--clean)
          local CLEAN="true"
          shift 1
        ;;
        -v|--version)
          local VERSION="$2"
          shift 2
        ;;
        -i|--repository-id)
          local REPOSITORY_ID="$2"
          shift 2
        ;;
        -r|--repository-url)
          local REPOSITORY_URL="$2"
          shift 2
        ;;
        -h|--help)
          shift 1
          print_help
          exit 0
        ;;
        *)
          echo "Unknown option: $1"
          exit 1
        ;;
      esac
  done

  if [[ -z "$DEPLOY" ]] ; then
    DEPLOY="false"
  fi

  clean
  if [[ "$CLEAN" == "true" ]]; then
    return 0
  fi

  run_setup_py "$VERSION"
  local artifact_name=$(get_python_artifact_file)
  local artifact_version=$(get_version)

  generate_site_packages "$artifact_version"
  archive_python_dist "$artifact_name"

  install "$SCRIPT_DIR/target/$artifact_name" "$artifact_version"

  if [[ "$DEPLOY" == "true" ]] ; then
    if [[ -z "$REPOSITORY_ID" ]] ; then
      echo "Repository id option is required  for deploying ambari-python artifact (-i or --repository-id)"
      exit 1
    fi
    if [[ -z "$REPOSITORY_URL" ]] ; then
      echo "Repository url option is required for deploying ambari-python artifact (-r or --repository-url)"
      exit 1
    fi
    deploy "$SCRIPT_DIR/target/$artifact_name" "$artifact_version" "$REPOSITORY_ID" "$REPOSITORY_URL"
  else
    echo "Skip deploying ambari-python artifact to remote repository."
  fi
}

main ${1+"$@"}
