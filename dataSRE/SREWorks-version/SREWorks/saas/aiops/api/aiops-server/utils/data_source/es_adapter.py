#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from datetime import datetime
import pandas as pd
from utils.data_source.data_source_connector import DataSourceConnector
from utils.metric_data.metric_data_handler import MetricDataHandler


class EsAdapter(object):
    def __init__(self, datasource_id, datasource_table):
        self.identifier = 'es'
        self.connector_factory = DataSourceConnector()
        self.connector = self.connector_factory.get_conn(self.identifier, datasource_id)
        self.index = datasource_table

        self.logger = logging.getLogger(__name__)

    def get_metric_data(self, default_train_hours, min_train_hours, series_interval, index_path, index_tags):
        train_time = MetricDataHandler.generate_train_time(default_train_hours, min_train_hours)
        train_start_ts = train_time['train_start_ts']
        train_end_ts = train_time['train_end_ts']

        query = '*'
        if index_tags:
            query_items = []
            kvtags = index_tags.split(",")
            for kvtag in kvtags:
                kvpair = kvtag.split("=")
                if len(kvpair) == 2:
                    query_items.append(kvpair[0] + ':' + kvpair[1])
            query = ' AND '.join(query_items)

        body = {
            "size": 0,
            "query": {
                "bool": {
                    "filter": [
                        {"range": {"@timestamp": {"gte": train_start_ts, "lte": train_end_ts, "format": "epoch_millis"}}},
                        {"query_string": {"analyze_wildcard": True, "query": query}}
                    ]
                }
            },
            "aggs": {
                "series": {
                    "date_histogram": {
                        "interval": str(series_interval) + "s",
                        "field": "@timestamp",
                        "min_doc_count": 0,
                        "extended_bounds": {"min": train_start_ts, "max": train_end_ts},
                        "format": "epoch_millis"
                    },
                    "aggs": {
                        "data": {
                            "avg": {
                                "field": index_path
                            }
                        }
                    }
                }
            }
        }
        self.logger.info(body)

        res = self.connector.search(index=self.index, body=body)
        data_series = res.get('aggregations').get('series').get('buckets')
        self.logger.info("原始数据序列大小:%s" % len(data_series))

        return self._format_metric_data(data_series, train_start_ts, train_end_ts, default_train_hours, min_train_hours)

    def _format_metric_data(self, es_data_series, train_start_ts, train_end_ts, default_train_hours, min_train_hours):
        df_es_data_series = pd.DataFrame(es_data_series)
        df_es_data_series.rename(columns={'key': 'ts'}, inplace=True)
        df_valid_data = df_es_data_series[df_es_data_series['data'].apply(lambda x: x['value'] is not None)]
        self.logger.info("合法数据序列大小:%s" % len(df_valid_data))
        MetricDataHandler.check_data(df_valid_data, train_start_ts, train_end_ts, default_train_hours, min_train_hours)

        def func(item):
            value = item['data']['value']
            if value is not None:
                return pd.Series({
                    'ts': datetime.fromtimestamp(int(item['ts']/1000)).strftime("%Y-%m-%d %H:%M:%S"),
                    'kpi': value
                })
        return pd.DataFrame(df_valid_data.apply(func, axis=1))
