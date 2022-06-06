"""Networking functions to support backwards compatibility.

Distinct from the backward(2/3) functions to handle ipv6 changes between Python versions 2.5 and 2.6.
"""

import sys

if sys.hexversion < 0x02060000:  # < Python 2.6
    from ambari_stomp.backwardsock25 import *
else:  # Python 2.6 onwards
    from ambari_stomp.backwardsock26 import *
