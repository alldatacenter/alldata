#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging

from services.base_service import BaseService
from services.metric_service import MetricService
from services.metric_instance_service import MetricInstanceService
from services.metric_tsp_config_service import MetricTSPConfigService
from services.tsp_algorithms_service import TSPAlgorithmsService
from services.tsp_algorithm_params_service import TSPAlgorithmParamsService
from services.tsp_preprocess_params_service import TSPAPreprocessParamsService
from models.tsp_result_model import TSPResultModel
from tsp_train import async_train


class MetricTSPTaskService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)
        self.metric_service = MetricService()
        self.metric_instance_service = MetricInstanceService()
        self.metric_tsp_config_service = MetricTSPConfigService()
        self.algorithm_service = TSPAlgorithmsService()
        self.algorithm_params_service = TSPAlgorithmParamsService()
        self.preprocess_params_service = TSPAPreprocessParamsService()
        self.tsp_result_model = TSPResultModel()

    def train_tsp_task(self, config_id):
        """
        触发指标时序预测任务
        :return: 预测结果
        """
        tsp_config = self.metric_tsp_config_service.get_config_by_id(config_id)
        if not tsp_config:
            raise ValueError("指标预测配置不存在 config_id：%s" % config_id)

        metric_id = tsp_config.get('metric_id')
        metric = self.metric_service.get_metric_by_id(metric_id)
        if not metric:
            raise ValueError("指标不存在 metric_id：%s" % metric_id)

        metric_instances = self.metric_instance_service.get_instance_by_metric(metric_id)
        if not metric_instances:
            raise ValueError("指标实例为空 metric_id：%s，无法创建预测任务" % metric_id)

        tsp_algorithm = self.algorithm_service.get_tsp_algorithm(tsp_config.get('algorithm_id'))
        tsp_algorithm_param = self.algorithm_params_service.get_tsp_algorithm_param(tsp_config.get('algorithm_param_id'))
        if not (tsp_algorithm and tsp_algorithm_param):
            raise ValueError("指标预测任务算法配置异常 config_id：%s" % config_id)
        tsp_preprocess_param = self.preprocess_params_service.get_tsp_preprocess_param(tsp_config.get('preprocess_param_id'))

        tsp_config['algorithm_name'] = tsp_algorithm.get('name')
        tsp_config['algorithm_params'] = tsp_algorithm_param.get('value')
        if tsp_preprocess_param:
            tsp_config['preprocess_param'] = tsp_preprocess_param.get('value')

        self.logger.info(tsp_config)

        async_train.delay(metric, tsp_config, metric_instances)

        data = {
            'size': len(metric_instances),
            'status': 'running'
        }
        return data

    def get_tsp_result(self, config_id, metric_instance_id, start_ts, end_ts):
        """
        获取指标时序预测任务结果
        :return: 预测结果
        """
        tsp_config = self.metric_tsp_config_service.get_config_by_id(config_id)
        if not tsp_config:
            raise ValueError("指标预测配置不存在 config_id：%s" % config_id)

        points = self.tsp_result_model.get_result_by_id(tsp_config.get('metric_id'), config_id, metric_instance_id,
                                                        start_ts, end_ts)

        data = {
            'size': len(points),
            'points': points
        }
        return data
