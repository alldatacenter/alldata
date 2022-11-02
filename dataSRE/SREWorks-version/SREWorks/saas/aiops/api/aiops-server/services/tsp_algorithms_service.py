#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from services.base_service import BaseService
from models.tsp_algorithms_model import TSPAlgorithmsModel


class TSPAlgorithmsService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_tsp_algorithm(self, algorithm_id):
        tsp_algorithm = TSPAlgorithmsModel.query.filter_by(id=algorithm_id).one_or_none()
        if tsp_algorithm:
            return tsp_algorithm.to_json()
        else:
            return None
