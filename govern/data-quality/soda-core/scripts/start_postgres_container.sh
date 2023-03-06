#!/usr/bin/env bash

# Run this from the root project dir with scripts/start_postgres_container.sh

# POSTGRES_DATA_DIR=./tests/postgres_container/.postgres
# if [ -d "$POSTGRES_DATA_DIR" ]; then
#   rm -rf "$POSTGRES_DATA_DIR"
# fi

docker-compose -f soda/postgres/docker-compose.yml up --remove-orphans
