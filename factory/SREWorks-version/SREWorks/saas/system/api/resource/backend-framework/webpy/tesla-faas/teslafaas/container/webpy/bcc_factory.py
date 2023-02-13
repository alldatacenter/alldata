# coding: utf-8

# pylint: disable=import-error,undefined-variable
"""
兼容老的 bigdata_cloudconsole 中的factory获取middleware的方式
"""

import os
import sys
import web
import logging

import threading
# import tablestore
# import oss2

logger = logging.getLogger(__name__)


class BCCFactoryException(Exception):
    pass


class BCCFactoryBase(object):
    """
        base factory of product/$xx/factory/x
    """

    def __init__(self, *args, **kwargs):
        pass

    @classmethod
    def register(cls):
        return None


class BCCFactory(object):
    """
    兼容老的bcc中的factory使用方式

    BCCFactory is the easy way to get instances of data wrappers
    """

    def __init__(self, config):
        self.config = config
        self._clients = {}
        # self.load_product_factory()

    def get_config_wrapper(self, params=None):
        """
            config wrapper
        """
        return self.config

    def get_db_session_wrapper(self):
        raise NotImplementedError()

    def get_oss_wrapper(self):
        raise NotImplementedError()

    def get_redis_conn_wrapper(self, params=None):
        try:
            return self._clients['redis_conn']
        except KeyError:
            try:
                import redis
            except:
                return None
            else:
                pool = web.ctx.tesla.redis_pool
                redis_conn = redis.Redis(connection_pool=pool, socket_timeout=3)
            self._clients['redis_conn'] = redis_conn
            return redis_conn



    def get_zk_wrapper(self):
        try:
            return self._clients['zk']
        except KeyError:
            try:
                from kazoo.client import KazooClient
            except ImportError:
                return None
            zk_conf = self.config['zk']
            zk = KazooClient(hosts=zk_conf['endpoint'])
            zk.start()
            self._clients['zk'] = zk
            logger.info("ZooKeeper client has initialized")
            return zk

    def get_tablestore_wrapper(self):
        # try:
        #     return self._clients['tablestore']
        # except KeyError:
        #     otsconfig = self.config.ots
        #     client = tablestore.OTSClient(otsconfig['endpoint'], otsconfig['accessid'], otsconfig['accesskey'],
        #                                   otsconfig['instancename'])
        #     # 设置OTS打印日志级别
        #     client.logger.setLevel(logging.INFO)
        #     self._clients['tablestore'] = client
        #     return self._clients['tablestore']
        raise NotImplementedError()

