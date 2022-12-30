#!/bin/bash

set -x

sh ./render.sh data_source.json.tpl data_source.json

if [ $? != 0 ]; then
  exit 1
fi

python ./main.py