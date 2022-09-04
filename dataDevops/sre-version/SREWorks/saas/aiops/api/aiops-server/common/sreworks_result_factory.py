#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

from common.sreworks_base_result import SREWorksBaseResult


class SREWorksResultFactory(object):
    def build_success_result(self, request_id=None, message=None, data=None):
        return self.build_result(200, request_id, message, data)

    def build_failed_result(self, request_id=None, message=None):
        return self.build_result(400, request_id, message)

    def build_server_error_result(self, request_id=None, message=None):
        return self.build_result(500, request_id, message)

    def build_result(self, code, request_id=None, message=None, data=None):
        result = SREWorksBaseResult(code, request_id, message, data)
        return result.result_dict()
