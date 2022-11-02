#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'sreworks'

import logging
from flask import request
from common import trace_id_generator


class TraceIdFilter(logging.Filter):
    def filter(self, record):
        try:
            trace_id = request.environ.get("HTTP_TRACE_ID", "None")
            if trace_id == "None":
                trace_id = trace_id_generator.generate_trace_id(request.environ.get("REMOTE_ADDR"))
            user = request.environ.get("HTTP_X_AUTH_USER", "None")
        except Exception:
            trace_id = "None"
            user = "None"
        finally:
            record.trace_id = trace_id
            record.user = user
        return True
