#!/usr/bin/env python
# encoding: utf-8
""" """
import web
import redis
import sys
import math

__author__ = 'adonis'


class RedisMode(object):
    standalone = 'standalone'
    cluster = 'cluster'
    sentinel = 'sentinel'


def create_redis_pool(host, port, passwd, pool_size=50, db=0, mode=RedisMode.standalone):
    # TODO: support sentinel and cluster mode
    if mode != RedisMode.standalone:
        raise Exception("Redis mode '%s' not supported" % mode)
    pool = redis.ConnectionPool(host=host, port=int(port), db=db,
                                password=passwd, socket_timeout=3)
    # redis_conn = redis.Redis(connection_pool=pool, socket_timeout=3)
    return pool
