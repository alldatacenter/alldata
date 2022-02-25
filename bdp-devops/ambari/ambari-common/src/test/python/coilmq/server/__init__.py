"""
Package of available server implementations and shared functionality/interfaces.

CoilMQ is designed for the Python StompServer reference socket server (specifically 
multi-threaded); however, some alternative implementation examples are also provided. 
"""
import abc

__authors__ = ['"Hans Lellelid" <hans@xmpl.org>']
__license__ = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""


class StompConnection(object):
    """
    An "interface" for server implementation classes to "implement". 

    This class serves primarily as a means to document the API that CoilMQ will expect
    the connection object to implement.

    @ivar reliable_subscriber: Whether this client will ACK all messages.
    @type reliable_subscriber: C{bool}
    """
    __metaclass__ = abc.ABCMeta

    reliable_subscriber = False

    @abc.abstractmethod
    def send_frame(self, frame):
        """
        Uses this connection implementation to send the specified frame to a connected client.

        @param frame: The STOMP frame to send.
        @type frame: C{stompclient.frame.Frame}  
        """
