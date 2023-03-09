#!/bin/bash
#
# Copyright 2022 The Koordinator Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cd $(dirname $0)/..
PWD=$(pwd)
BASENAME=$(basename $0)
OPTIONS=$@

function usage() {
    echo "Usage: "
    echo "  $BASENAME"
    echo "  $BASENAME test_pattern (Test* | *.go)"
}

function run_test() {
    pattern=$1
    if [[ $pattern == *.go ]]; then
        pattern=`basename $pattern`
        pkgs=`find ./pkg -name "$pattern" | xargs -i dirname {} | tr -s '\n' ' '`
        while read -r line
        do
            pkg=${line#".//"}
            echo go test -timeout 30s "github.com/koordinator-sh/koordinator/$pkg" -v
            go test -timeout 30s "github.com/koordinator-sh/koordinator/$pkg" -v
        done <<< "$pkgs"
    else
        pkgs=`grep "$pattern" -r ./pkg | cut -d: -f1 | grep -E ".go$" | xargs -i dirname {} | sort -u`
        while read -r line
        do
            pkg=${line#".//"}
            echo go test -timeout 30s "github.com/koordinator-sh/koordinator/$pkg" -run ^$pattern$ -v
            go test -timeout 30s "github.com/koordinator-sh/koordinator/$pkg" -run ^$pattern$ -v
        done <<< "$pkgs"
    fi
}

case $1 in
    -*)
        usage
        exit 0
        ;;
    "")
        go test -timeout 30s github.com/koordinator-sh/koordinator/pkg/... -v
        ;;
    *)
        run_test $1
        ;;
esac
