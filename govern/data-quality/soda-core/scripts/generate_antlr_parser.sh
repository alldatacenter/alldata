#!/usr/bin/env bash

# set -x
set -e

ANTLR_FILE="antlr-4.11.1-complete.jar"
ANTLR_DOWNLOAD_URL="https://www.antlr.org/download/$ANTLR_FILE"
SCRIPTS_DIR=$(cd "$(dirname "$0")"; pwd -P)
ANTLR_LOCAL_JAR_PATH="$SCRIPTS_DIR/$ANTLR_FILE"
PARSER_DIR=$(cd "$SCRIPTS_DIR/../soda/core/soda/sodacl/antlr"; pwd -P)
PARSER_GRAMMAR_FILE=$PARSER_DIR/SodaCLAntlr.g4

if [ ! -f "$ANTLR_LOCAL_JAR_PATH" ]; then
    echo "$ANTLR_LOCAL_JAR_PATH does not exist.  Downloading..."
    curl -o $ANTLR_LOCAL_JAR_PATH $ANTLR_DOWNLOAD_URL
fi

ANTLR4_CMD="java -Xmx500M -cp $ANTLR_LOCAL_JAR_PATH org.antlr.v4.Tool"

$ANTLR4_CMD -Dlanguage=Python3 -o $PARSER_DIR -package soda.configuration.checks_parser -listener -visitor -lib $PARSER_DIR $PARSER_GRAMMAR_FILE

echo "Parser was generated at $(date +"%T")"
