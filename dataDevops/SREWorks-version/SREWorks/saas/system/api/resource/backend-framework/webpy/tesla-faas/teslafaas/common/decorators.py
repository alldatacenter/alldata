# coding: utf-8
"""
Base Decorators
"""
import logging
import traceback

from teslafaas.common.jsonify import jsondumps
from teslafaas.common.exceptions import TeslaBaseException

logger = logging.getLogger(__name__)


def exception_wrapper(func):
    def __decorator(*args, **kwargs):
        try:
            ret = func(*args, **kwargs)

        except Exception as e:
            ret = {}

            trace_msg = traceback.format_exc()
            logger.error(trace_msg)

            if isinstance(e, TeslaBaseException):
                ret['code'] = e.get_ret_code()
                ret['message'] = e.get_ret_message()
                ret['data'] = e.get_ret_data()
            else:
                error_mesg = "Server exception: %s" % e
                ret['message'] = error_mesg
                ret['code'] = 500
                ret['data'] = trace_msg

            ret = jsondumps(ret)

        return ret

    return __decorator
