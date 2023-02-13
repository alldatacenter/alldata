#!/usr/bin/env python
# encoding: utf-8
""" """
import types
import web
from web import Storage


from teslafaas.common.trace_id import get_upstream_trace
from teslafaas.container.webpy.bcc_factory import BCCFactory, BCCFactoryBase
from teslafaas.container.webpy.common.hooks import tesla_loadhook
from teslafaas.container.webpy.middleware.mysql_manager import create_db_pool
from teslafaas.container.webpy.middleware.redis_manager import create_redis_pool
from teslafaas.container.webpy.middleware.domain_model import DomainModel
import logging

__author__ = 'adonis'


def context_hook(context_manager):
    tesla_dict = {
        'config': context_manager.config,
        'db': context_manager.db,
        'dbs': context_manager.dbs,
        'dm': DomainModel,
        'logger': context_manager.logger,
        'tesla_sdk_client': context_manager.tesla_sdk,
        'tesla_sdk': context_manager.tesla_sdk,
        "tesla_sdk_by_gateway": context_manager.tesla_sdk_by_gateway,
        'tesla_sdk_channel': context_manager.tesla_sdk_channel,
        'redis_pool': context_manager.redis_pool,
        'factory': context_manager.factory,

        # 兼容老的代码
        'clusterinfo_source': '',
        'http_opts': {
            'cors_wl': '*'
        },
        'db_opts': {},
        'request_id': web.ctx.get('trace_id', None),
        'trace_id': web.ctx.get('trace_id', None),
    }
    web.ctx.tesla = Storage(tesla_dict)
    # 兼容老的bcc中的使用方式
    web.ctx.factory = context_manager.factory
    # 兼容老的代码
    web.ctx.server = ''
    web.ctx.db = web.ctx.tesla.db


class ContextManager(object):
    """
    # FIXME: dynamic resource and static resource

    dynamic resource: db, redis, sdk, logger etc. need to be loaded
    refresh every worker process

    static resource: conf, hooks, may be shared across workers
    """
    def __init__(self, app, config, raw_mod=None):
        self.app = app
        self.raw_mod = raw_mod
        self.config = config
        self.db = None
        self.redis_pool = None
        self.logger = None
        self.tesla_sdk = None
        self.tesla_sdk_channel = None
        self.tesla_sdk_by_gateway = None
        self.factory = None

    def load_middle_ware(self):
        self.db = self.load_db()
        self.dbs = self.load_dbs()
        self.logger = self.load_logger()
        self.redis_pool = self.load_redis_pool()
        self.factory = self.load_factory()

    def load_logger(self):
        # TODO: container log isolation
        pass

    def generate_trace_id(self):
        # TODO: get from http header
        pass

    def load_factory(self):
        factory = BCCFactory(self.config)
        self._load_sub_factory(factory)
        return factory

    def _load_sub_factory(self, factory):
        if not self.raw_mod:
            return
        sub_factory_mod = getattr(self.raw_mod, 'factory', None)
        if not sub_factory_mod:
            return
        for file_mod in dir(sub_factory_mod):
            if file_mod.startswith("__"):
                continue
            file_mod = getattr(sub_factory_mod, file_mod)
            if not isinstance(file_mod, types.ModuleType):
                continue
            for class_obj in dir(file_mod):
                if class_obj.startswith("__"):
                    continue
                class_obj = getattr(file_mod, class_obj)
                if not isinstance(class_obj, (types.ClassType, types.TypeType)):
                    continue
                if not issubclass(class_obj, BCCFactoryBase) or class_obj is BCCFactoryBase:
                    continue
                if hasattr(class_obj, 'register'):
                    factory_instance = class_obj()
                    setattr(factory, factory_instance.register(), factory_instance)

    def load_db(self, db_conf=None):
        if not db_conf:
            db_conf = self.config.get('database', None)
        if not db_conf:
            return None
        db_name = db_conf.get('db', '')
        db_name = db_conf.get('dbname', '') if not db_name else db_name
        db_name = db_conf.get('database', '') if not db_name else db_name
        if not db_name:
            raise Exception("no db name specified in config file")
        db_type = db_conf.get('db_type', 'mysql')
        pool = create_db_pool(
            db_conf['host'],
            db_conf['port'],
            db_conf['user'],
            db_conf['passwd'],
            db_name,
            db_conf.get('pool_size', 20),
            dbn=db_type,
            params=db_conf.get('params')
        )
        return pool

    def load_dbs(self):
        dbs = Storage({})
        db_conf = self.config.get('databases', None)
        if db_conf is None:
            db_conf = self.config.get('rds', None)
        if db_conf is None:
            db_conf = self.config.get('dbs', None)
        if not db_conf:
            return dbs
        if not isinstance(db_conf, dict):
            raise Exception("invalid multiple database config: "
                            "not dict format")
        for k, v in db_conf.iteritems():
            if not isinstance(v, dict):
                raise Exception("invalid multiple database config: "
                                "data source config '%s' not dict format" % k)
            dbs[k] = self.load_db(db_conf=v)
        return dbs

    def load_redis_pool(self):
        redis_conf = self.config.get('redis', None)
        if not redis_conf:
            return None
        pool = create_redis_pool(
            redis_conf['host'],
            redis_conf['port'],
            redis_conf['passwd'],
            50,
            redis_conf['db'],
            mode=redis_conf['mode']
        )
        return pool


    def trace_id_hook(self):
        trace_id = get_upstream_trace()
        if trace_id:
            web.ctx.trace_id = trace_id
        else:
            wsgi_env = web.ctx['environ']
            try:
                remote = wsgi_env['HTTP_X_FORWARDED_FOR'].split(',')[-1].strip()
            except KeyError:
                remote = wsgi_env['REMOTE_ADDR']
            # trace_id = generate_trace_id(remote)
                trace_id = 'xxxx'
        web.ctx.trace_id = trace_id

    def set_context_hook(self):
        self.app.add_processor(web.loadhook(self.trace_id_hook))
        self.app.add_processor(tesla_loadhook(context_hook, self))
