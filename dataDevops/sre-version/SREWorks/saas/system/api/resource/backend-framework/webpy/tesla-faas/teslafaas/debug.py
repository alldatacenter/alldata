#!/home/tops/bin/python
# encoding:utf-8

"""
    启动开发模式，每次修改bigdatak, 当前faas下的代码都会自动重启server
"""

__author__ = 'aiyu.zyj'

import os
import logging
import sys
import inotify.adapters
import subprocess
import os
import signal

class DebugFaas():

    def __init__(self, service=None, bigdatak=None, start=None, *args):
 
        self.set_logger()
        self.product_dir = service
        self.bigdatak_dir = bigdatak
        self.gevent_start_sh = start
        self.subp = None

    def set_logger(self):
        _DEFAULT_LOG_FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        _LOGGER = logging.getLogger()
        _LOGGER.setLevel(logging.INFO)
        ch = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter(_DEFAULT_LOG_FORMAT)
        ch.setFormatter(formatter)
        _LOGGER.addHandler(ch)
        self.logger = _LOGGER

    def kill_child_processes(self, parent_pid, sig=signal.SIGTERM):
        print(parent_pid)
        print(type(parent_pid))
        ps_command = subprocess.Popen("ps -o pid --ppid %s --noheaders" % parent_pid, shell=True, stdout=subprocess.PIPE)
        ps_output = ps_command.stdout.read()
        retcode = ps_command.wait()
        assert retcode == 0, "ps command returned %d" % retcode
        for pid_str in ps_output.split("\n")[:-1]:
                os.kill(int(pid_str), sig)


    def restart_server(self):
        if self.subp: 
            ppid = self.subp.pid
            self.kill_child_processes(ppid)
            self.subp.wait()
        subp = subprocess.Popen(self.gevent_start_sh, shell=True)
        self.subp = subp

    def run(self):
        self.restart_server()
        self.notify()

    def notify(self):
        print 'add watch ',self.product_dir
        print 'add watch ',self.bigdatak_dir
        i = inotify.adapters.InotifyTrees([self.product_dir, self.bigdatak_dir])
        for event in i.event_gen():
            if event is not None:
                (header, type_names, watch_path, filename) = event
                if filename.startswith('.'):  # vim tmp file 
                    continue
                if not filename.endswith('.py'):  
                    continue
                type = type_names[0]
                if type in ('IN_CLOSE_WRITE'):
                    self.logger.info("TYPE=%s " "WATCH-PATH=[%s] FILENAME=[%s]", type_names, watch_path.decode('utf-8'), filename.decode('utf-8'))
                    self.restart_server()

if __name__ == "__main__":
    mys = BigdataCloudConsole('x')
    mys.run() 
