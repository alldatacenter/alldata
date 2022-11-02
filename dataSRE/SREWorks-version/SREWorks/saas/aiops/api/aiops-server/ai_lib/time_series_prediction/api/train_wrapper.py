# -*- coding: utf-8 -*-

"""
 @author: fangzong.lyj@alibaba-inc.com
 @date: 2020/08/18 20:29
"""


from ai_lib.time_series_prediction.algorithm.factory import AlgorithmFactory


class TrainWrapper(object):
    """
    Time series prediction
    时间序列预测接口类
    """

    def __init__(self):
        pass

    @staticmethod
    def train(df_train_data, train_id, metric_id, algorithm_name, algorithm_params, preprocess_params,
              interval, forecast_horizon, user_config=None, column_name='kpi'):
        """
        :param df_train_data: 指标数据[{}, {}] df json格式
        :param train_id: 训练id
        :param metric_id: 指标实例id
        :param algorithm_name: 时间序列预测算法名称
        :param algorithm_params: 模型参数  json格式
        :param preprocess_params: 预处理参数 json格式
        :param interval: 指标数据时间间隔
        :param forecast_horizon: 预测时长秒
        :param user_config: 用户配置 json格式
        :param column_name: 预测列名 默认值取kpi
        :return:
        """

        preprocess_params = preprocess_params if preprocess_params else {}
        user_config = user_config if user_config else {}
        if 'advanced_config_map' not in user_config:
            user_config['advanced_config_map'] = {}
        column_name = preprocess_params.get("colname", column_name)
        process_method = preprocess_params.get("process_method", {})

        # period: 单周期点个数
        period = int(86400 / interval)

        train_dataset = AlgorithmFactory.data_preprocess(algorithm_name, df_train_data.copy(), column_name, interval,
                                                         period, process_method, user_config)

        tsp_ins = AlgorithmFactory.get_algorithm(algorithm_name, period, interval, column_name, algorithm_params,
                                                 user_config, forecast_horizon, metric_id, train_id)
        tsp_ins.fit(train_dataset)
        tsp_result, new_algorithm_params, damo_input, damo_output = tsp_ins.predict()
        tsp_result = tsp_result if tsp_result else {}
        new_algorithm_params = new_algorithm_params if new_algorithm_params else {}

        return tsp_result, new_algorithm_params, len(df_train_data)
