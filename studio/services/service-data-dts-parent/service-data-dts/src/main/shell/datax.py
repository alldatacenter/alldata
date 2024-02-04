#!/usr/bin/env python
# -*- coding:utf-8 -*-

import sys
import os
import signal
import subprocess
import time
import re
import socket
import json
from optparse import OptionParser
from optparse import OptionGroup
from string import Template
import codecs
import platform

def printCopyright():
    print '''
AllDataDC (%s), From AllDataDC !
AllDataDC All Rights Reserved.

'''
    sys.stdout.flush()

if __name__ == "__main__":
	printCopyright()
	abs_file=sys.path[0]
	json_file=sys.argv[1]
	log_name=sys.argv[2]
	startCommand = "java -server -Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=E:\datax/log -Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=E:\datax/log -Dloglevel=info -Dfile.encoding=UTF-8 -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener -Djava.security.egd=file:///dev/urandom -Ddatax.home=E:\datax -Dlogback.configurationFile=E:\datax/conf/logback.xml -classpath E:\datax/lib/*  -Dlog.file.name=8e8e5f7d4cd0fd5_json com.alibaba.datax.core.Engine -mode standalone -jobid -1 -job %s > %s"  %(json_file,log_name)
	print startCommand
	child_process = subprocess.Popen(startCommand, shell=True)
	(stdout, stderr) = child_process.communicate()

	sys.exit(child_process.returncode)