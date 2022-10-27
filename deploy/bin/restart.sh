#!/bin/sh

source /etc/profile

cd $(dirname $0)

sh ./stop.sh

sh ./start.sh

