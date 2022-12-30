#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import json
from json.decoder import JSONDecodeError
from sqlalchemy.ext.declarative import DeclarativeMeta


class AlchemyDecoder(object):

    @staticmethod
    def decode(obj):
        fields = {}
        if isinstance(obj.__class__, DeclarativeMeta):
            fields = {}
            for field in [x for x in dir(obj) if not (x.startswith('_') or x in ['metadata', 'registry', 'to_json', 'query', 'query_class'])]:
                data = obj.__getattribute__(field)
                try:
                    fields[field] = json.loads(data)
                except (JSONDecodeError, TypeError):
                    fields[field] = data
                except Exception:
                    fields[field] = None
        return fields

