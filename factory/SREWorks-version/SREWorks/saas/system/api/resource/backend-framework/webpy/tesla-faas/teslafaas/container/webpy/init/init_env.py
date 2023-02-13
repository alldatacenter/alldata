#!/usr/bin/env python
# encoding: utf-8
"""
DB-utils:
https://cito.github.io/w4py-olde-docs/Webware/DBUtils/Docs/UsersGuide.html
"""
import web

__author__ = 'adonis'

DEFAULT_POOL_SIZE = 50


def init_python_env():
    import sys
    reload(sys)
    sys.setdefaultencoding('utf-8')
    # print '------ set sys.defaultencoding to utf-8'


def init_coroutine_env():
    try:
        import gevent
        HAS_GEVENT = True
    except:
        HAS_GEVENT = False

    if HAS_GEVENT:
        # Add monkey patch here not gunicorn startup time,
        # because of subprocess=True patched subprocess
        from gevent import monkey
        monkey.patch_all(subprocess=False)
        # print '------ Coroutine ENV inited: use gevent'
    else:
        # print '------ Coroutine ENV inited: no gevent'
        pass


def init_db_driver():
    import patch_webpy
    patch_webpy.patch_all()

    import pymysql
    pymysql.install_as_MySQLdb()
    # print '------ DB driver inited'


def init_web_ctx():
    pass


def init_all(use_gevent=True):
    # print '-------- Initing server -------------'
    if use_gevent:
        init_coroutine_env()
    else:
        pass
        # print '------ no coroutine(GEVENT) used'
    init_python_env()
    init_db_driver()
    web.config.debug = False
    # print '-------- Server inited -------------'
