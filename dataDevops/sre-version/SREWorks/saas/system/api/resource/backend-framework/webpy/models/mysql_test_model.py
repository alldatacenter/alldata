#!/usr/bin/env python
# encoding: utf-8
""" """
from teslafaas.container.webpy.common.BaseModel import BaseModel
# import  pandas as pd


class MysqlTestModel(BaseModel):

    def get_db_time(self):
        db = self.db
        # Or get db from web.ctx.tesla without BaseHandler/BaseModel as parent
        # tesla_ctx = web.ctx.tesla
        # tesla_ctx = self.tesla_conf
        # db = tesla_ctx.db
        data = {
            # 使用默认数据数据源
            'db': list(db.query("SELECT NOW() AS now;"))[0],
            # 使用数据源 test1
            'db_test1': list(self.dbs.test1.query("SELECT NOW() AS now;"))[0],
            # 使用数据源 test2
            'db_test2': list(self.dbs.test2.query("SELECT NOW() AS now;"))[0]
        }
        return data

        # CRUD 相关的用法参考 http://webpy.org/cookbook/select

    def pandans(self):
        pass
        # pandas usage
        #pd.read_sql(sql, con=self.dbs.test1.ctx.db)
        #pd.read_sql(sql, con=self.db.ctx.db)
