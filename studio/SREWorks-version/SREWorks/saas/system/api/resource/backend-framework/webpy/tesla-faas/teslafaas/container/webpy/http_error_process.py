#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'
import web


def customize_http_error(application):
    application.notfound = not_found
    # application.add_processor(web.loadhook(hook_tesla))


def not_found():
    return web.notfound(
        '{"code": 404, "message": "url path not matched %s", "data": ""}' % web.ctx.path)
