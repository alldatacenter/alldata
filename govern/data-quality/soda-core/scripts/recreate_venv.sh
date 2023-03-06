#!/usr/bin/env bash

set -e

# Run this from the root project dir with scripts/recreate_venv.sh

rm -rf .venv
rm -rf soda_sql.egg-info

python3 -m venv .venv
# shellcheck disable=SC1091
source .venv/bin/activate
pip install --upgrade pip
pip install "$(grep pip-tools < dev-requirements.in )"
pip-compile dev-requirements.in
pip install -r dev-requirements.txt
pip install pre-commit

cat requirements.txt | while read requirement || [[ -n $requirement ]];
do
   pip install -e $requirement
done
