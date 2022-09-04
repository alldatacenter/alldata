# coding: utf-8
"""
Config file parser
"""

import ConfigParser
import math
import os
import sys
import web
from binascii import b2a_hex, a2b_hex

DEFAULT_POOL_SIZE = 10


def conf_to_dict(filename):
    config = ConfigParser.ConfigParser()
    config.read(filename)
    return config._sections


def parser(filename):
    config = ConfigParser.ConfigParser()
    config.optionxform = str
    config.read(filename)
    return config


def get_ini_conf(cfg):
    """
    获取实际配置文件
    :param cfg: 配置文件原始文本
    :return dict: 配置文件字典
    """
    config = parser(cfg)
    result = {}
    for section in config.sections():
        result[section] = {}
        for option in config.options(section):
            result[section][option] = config.get(section, option)
    return result


def get_tesla(cfg, worker_processes=5):
    tesla = {}
    specialitems = ['database', 'http']
    conf = parser(cfg)
    # init database
    opts = dict(conf.items("database"))
    tesla["db_opts"] = opts

    pool_size = int(opts['pool_size']) if hasattr(opts, 'pool_size') \
        else DEFAULT_POOL_SIZE
    ms = mc = math.ceil(float(pool_size) / worker_processes)
    # DB connection is thread safe, can not be used in multi-process env!
    web_db_kwargs = dict(
        dbn="mysql",
        host=opts["host"],
        port=int(opts["port"]),
        db=opts["db"],
        user=opts["user"],
        pw=opts["passwd"],
        charset="utf8",
        use_unicode=True,
        maxshared=0,  # Do not share connection between threads
        # maxshared = ms,
        maxconnections=mc,
        blocking=True
    )
    # 判断是否引入pymysql, 并判断MySQLdb是否被覆盖, TDDL超时时间12s
    if 'pymysql' in sys.modules.keys():
        if 'MySQLdb' in sys.modules.keys() and sys.modules["MySQLdb"] is sys.modules["pymysql"]:
            # web_db_kwargs['read_timeout'] = 11.5  # 引入pymysql, 且也引入了mysqldb, 并调用了install_as_MySQLdb
            pass
        elif 'MySQLdb' not in sys.modules.keys():
            # web_db_kwargs['read_timeout'] = 11.5  # 只引入了pymysql, 没有引入mysqldb
            pass
        else:
            pass  # 引入pymysql, 且也引入了mysqldb, 但没有调用了install_as_MySQLdb
    else:
        pass
    tesla["db"] = web.database(**web_db_kwargs)

    # init http server config
    opts = dict(conf.items("http"))
    tesla['http_opts'] = opts
    tesla["http_opts"].update({
        "port": int(opts["port"]),
        "debug": int(opts["debug"]) == 1,
        "cors_wl": opts['cors_wl'].split(','),
    })

    items = conf.sections()
    for item in items:
        if item not in specialitems:
            tesla[item] = dict(conf.items(item))

    return tesla

