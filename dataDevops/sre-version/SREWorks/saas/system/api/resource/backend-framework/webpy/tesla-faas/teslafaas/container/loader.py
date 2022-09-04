#!/usr/bin/env python
# encoding: utf-8
""" """
from web import Storage

from teslafaas.container.webpy.context_manager import ContextManager
from teslafaas.container.webpy.http_error_process import customize_http_error

import os
import sys
import web
import json
import pkgutil
import logging
import importlib
from codecs import open

from teslafaas.container.webpy.init import init_all
from teslafaas.container.webpy.common import urls as global_urls

__author__ = 'adonis'


init_all(use_gevent=False)


class InvalidUserApp(Exception):
    pass


class DummyModule(dict):
    def __init__(self, **kw):
        super(DummyModule, self).__init__(**kw)

    def __getattr__(self, key):
        # FIXME: vars() support, vars() will not come here
        # if key == '__dict__':
        #     return {k: v for k, v in self.iteritems()}
        try:
            return self[key]
        except KeyError:
            raise AttributeError(
                r"'DummyModule' object has no attribute '%s'" % key)

    def __setattr__(self, key, value):
        self[key] = value
        # FIXME: vars() support
        self.__dict__[key] = value


def webpy_wsgifunc_wrapper(self):
    conf = self.conf
    webpy_app = self.webpy_app
    # TODO: load dynamic resource
    manager = ContextManager(webpy_app, conf, raw_mod=self.raw_mod)
    manager.load_middle_ware()
    manager.set_context_hook()
    return webpy_app.wsgifunc()


class ContainerLoader(object):

    def __init__(self, init_context):
        self.conf = {}
        self.init_context = init_context

    def wrap_webpy_raw_module(self, mod, conf, urls):
        wsgi_mod = DummyModule()
        app = web.application(urls, globals(), autoreload=False)
        customize_http_error(app)
        # Unable to set a FunctionType to instance and void conflict
        # app.wsgi = FunctionType app.wsgifunc()
        # TODO: wrap wsgifunc to do dynamic resource reload, refresh automatically
        wsgi_mod.conf = conf
        wsgi_mod.raw_mod = mod
        wsgi_mod.webpy_app = app
        manager = ContextManager(app, conf, raw_mod=mod)
        manager.load_middle_ware()
        manager.set_context_hook()
        wsgi_mod.wsgifunc = app.wsgifunc()
        # wsgi_mod.refresh =
        return wsgi_mod

    def _import_sub_modules(self, mod):
        for importer, modname, ispkg in pkgutil.walk_packages(path=mod.__path__, prefix=mod.__name__+'.'):
            __import__(modname)

    def load_module(self, src_path, container_name=None, env="common"):
        """
        :param src_path: user module dir path
        :param container_name: used for url router and unix socket name,
            default: last dir name of src_path
        :return: 2-item tuple, wsgi module name and container name
            Gunicorn used python app module name, with wsgifunc()
            entry function
        """
        base_dir = os.path.abspath(src_path.rstrip('/'))
        module_name = dir_name = os.path.basename(base_dir)
        src_parent_path = os.path.dirname(base_dir)
        if not container_name:
            container_name = dir_name
        if src_parent_path not in sys.path:
            sys.path.append(src_parent_path)
        # TODO: add src to python path
        if src_parent_path not in sys.path:
            sys.path.append(src_parent_path)
        # if module_name in sys.modules:
        #     raise InvalidUserApp(
        #         "Invalid module name '%s': conflict with a already exist "
        #         "module(%s), please change a name"
        #         % (module_name, sys.modules[module_name].__file__))
        try:
            raw_module = __import__(module_name)
        except ImportError:
            raise InvalidUserApp(
                "Can't import '%s' after add '%s' to PYTHONPATH. "
                "Please check if __init__.py exist in '%s'"
                % (dir_name, src_parent_path, src_path))
        # conf_suffix = ""
        # if env != "common":
        #     if env == "DAILY":
        #         conf_suffix = "_daily"
        #     elif env == "PRE":
        #         conf_suffix = "_pre"
        #     elif env == "PRODUCTION":
        #         conf_suffix = "_prod"
        #     if os.path.exists(os.path.join(x, 'conf%s.ini' % conf_suffix)):
        #         config.update(self.parse_ini_conf(os.path.join(x, 'conf%s.ini' % conf_suffix)))
        #         shutil.copyfile(os.path.join(x, 'conf%s.ini' % conf_suffix), os.path.join(x, 'conf.ini'))
        #     if os.path.exists(os.path.join(x, 'conf%s.json' % conf_suffix)):
        #         config.update(self.parse_json_conf(os.path.join(x, 'conf%s.json' % conf_suffix)))
        #         shutil.copyfile(os.path.join(x, 'conf%s.json' % conf_suffix), os.path.join(x, 'conf.json'))
        #     if os.path.exists(os.path.join(x, 'conf%s.py' % conf_suffix)):
        #         config.update(self.parse_py_conf("%s.%s" % (mod_name, 'conf%s' % conf_suffix)))
        #         shutil.copyfile(os.path.join(x, 'conf%s.py' % conf_suffix), os.path.join(x, 'conf.py'))

        # # FIXME: conflict for multiple containers
        # if src_path not in sys.path:
        #     sys.path.append(src_path)
        urls = self.load_urls(raw_module, self.init_context.config)
        # 兼容老的 sub factory 用法
        try:
            __import__('%s.factory' % raw_module.__name__)
        except ImportError:
            pass
        else:
            print 'importing module and submodules: %s.factory' % raw_module.__name__
            self._import_sub_modules(getattr(raw_module, 'factory'))
        wsgi_module_name = "%s.%s" % ('teslafaas.container.wsgi_mods', module_name)
        wsgi_module = self.wrap_webpy_raw_module(raw_module, conf=self.init_context.config, urls=urls)
        sys.modules[wsgi_module_name] = wsgi_module
        return wsgi_module, container_name

    def load_urls(self, raw_mod, config):

        handlers = self.load_handlers(raw_mod, config)
        global global_urls 
        if os.path.exists(os.path.join(raw_mod.__path__[0] + '/urls.py')):
            logging.info('importing module: %s.urls', raw_mod.__name__)
            __import__('%s.urls' % raw_mod.__name__)
            if hasattr(raw_mod.urls, 'urls'):
                m_urls = getattr(raw_mod.urls, 'urls')
                if not global_urls is m_urls:
                    global_urls.extend(m_urls)

        if config.get('disable_index', False) is True:
            from teslafaas.container.webpy.common.IndexHandler import IndexHandler 
            global_urls += [r"/", IndexHandler]

        for i in xrange(1, len(global_urls), 2):
            if type(global_urls[i]) == str:
                hname = global_urls[i]
                if hname not in handlers:
                    print 'Error: cannot find handler class for %s' % hname
                global_urls[i] = handlers[hname]

        return global_urls

    def load_handlers(self, raw_mod, config):
        handlers = {}
        path = os.path.join(raw_mod.__path__[0] + '/handlers')
        if not os.path.exists(path):
            return handlers

        from teslafaas.container.webpy.common.BaseHandler import BaseHandler 
        for f in os.listdir(path):
            if f.endswith('.py'):
                name, postfix = os.path.splitext(f)
                m = importlib.import_module('%s.handlers.%s' % (raw_mod.__name__, name))
                for obj in dir(m):
                    classobj = getattr(m, obj)
                    try:
                        if issubclass(classobj, BaseHandler):
                            name = classobj.__name__
                            if name not in handlers:
                                handlers[name] = classobj
                    except:
                        pass
        return handlers

    def reset_container_log(self):
        #
        """
        初始化日志配置
        """
        with open(os.path.join(self.root_path, 'conf/logging.json')) as f:
            try:
                log_config = json.loads(f.read())
            except ValueError:
                raise ValueError('Invalid logging config, cannot loads to JSON')
            logging.dictConfig(log_config)
            self.logger = logging.getLogger()  # set default root logger

