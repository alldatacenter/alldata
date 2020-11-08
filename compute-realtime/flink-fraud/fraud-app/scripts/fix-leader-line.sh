#!/bin/bash

if grep -q 'module.exports = LeaderLine' ./node_modules/leader-line/leader-line.min.js; then
  echo 'Leader Line already patched'
else
  echo "\
    if (module && module.exports) { module.exports = LeaderLine }\
  " >> ./node_modules/leader-line/leader-line.min.js
  echo "Fixed LeaderLine!"
fi;


