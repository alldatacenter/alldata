#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'


UNIX_SOCKET_PATH = "unix:/tmp/tesla-faas/container-%s.socket"

STD_HEADERS = ['Content-Type', 'Content-Length', 'Content-Encoding',
               'Content-Disposition', 'Origin',
               'Accept', 'Accept-Encoding', 'Accept-Language',
               'Access-Control-Allow-Credentials',
               'Access-Control-Allow-Headers',
               'Access-Control-Allow-Methods',
               'Access-Control-Allow-Origin',
               'Access-Control-Expose-Headers',
               ]
X_HEADERS = ['X-AUTH-USER', 'X-EMPID', 'X-Traceid', 'X-Biz-App', 'X-Locale', 'X-From-EmpId', 'X-From-Auth-User']
ALL_HEADERS = STD_HEADERS + X_HEADERS

NON_WSGI_HEADER = ['Content-Type', 'Content-Length', 'Content-Encoding',
                   'Content-Disposition', ]
