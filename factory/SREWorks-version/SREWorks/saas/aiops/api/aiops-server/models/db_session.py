#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import pymysql
from flask_sqlalchemy import SQLAlchemy
from common.config import get_config
from app.aiops_server_app import get_flask_app, DEFAULT_APP_NAME

app_name = DEFAULT_APP_NAME
app = get_flask_app(app_name)

config = get_config()
aiops_config = config.get('mysql').get('aiops')
pmdb_config = config.get('mysql').get('pmdb')


def _build_mysql_url(host, port, dbname, username, password):
    return 'mysql+mysqldb://' + str(username) + ':' + str(password) + '@' + host + ':' + str(port) + '/' + str(dbname)


SQLALCHEMY_DATABASE_URI = _build_mysql_url(aiops_config.get('host'), aiops_config.get('port'), aiops_config.get('db'),
                                           aiops_config.get('username'), aiops_config.get('password'))
SQLALCHEMY_BINDS = {
    'pmdb': _build_mysql_url(pmdb_config.get('host'), pmdb_config.get('port'), pmdb_config.get('db'),
                             pmdb_config.get('username'), pmdb_config.get('password'))
}

app.config["SQLALCHEMY_DATABASE_URI"] = SQLALCHEMY_DATABASE_URI
app.config["SQLALCHEMY_BINDS"] = SQLALCHEMY_BINDS

pymysql.install_as_MySQLdb()   # MySQLdb不兼容py3.5以后的版本, 使用pymysql代替MySQLdb
db = SQLAlchemy(app)

db.create_all()
