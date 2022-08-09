import sys

"""Functions to support backwards compatibility.

Basically where we have functions which differ between python 2 and 3, we provide implementations here
and then Python-specific versions in backward2 and backward3.
"""

if sys.hexversion >= 0x03000000:  # Python 3+
    from ambari_stomp.backward3 import *
else:  # Python 2
    from ambari_stomp.backward2 import *


def get_errno(e):
    """
    Return the errno of an exception, or the first argument if errno is not available.

    :param Exception e: the exception object
    """
    try:
        return e.errno
    except AttributeError:
        return e.args[0]


try:
    from time import monotonic
except ImportError:  # Python < 3.3/3.5
    from time import time as monotonic
