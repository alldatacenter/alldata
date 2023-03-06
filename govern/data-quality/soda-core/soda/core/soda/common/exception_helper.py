import sys
import traceback
from typing import Optional


def get_exception_stacktrace(exception) -> Optional[str]:
    if isinstance(exception, BaseException):
        if sys.version_info < (3, 10):
            return "".join(
                traceback.format_exception(etype=type(exception), value=exception, tb=exception.__traceback__)
            )
        return "".join(traceback.format_exception(exception))
    return None
