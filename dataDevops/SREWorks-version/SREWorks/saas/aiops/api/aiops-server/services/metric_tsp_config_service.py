#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging

from services.base_service import BaseService
from services.metric_service import MetricService
from services.metric_instance_service import MetricInstanceService
from services.tsp_algorithms_service import TSPAlgorithmsService
from services.tsp_algorithm_params_service import TSPAlgorithmParamsService
from services.tsp_preprocess_params_service import TSPAPreprocessParamsService
from models.metric_tsp_config_model import MetricTspConfigModel
from models.tsp_result_model import TSPResultModel


class MetricTSPConfigService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)
        self.metric_service = MetricService()
        self.metric_meta_service = MetricInstanceService()
        self.algorithm_service = TSPAlgorithmsService()
        self.algorithm_params_service = TSPAlgorithmParamsService()
        self.preprocess_params_service = TSPAPreprocessParamsService()
        self.tsp_result_model = TSPResultModel()

    def add_metric_tsp_config(self, title, metric_id, algorithm_id, algorithm_param_id, preprocess_param_id,
                              min_train_hours, default_train_hours, series_interval, horizon, trigger_interval, enable,
                              owners, creator, description):
        """
        新增指标时序预测任务
        :return: 任务ID
        """

        if self.metric_service.not_exist_metric(metric_id):
            raise ValueError("指标不存在 metric_id：%s" % metric_id)

        metric_tsp_task = MetricTspConfigModel(
            title=title,
            metric_id=metric_id,
            algorithm_id=algorithm_id,
            algorithm_param_id=algorithm_param_id,
            preprocess_param_id=preprocess_param_id,
            min_train_hours=min_train_hours,
            default_train_hours=default_train_hours,
            series_interval=series_interval,
            horizon=horizon,
            trigger_interval=trigger_interval,
            enable=enable,
            owners=owners,
            creator=creator,
            description=description
        )

        try:
            self.db_session.add(metric_tsp_task)
            self.db_session.flush()
            config_id = metric_tsp_task.id
            self.logger.info(config_id)
            self.db_session.commit()
        except Exception as ex:
            self.db_session.rollback()
            self.logger.error(ex)
            raise ex
        finally:
            self.db_session.close()

        return config_id

    def get_config_by_id(self, config_id):
        metric_tsp_config = MetricTspConfigModel.query.filter_by(id=config_id).one_or_none()
        if metric_tsp_config:
            return metric_tsp_config.to_json()
        else:
            return None

    def get_config_by_metric(self, metric_id):
        result_list = MetricTspConfigModel.query.filter_by(metric_id=metric_id).all()
        if result_list:
            metric_tsp_config_list = []
            for result in result_list:
                metric_tsp_config_list.append(result.to_json())

            return metric_tsp_config_list
        else:
            return None
