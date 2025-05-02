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
	startCommand = "java -cp %s/lib/* com.dtstack.chunjun.client.Launcher  -mode local -jobType sync -job %s -chunjunDistDir %s/chunjun-dist -flinkConfDir %s/flinkconf  > %s"  %(abs_file,json_file,abs_file,abs_file,log_name)
	print startCommand
	child_process = subprocess.Popen(startCommand, shell=True)
	(stdout, stderr) = child_process.communicate()

	sys.exit(child_process.returncode)
