#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from models.db_session import db
from models.alchemy_decoder import AlchemyDecoder


class DatasourceModel(db.Model):
    __bind_key__ = 'pmdb'
    __tablename__ = 'datasource'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(128), nullable=False)
    type = db.Column(db.String(64), nullable=False)
    endpoint = db.Column(db.String(128), nullable=False)
    port = db.Column(db.Integer, nullable=False)
    access_key = db.Column(db.String(128))
    secret_key = db.Column(db.String(128))
    source_table = db.Column(db.String(128), nullable=False)
    team_id = db.Column(db.Integer, default=0)
    app_id = db.Column(db.Integer, default=0)
    creator = db.Column(db.String(64))
    owners = db.Column(db.String(1024))
    description = db.Column(db.Text)

    def to_json(self):
        return AlchemyDecoder.decode(self)
