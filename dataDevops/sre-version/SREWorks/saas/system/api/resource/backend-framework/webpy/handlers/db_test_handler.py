#!/usr/bin/env python
# encoding: utf-8
""" """
from teslafaas.container.webpy.common.BaseHandler import BaseHandler
from teslafaas.container.webpy.common.decorators import fcached,ActionError,app_route
import time
import web
import threading
import logging

from models.mysql_test_model import MysqlTestModel

@app_route(r'/pg/(.*)')
class DBTestBaseHandler(BaseHandler):

    def query(self, params):
        db = self.dbs.get('pg')
        self.logger.info("xxxxxxxx")
        sql = 'select * from boy'
        return db.query(sql).list()

    def insert(self, params):
        db = self.dbs.get('pg')
        sql = "insert into boy (id, name, information) VALUES (5, 'xx', '{\"xx\": \"haha\"}')"
        return db.query(sql)

    def insert2(self, params):
        db = self.dbs.get('pg')
        return db.insert("boy", id=6, name="haha", information="{\"xx\": \"haha\"}")

    def update(self, param):
        db = self.dbs.get('pg')
        sql = "update boy set name = 'test' where id = 1"
        return db.query(sql)


@app_route(r'/mysql/(.*)')
class MysqlDBTestHandler(BaseHandler):

    def query(self, params):
        self.logger.info("start *** %s", web.ctx.fullpath)
        time_1 = time.time()
        db = self.dbs.get('mysql')
        sql = 'select * from user'
        data = db.query(sql).list()
        time_2 = time.time()
        self.logger.info("end queryCost=%s", int((time_2 - time_1) * 1000))
        return data
