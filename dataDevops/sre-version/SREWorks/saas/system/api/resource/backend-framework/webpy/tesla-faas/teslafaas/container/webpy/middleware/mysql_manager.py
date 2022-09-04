#!/usr/bin/env python
# encoding: utf-8
""" """
import web
import sys

__author__ = 'adonis'


def create_db_pool(host, port, user, passwd, db, pool_size=20, dbn='mysql', params=None):
    import web
    # Create DB pool
    ms = mc = pool_size
    # DB connection is thread safe, can not be used in multi-process env!

    if dbn not in ['mysql', 'postgres']:
        raise Exception("not support current db type, dbtype=%s".format(dbn))

    web_databases_kwargs = dict(
        dbn=dbn,
        host=host,
        port=int(port),
        db=db,
        user=user,
        pw=passwd,
        # charset="utf8",
        # use_unicode=True,
        maxshared=0,    # Do not share connection between threads
        # maxshared = ms,
        maxconnections=mc,
        blocking=True
    )

    if dbn == 'mysql':
        web_databases_kwargs['charset'] = 'utf8'
        web_databases_kwargs['use_unicode'] = True

    # 判断是否引入pymysql,并判断MySQLdb是否被覆盖, TDDL超时时间12s
    if dbn == 'mysql':
        if 'pymysql' in sys.modules.keys():
            if 'MySQLdb' in sys.modules.keys() and sys.modules["MySQLdb"] is sys.modules["pymysql"]:
                web_databases_kwargs['read_timeout'] = 55  # 引入pymysql, 且也引入了mysqldb, 并调用了install_as_MySQLdb
            elif 'MySQLdb' not in sys.modules.keys():
                web_databases_kwargs['read_timeout'] = 55  # 只引入了pymysql, 没有引入mysqldb
            else:
                pass  # 引入pymysql, 且也引入了mysqldb, 但没有调用了install_as_MySQLdb
        else:
            pass
    if params is not None:
        for key in params.keys():
            web_databases_kwargs[key] = params.get(key)
    db_pool = web.database(**web_databases_kwargs)
    return db_pool
