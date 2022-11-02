#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
import threading
from elasticsearch import Elasticsearch
from common.exception.errors import DatasourceError
from services.datasource_service import DatasourceService


# 同步锁
def synchronous_lock(func):
    def wrapper(*args, **kwargs):
        with threading.Lock():
            return func(*args, **kwargs)
    return wrapper


class DataSourceConnector(object):

    es_identifier = 'es'

    instance = None
    @synchronous_lock
    def __new__(cls, *args, **kwargs):
        if cls.instance is None:
            cls.instance = object.__new__(cls)
        return cls.instance

    def __init__(self):
        self.datasource_service = DatasourceService()
        self.db_conn = {}
        self.logger = logging.getLogger(__name__)

    def get_conn(self, data_source_type, data_source_id):
        # TODO 目前仅支持es数据源
        if data_source_type == self.es_identifier:
            if data_source_type not in self.db_conn:
                self.db_conn[data_source_type] = {}

            if data_source_id not in self.db_conn.get(data_source_type):
                es_connector = self._get_es_conn(data_source_id)
                self.db_conn[data_source_type][data_source_id] = es_connector
                return es_connector
            else:
                return self.db_conn[data_source_type][data_source_id]

    def _get_es_conn(self, data_source_id):
        datasource = self.datasource_service.get_datasource_by_id(data_source_id)
        if datasource is None:
            raise DatasourceError("数据源异常")

        self.logger.info(datasource)
        es_clusters = [{
            "host": datasource.get("endpoint"),
            # "host": "elasticsearch-master.ca221ae8860d9421688e59c8ab45c8b21.cn-hangzhou.alicontainer.com",
            "port": datasource.get("port"),
            # "port": 80,
            "index": datasource.get("source_table")
        }]

        access_key = datasource.get("access_key")
        secret_key = datasource.get("secret_key")
        if access_key and secret_key:
            connector = Elasticsearch(es_clusters, http_auth=(access_key, secret_key))
        else:
            connector = Elasticsearch(es_clusters)
        return connector

    def _get_mysql_conn(self, name):
        pass
