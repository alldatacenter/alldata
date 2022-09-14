#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class TSPAlgorithmParamsModel(db.Model):
    __tablename__ = 'time_series_prediction_algorithm_params'

    id = db.Column(db.Integer, primary_key=True)
    tsp_algorithm_id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(255), nullable=False)
    value = db.Column(db.String(1024), nullable=False)

    def to_json(self):
        return AlchemyDecoder.decode(self)
