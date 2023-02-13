#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import datetime
import time
import logging


class MetricDataHandler(object):

    logger = logging.getLogger(__name__)

    @staticmethod
    def check_data(df_data, train_start_ts, train_end_ts, default_train_hours, min_train_hours):
        df_data = df_data[(df_data['ts'] >= train_start_ts) & (df_data['ts'] <= train_end_ts)]
        train_data_len = len(df_data)
        real_train_etime = df_data['ts'].max()
        if train_data_len == 0:
            raise Exception("指标无训练数据, 训练时段：%s ～ %s" % (train_start_ts, real_train_etime))

        strict_train_hours = min_train_hours if min_train_hours != -1 else default_train_hours   # 最低训练小时数

        train_ts = df_data['ts'].to_list()
        train_ts.sort()
        MetricDataHandler.logger.info("原始数据序列开始时间戳%s" % train_ts[0])
        MetricDataHandler.logger.info("原始数据序列开始时间戳%s" % train_ts[-1])
        delta_train_hours = (train_ts[-1] - train_ts[0])/1000/60/60   # 训练数据小时跨度

        MetricDataHandler.logger.info(delta_train_hours)

        if delta_train_hours < strict_train_hours:
            raise Exception(f"训练时间不足{strict_train_hours}小时，不满足最低训练时长")

    @staticmethod
    def generate_train_time(default_train_hours, min_train_hours):
        d_now = datetime.datetime.now()
        d_train_end = d_now - datetime.timedelta(minutes=1)

        default_train_hours = int(default_train_hours)
        min_train_hours = int(min_train_hours)
        delta_train_hours = min_train_hours if min_train_hours > default_train_hours else default_train_hours
        d_train_start = d_train_end - datetime.timedelta(hours=delta_train_hours)

        # train_etime = d_train_end.strftime("%Y-%m-%d %H:%M:%S")
        # train_stime = d_train_start.strftime("%Y-%m-%d %H:%M:%S")
        train_start_ts = int(time.mktime(d_train_start.timetuple())) * 1000
        train_end_ts = int(time.mktime(d_train_end.timetuple())) * 1000

        return {
            'train_start_ts': train_start_ts,
            'train_end_ts': train_end_ts
        }

