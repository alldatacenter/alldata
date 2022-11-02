"""stomp.py provides connectivity to a message broker supporting the STOMP protocol.
Protocol versions 1.0, 1.1 and 1.2 are supported.

See the GITHUB project page for more information.

Author: Jason R Briggs
License: http://www.apache.org/licenses/LICENSE-2.0
Project Page: https://github.com/jasonrbriggs/stomp.py

"""

import ambari_stomp.connect as connect
import ambari_stomp.listener as listener

__version__ = (4, 1, 17)

##
# Alias for STOMP 1.0 connections.
#
Connection10 = connect.StompConnection10
StompConnection10 = Connection10

##
# Alias for STOMP 1.1 connections.
#
Connection11 = connect.StompConnection11
StompConnection11 = Connection11

##
# Alias for STOMP 1.2 connections.
#
Connection12 = connect.StompConnection12
StompConnection12 = Connection12

##
# Default connection alias (STOMP 1.1).
#
Connection = connect.StompConnection11

##
# Access to the default connection listener.
#
ConnectionListener = listener.ConnectionListener

##
# Access to the stats listener.
#
StatsListener = listener.StatsListener

##
# Access to the 'waiting' listener.
WaitingListener = listener.WaitingListener

##
# Access to the printing listener
PrintingListener = listener.PrintingListener
