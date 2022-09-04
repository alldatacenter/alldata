#!/bin/bash

set -x

file_name=$1
dest_name=$2

echo "${file_name}"
echo "${dest_name}"

ENV_ARG=$(awk 'BEGIN{for(v in ENVIRON) printf "${%s} ", v;}')
envsubst "${ENV_ARG}" <${file_name}> ${dest_name}