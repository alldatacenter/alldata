#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'

import logging
import web
from teslafaas.common.trace_id import get_upstream_trace


class TraceIdFilter(logging.Filter):
    def filter(self, record):
        req_env = web.ctx.get('env', {})
        trace_id = get_upstream_trace()
        user = req_env.get("HTTP_X_AUTH_USER", "None")
        record.trace_id = trace_id
        record.user = user
        return True
