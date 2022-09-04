# coding: utf-8
# pylint: disable=invalid-sequence-index

import logging
# import uuid
import threading

import web

tlocal = threading.local()
from teslafaas.common.request_id_gen import gen_request_id


def get_current_request_id():
    return tlocal.request_id


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        if 'environ' in web.ctx:
            if not hasattr(tlocal, 'request_id'):
                # tlocal.request_id = uuid.uuid4().hex
                tlocal.request_id = gen_request_id(web.ctx['environ']['REMOTE_ADDR'])
            record.requestid = "%s" % tlocal.request_id
        else:
            record.requestid = "None"
        return True
