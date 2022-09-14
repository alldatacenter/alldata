#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class MetricInstance(db.Model):
    __bind_key__ = 'pmdb'
    __tablename__ = 'metric_instance'

    id = db.Column(db.String(128), primary_key=True)
    name = db.Column(db.String(128), nullable=False)
    metric_id = db.Column(db.Integer, nullable=False)
    metric_name = db.Column(db.String(128), nullable=False)
    index_path = db.Column(db.String(256), nullable=False)
    index_tags = db.Column(db.String(2048))
    description = db.Column(db.Text)

    def to_json(self):
        return AlchemyDecoder.decode(self)
