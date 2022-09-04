#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db


class BaseService(object):
    def __init__(self):
        self.db = db
        self.db_session = db.session
