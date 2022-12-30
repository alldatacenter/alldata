#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging

from services.base_service import BaseService
from models.metric_model import MetricModel


class MetricService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_metric_by_id(self, metric_id):
        """
        通过id查询metric
        :return: metric_model
        """
        metric = MetricModel.query.filter_by(id=metric_id).one_or_none()
        if metric:
            return metric.to_json()
        else:
            return None

    def exist_metric(self, metric_id):
        metric = MetricModel.query.filter_by(id=metric_id).one_or_none()
        if metric:
            return True
        else:
            return False

    def not_exist_metric(self, metric_id):
        exist = self.exist_metric(metric_id)
        return not exist
