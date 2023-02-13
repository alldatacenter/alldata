#!/usr/bin/env python
# encoding: utf-8
""" """

import json
from models.redis_conn import RedisConnFactory


class TSPResultModel(object):
    def __init__(self):
        self.redis_conn_factory = RedisConnFactory()
        # 48小时过期
        self.ex = 48*3600

    def get_result_by_id(self, metric_id, config_id, instance_id, start_ts, end_ts):
        key = str(metric_id) + "_" + str(config_id) + "_" + str(instance_id)
        r_conn = self.redis_conn_factory.get_tsp_redis_conn()
        result_list = r_conn.zrangebyscore(key, start_ts, end_ts)
        points = []
        for result in result_list:
            item = json.loads(result)
            points.append(item)
        return points

    def save_result(self, metric_id, config_id, instance_id, datas):
        redis_data_dict = {}
        for data in datas:
            str_tsp_result = json.dumps(data)
            redis_data_dict[str_tsp_result] = data.get('ts')

        key = str(metric_id) + "_" + str(config_id) + "_" + str(instance_id)
        r_conn = self.redis_conn_factory.get_tsp_redis_conn()
        r_conn.zadd(key, redis_data_dict)
        r_conn.expire(key, self.ex)
