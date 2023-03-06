#!/usr/bin/env bash

set -x

source .venv/bin/activate
python3 -m pytest soda/core/tests/
