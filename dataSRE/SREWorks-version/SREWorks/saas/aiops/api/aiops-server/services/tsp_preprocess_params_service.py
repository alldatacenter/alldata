#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from services.base_service import BaseService
from models.tsp_preprocess_params_model import TSPPreprocessParamsModel


class TSPAPreprocessParamsService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_tsp_preprocess_param(self, param_id):
        tsp_preprocess_param = TSPPreprocessParamsModel.query.filter_by(id=param_id).one_or_none()
        if tsp_preprocess_param:
            return tsp_preprocess_param.to_json()
        else:
            return None
