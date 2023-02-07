#!/usr/bin/env bash

flume-ng agent --conf ./conf/ --conf-file ./conf/test2.conf --name agent &

#监控
tail -f /app/hadoop/access.log