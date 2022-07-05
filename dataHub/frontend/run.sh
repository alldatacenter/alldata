#!/bin/bash

# 记录发布日志
function record_release_log() {
    now=$(date +'%Y-%m-%d %H:%M:%S')
    commit=$(git log --format=oneline -n 1)
    echo "$now $commit" >> release.log
}

case $1 in
    build:prod)
        echo "npm run build:prod"
        record_release_log
        npm run build:prod
        ;;
    build:test)
        echo "npm run build:test"
        record_release_log
        npm run build:test
        ;;
    *)
        echo ""
        echo "Use: ./run.sh <command>"
        echo ""
        echo "👉  You must specify a parameter, available parameters:"
        echo ""
        echo "   build:prod"
        echo "   build:test"
        echo ""
        echo "Example: ./run.sh start"
        echo ""
        ;;
esac
