#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging

from services.base_service import BaseService
from models.metric_instance_model import MetricInstance


class MetricInstanceService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_instance_by_id(self, instance_id):
        """
        通过id查询指标实例
        :return: metric_instance_model
        """
        metric_instance = MetricInstance.query.filter_by(id=instance_id).one_or_none()
        if metric_instance:
            return metric_instance.to_json()
        else:
            return None

    def get_instance_by_metric(self, metric_id):
        """
        通过指标查询指标实例
        :return: metric_instance_model
        """
        result_list = MetricInstance.query.filter_by(metric_id=metric_id).all()
        if result_list:
            metric_instance_list = []
            for result in result_list:
                metric_instance_list.append(result.to_json())
            return metric_instance_list
        else:
            return None
