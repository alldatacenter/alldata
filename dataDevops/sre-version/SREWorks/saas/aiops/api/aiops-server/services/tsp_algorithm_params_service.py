#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from services.base_service import BaseService
from models.tsp_algorithm_params_model import TSPAlgorithmParamsModel


class TSPAlgorithmParamsService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_tsp_algorithm_param(self, param_id):
        tsp_algorithm_param = TSPAlgorithmParamsModel.query.filter_by(id=param_id).one_or_none()
        if tsp_algorithm_param:
            return tsp_algorithm_param.to_json()
        else:
            return None
