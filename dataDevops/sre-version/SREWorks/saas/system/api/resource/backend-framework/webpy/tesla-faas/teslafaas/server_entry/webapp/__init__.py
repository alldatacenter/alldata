#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'

import web
from .proxy_handler import ProxyHandler


web.config.debug = False

urls = ('/.*', ProxyHandler)

proxy_app = web.application(urls, globals(), autoreload=False)
