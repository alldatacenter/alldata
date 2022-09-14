#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class TSPAlgorithmsModel(db.Model):
    __tablename__ = 'time_series_prediction_algorithms'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), nullable=False)

    def to_json(self):
        return AlchemyDecoder.decode(self)
