#!/bin/bash
set -e

if [ -n "$CUSTOM_SANITIZE" ] ; then
    exit 0
fi

UTESTS="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )/build"
cd $UTESTS
src/unit_tests_dbms --output-on-failure

