#!/usr/bin/env python
# encoding: utf-8
"""
    可以在handler中使用app_route来动态注册路由，也可以使用老模式在urls.py里面显式声明
"""

urls = (
    r'/demo/(.+)', "DemoHandler"
)
