#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from flask import Flask
from common import env_init
import threading

DEFAULT_APP_NAME = 'sreworks_aiops_server'
__FLASK_APP = dict()

lock = threading.Lock()


def get_flask_app(app_name=DEFAULT_APP_NAME):
    lock.acquire()
    app = __FLASK_APP.get(app_name, None)
    if app is None:
        app = __AiOpsServerApp(app_name)
        __FLASK_APP[app_name] = app
    lock.release()

    return app


class __AiOpsServerApp(Flask):
    def __init__(self, app_name):
        super().__init__(
            import_name=app_name
        )
        self.app_name = app_name
        env_init.init_logger(self.app_name)

