#!/usr/bin/env python
# encoding: utf-8
""" local dev file for bin/tesla-faas """
__author__ = 'adonis'

import sys
import os


version_info = (1, 0, 0)
__version__ = ".".join([str(v) for v in version_info])
SERVER_SOFTWARE = "tesla-faas-%s" % __version__


here = os.path.abspath(os.path.dirname(__file__))
tg_path = os.path.join(here, "tesla-gunicorn/")
if tg_path not in sys.path:
    sys.path.insert(0, tg_path)

import gunicorn
from container import webpy
from common import env_init


__all__ = ['env_init', 'webpy', 'common', 'container', 'gunicorn', 'server_entry', 'middleware']
