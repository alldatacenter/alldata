#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import time


class SREWorksBaseResult(object):
    def __init__(self, code=200, request_id=None, message=None, data=None):
        self.code = code
        self.data = data
        self.message = message
        self.request_id = request_id
        self.timestamp = int(time.time() * 1000)

    def result_dict(self):
        return {
            "code": self.code,
            "requestId": self.request_id,
            "message": self.message,
            "timestamp": self.timestamp,
            "data": self.data,
        }