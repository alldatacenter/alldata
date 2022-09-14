# -*- coding: utf-8 -*-

"""
 @author: fangzong.lyj@alibaba-inc.com
 @date: 2020/08/18 20:29
"""

import json
import datetime
import pandas as pd

from ai_lib.time_series_prediction.algorithm import AlgorithmFactory


class TrainWrapper(object):
    """
    Time series prediction
    时间序列预测入口类
    """

    def __init__(self):
        pass

    def train(self, data, train_id, metric_id, algorithm_name, algorithm_params_text, preprocess_params_text,
              user_config_text, train_stime, train_etime, period, interval, min_train_hours, default_train_hours,
              forecast_horizon, column_name='kpi'):
        """
        :param data: 指标数据[{}, {}] json格式文本
        :param train_id: 训练id
        :param metric_id: 指标实例id
        :param algorithm_name: 时间序列预测算法名称
        :param algorithm_params_text: 模型参数  json格式文本
        :param preprocess_params_text: 预处理参数 json格式文本
        :param user_config_text: 用户配置 json格式文本
        :param train_stime: 训练开始时间
        :param train_etime: 训练结束时间
        :param period: 单周期点个数
        :param interval: 指标数据时间间隔
        :param min_train_hours: 最小训练时长
        :param default_train_hours: 默认训练时长
        :param forecast_horizon: 预测时长秒
        :param column_name: 预测列名 默认值取kpi
        :return:
        """

        preprocess_params = json.loads(preprocess_params_text)
        column_name = preprocess_params.get("colname", column_name)
        process_method = preprocess_params.get("process_method", {})
        algorithm_params = json.loads(algorithm_params_text)
        user_config = json.loads(user_config_text)

        df_train_data = pd.DataFrame(json.loads(data))
        df_train_data = df_train_data[(df_train_data['ts'] >= train_stime) & (df_train_data['ts'] <= train_etime)]
        train_data_len = len(df_train_data)
        real_train_etime = df_train_data['ts'].max()
        if train_data_len == 0:
            raise Exception("指标无训练数据, 训练时段：%s ～ %s" % (train_stime, real_train_etime))

        strict_train_hours = min_train_hours if min_train_hours != -1 else default_train_hours   # 最低训练小时数

        train_hours = df_train_data['ts'].apply(lambda x: str(x).split(':')[0]).unique()
        train_hours.sort()
        min_datetime = datetime.datetime.strptime(train_hours[0], '%Y-%m-%d %H')
        max_datetime = datetime.datetime.strptime(train_hours[-1], '%Y-%m-%d %H')
        delta_train_hours = int(((max_datetime - min_datetime).total_seconds())/60/60)   # 训练数据小时跨度

        actual_train_hours = len(train_hours)   # 实际数据小时数

        if delta_train_hours < strict_train_hours:
            raise Exception("训练时间不足%s小时，不满足最低训练时长" % strict_train_hours)
        else:
            if actual_train_hours < strict_train_hours:
                raise Exception("训练时间不足%s天，不满足最低训练时长，采集数据有丢失引起" % strict_train_hours)

        train_dataset = AlgorithmFactory.data_preprocess(algorithm_name, df_train_data.copy(), column_name, interval,
                                                         period, process_method, user_config)

        tsp_ins = AlgorithmFactory.get_algorithm(algorithm_name, period, interval, column_name, algorithm_params,
                                                 user_config, forecast_horizon, metric_id, train_id)
        tsp_ins.fit(train_dataset)
        tsp_result, new_algorithm_params, damo_input, damo_output = tsp_ins.predict()
        tsp_result = tsp_result if tsp_result else {}
        new_algorithm_params = new_algorithm_params if new_algorithm_params else {}

        return tsp_result, new_algorithm_params, train_data_len, real_train_etime
