#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging

from services.base_service import BaseService
from models.datasource_model import DatasourceModel


class DatasourceService(BaseService):
    def __init__(self):
        BaseService.__init__(self)
        self.logger = logging.getLogger(__name__)

    def get_datasource_by_id(self, metric_id):
        """
        通过id查询datasource
        :return: datasource_model
        """
        datasource = DatasourceModel.query.filter_by(id=metric_id).one_or_none()
        if datasource:
            return datasource.to_json()
        else:
            return None

    def exist_datasource(self, datasource_id):
        datasource = DatasourceModel.query.filter_by(id=datasource_id).one_or_none()
        if datasource:
            return True
        else:
            return False

    def not_exist_datasource(self, datasource_id):
        exist = self.exist_datasource(datasource_id)
        return not exist
