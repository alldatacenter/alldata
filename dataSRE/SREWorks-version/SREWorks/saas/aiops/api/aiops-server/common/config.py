#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import os
import json
import threading

env = os.getenv('APP_ENV', 'test')
data_source_config = {
    'test': 'data_source_test.json',
    'prod': 'data_source.json'
}

__conf = dict()
lock = threading.Lock()


def get_config():
    lock.acquire()
    if not __conf:
        cur_dir = os.path.dirname(os.path.split(os.path.realpath(__file__))[0]) + '/'
        if os.path.exists(cur_dir + data_source_config.get(env)):
            __conf.update(json.loads(open(cur_dir + data_source_config.get(env), 'r').read()))
    lock.release()

    return __conf
