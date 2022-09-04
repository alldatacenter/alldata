#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import time
import logging
from flask import request

from common.var_type import VarType
from handlers.base_view import BaseView
from services.metric_tsp_config_service import MetricTSPConfigService
from services.metric_tsp_task_service import MetricTSPTaskService


class TSPView(BaseView):
    def __init__(self, exec_func):
        BaseView.__init__(self, exec_func)
        self.logger = logging.getLogger(__name__)
        self.metric_tsp_config_service = MetricTSPConfigService()
        self.metric_tsp_task_service = MetricTSPTaskService()

    def get_tsp_config_by_id(self):
        self.logger.info(request.args)
        params = request.args
        config_id = params.get('config_id', None)
        tsp_config = self.metric_tsp_config_service.get_config_by_id(config_id)
        print(tsp_config)
        return self.result_factory.build_success_result(message='ok', data=tsp_config)

    def get_tsp_config_by_metric(self):
        self.logger.info(request.args)
        params = request.args
        metric_id = params.get('metric_id', None)
        tsp_config = self.metric_tsp_config_service.get_config_by_metric(metric_id)
        return self.result_factory.build_success_result(message='ok', data=tsp_config)

    def create_tsp_config(self):
        self.logger.info(request.json)
        params = request.json
        title = params.get('title', None)
        metric_id = params.get('metric_id', None)
        algorithm_id = 2
        algorithm_param_id = 2
        preprocess_param_id = 1
        min_train_hours = params.get('min_train_hours', 3)
        default_train_hours = params.get('default_train_hours', 168)
        series_interval = params.get('series_interval', 60)
        horizon = params.get('horizon', 10800)
        trigger_interval = params.get('trigger_interval', 900)
        enable = params.get('enable', True)
        owners = params.get('owners', '')
        creator = params.get('creator', '')
        description = params.get('description', None)

        try:
            title = self._check_param('title', title, VarType.STR, False)
            metric_id = self._check_param('metric_id', metric_id, VarType.STR, False)
            min_train_hours = self._check_param('min_train_hours', min_train_hours, VarType.INT, False, True)
            default_train_hours = self._check_param('default_train_hours', default_train_hours, VarType.INT, False, True)
            series_interval = self._check_param('series_interval', series_interval, VarType.INT, False, True)
            horizon = self._check_param('horizon', horizon, VarType.INT, False, True)
            trigger_interval = self._check_param('series_interval', trigger_interval, VarType.INT, False, True)
            enable = self._check_param('enable', enable, VarType.BOOL)
        except ValueError as ex:
            self.logger.error(ex)
            return self.result_factory.build_failed_result(str(ex))

        try:
            config_id = self.metric_tsp_config_service.add_metric_tsp_config(
                title, metric_id, algorithm_id, algorithm_param_id, preprocess_param_id, min_train_hours,
                default_train_hours, series_interval, horizon, trigger_interval, enable, owners, creator, description)
            return self.result_factory.build_success_result(message='ok', data={'config_id': config_id})
        except Exception as ex:
            return self.result_factory.build_failed_result(message=str(ex))

    def submit_tsp_task(self):
        self.logger.info(request.json)
        params = request.json
        config_id = params.get('config_id', None)

        try:
            config_id = self._check_param('config_id', config_id, VarType.INT)
        except ValueError as ex:
            self.logger.error(ex)
            return self.result_factory.build_failed_result(str(ex))

        try:
            tsp_result = self.metric_tsp_task_service.train_tsp_task(config_id)
            return self.result_factory.build_success_result(message='ok', data=tsp_result)
        except Exception as ex:
            self.logger.error(ex)
            return self.result_factory.build_failed_result(str(ex))

    def get_tsp_result(self):
        self.logger.info(request.args)
        params = request.args
        config_id = params.get('config_id', None)
        instance_id = params.get('metric_instance_id', None)
        start_ts = params.get('start_ts', None)
        end_ts = params.get('end_ts', None)

        try:
            config_id = self._check_param('config_id', config_id, VarType.INT)
            instance_id = self._check_param('instance_id', instance_id, VarType.STR, allow_empty=False)
            start_ts = self._check_param('start_ts', start_ts, VarType.INT, True)
            if not start_ts:
                start_ts = int(time.time())

            end_ts = self._check_param('end_ts', end_ts, VarType.INT, True)
            if not end_ts:
                end_ts = int(time.time()) + 3 * 60 * 60
        except ValueError as ex:
            self.logger.error(ex)
            return self.result_factory.build_failed_result(str(ex))

        try:
            tsp_result = self.metric_tsp_task_service.get_tsp_result(config_id, instance_id, start_ts, end_ts)
            return self.result_factory.build_success_result(message='ok', data=tsp_result)
        except Exception as ex:
            self.logger.error(ex)
            return self.result_factory.build_failed_result(str(ex))
