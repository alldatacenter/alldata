#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class MetricModel(db.Model):
    __bind_key__ = 'pmdb'
    __tablename__ = 'metric'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(128), nullable=False)
    type = db.Column(db.String(64), nullable=False)
    index_path = db.Column(db.String(256), nullable=False)
    tags = db.Column(db.String(2048))
    entity = db.Column(db.String(64), nullable=False)
    source_type = db.Column(db.String(64), nullable=False)
    source_id = db.Column(db.Integer, nullable=False)
    source_table = db.Column(db.String(128), nullable=False)
    team_id = db.Column(db.Integer, default=0)
    app_id = db.Column(db.Integer, default=0)
    creator = db.Column(db.String(64), default='')
    owners = db.Column(db.String(1024), default='')
    description = db.Column(db.Text, default='')

    def to_json(self):
        return AlchemyDecoder.decode(self)
