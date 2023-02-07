#!/bin/bash

rm -rf ./dist_local | true
mkdir ./dist_local

files="dist public src typings babel.config.js vue.config.js package.json"
for f in $files; do
  copy -r $f dist_local/
done
