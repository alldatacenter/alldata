#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class MetricTspConfigModel(db.Model):
    __tablename__ = 'metric_time_series_prediction_config'

    id = db.Column(db.BigInteger, primary_key=True, autoincrement=True)
    title = db.Column(db.String(128), nullable=False)
    metric_id = db.Column(db.String(128), primary_key=True)
    algorithm_id = db.Column(db.Integer, nullable=False)
    algorithm_param_id = db.Column(db.String(32), nullable=True)
    preprocess_param_id = db.Column(db.String(32), nullable=True)
    min_train_hours = db.Column(db.Integer, nullable=False, default=24)
    default_train_hours = db.Column(db.Integer, nullable=False, default=168)
    series_interval = db.Column(db.Integer, nullable=False, default=60)
    horizon = db.Column(db.BigInteger, nullable=False, default=10800)
    trigger_interval = db.Column(db.Integer, nullable=False, default=900)
    enable = db.Column(db.Boolean, nullable=False, default=1)
    owners = db.Column(db.String(128), default='')
    creator = db.Column(db.String(32), default='')
    description = db.Column(db.Text, default='')

    def to_json(self):
        return AlchemyDecoder.decode(self)
