#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'


import redis
import threading
from common.config import get_config


# 同步锁
def synchronous_lock(func):
    def wrapper(*args, **kwargs):
        with threading.Lock():
            return func(*args, **kwargs)
    return wrapper


class RedisConnFactory(object):

    instance = None
    @synchronous_lock
    def __new__(cls, *args, **kwargs):
        if cls.instance is None:
            cls.instance = object.__new__(cls)
        return cls.instance

    def __init__(self):
        self._object_map = {}
        config = get_config()
        self.redis_conn_config = config.get('redis')

    def get_tsp_redis_conn(self):
        return self._get_redis_conn('tsp')

    def _get_redis_conn(self, name):
        if name in self._object_map:
            return self._object_map[name]
        else:
            r_config = self.redis_conn_config.get(name)
            r_conn = redis.Redis(
                host=r_config['host'],
                port=r_config['port'],
                db=r_config['db'],
                password=r_config['password']
            )
            self._object_map[name] = r_conn
            return r_conn
