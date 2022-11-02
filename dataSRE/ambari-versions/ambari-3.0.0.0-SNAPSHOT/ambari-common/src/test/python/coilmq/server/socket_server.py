"""
The default/recommended SocketServer-based server implementation. 
"""
import logging
import socket
import threading
import Queue
try:
    from socketserver import BaseRequestHandler, TCPServer, ThreadingMixIn
except ImportError:
    from SocketServer import BaseRequestHandler, TCPServer, ThreadingMixIn


from coilmq.util.frames import FrameBuffer
from coilmq.server import StompConnection
from coilmq.engine import StompEngine
from coilmq.exception import ClientDisconnected

__authors__ = ['"Hans Lellelid" <hans@xmpl.org>']
__copyright__ = "Copyright 2009 Hans Lellelid"
__license__ = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""


class StompRequestHandler(BaseRequestHandler, StompConnection):
    """
    Class that will be instantiated to handle STOMP connections.

    This class will be instantiated once per connection to the server.  In a multi-threaded
    context, that means that instances of this class are scoped to a single thread.  It should
    be noted that while the L{coilmq.engine.StompEngine} instance will be thread-local, the 
    storage containers configured into the engine are not thread-local (and hence must be
    thread-safe). 

    @ivar buffer: A StompBuffer instance which buffers received data (to ensure we deal with
                    complete STOMP messages.
    @type buffer: C{stompclient.util.FrameBuffer}

    @ivar engine: The STOMP protocol engine.
    @type engine: L{coilmq.engine.StompEngine}

    @ivar debug: Whether to enable extra-verbose debug logging.  (Will be logged at debug level.)
    @type debug: C{bool}
    """

    def setup(self):
        if self.server.timeout is not None:
            self.request.settimeout(self.server.timeout)
        self.debug = False
        self.log = logging.getLogger('%s.%s' % (self.__module__, self.__class__.__name__))
        self.buffer = FrameBuffer()
        self.engine = StompEngine(connection=self,
                                  authenticator=self.server.authenticator,
                                  queue_manager=self.server.queue_manager,
                                  topic_manager=self.server.topic_manager,
                                  protocol=self.server.protocol)

    def handle(self):
        """
        Handle a new socket connection.
        """
        # self.request is the TCP socket connected to the client
        try:
            while not self.server._shutdown_request_event.is_set():
                try:
                    data = self.request.recv(8192)
                    if not data:
                        break
                    if self.debug:
                        self.log.debug("RECV: %r" % data)
                    self.buffer.append(data)

                    for frame in self.buffer:
                        self.server.frames_queue.put(frame)
                        self.log.debug("Processing frame: %s" % frame)
                        self.engine.process_frame(frame)
                        if not self.engine.connected:
                            raise ClientDisconnected()
                except socket.timeout:  # pragma: no cover
                    pass
        except ClientDisconnected:
            self.log.debug("Client disconnected, discontinuing read loop.")
        except Exception as e:  # pragma: no cover
            self.log.error("Error receiving data (unbinding): %s" % e)
            self.engine.unbind()
            raise

    def finish(self):
        """
        Normal (non-error) termination of request.

        Unbinds the engine.
        @see: L{coilmq.engine.StompEngine.unbind}
        """
        self.engine.unbind()

    def send_frame(self, frame):
        """ Sends a frame to connected socket client.

        @param frame: The frame to send.
        @type frame: C{stompclient.frame.Frame}
        """
        packed = frame.pack()
        if self.debug:  # pragma: no cover
            self.log.debug("SEND: %r" % packed)
        self.request.sendall(packed)


class StompServer(TCPServer):
    """
    Subclass of C{StompServer.TCPServer} to handle new connections with 
    instances of L{StompRequestHandler}.

    @ivar authenticator: The authenticator to use.
    @type authenticator: L{coilmq.auth.Authenticator}

    @ivar queue_manager: The queue manager to use.
    @type queue_manager: L{coilmq.queue.QueueManager}

    @ivar topic_manager: The topic manager to use.
    @type topic_manager: L{coilmq.topic.TopicManager}
    """

    # This causes the SO_REUSEADDR option to be set on the socket, allowing
    # server to rebind to the same address (w/o waiting for connections to
    # leave TIME_WAIT after unclean disconnect).
    allow_reuse_address = True

    def __init__(self, server_address, RequestHandlerClass=None, timeout=3.0,
                 authenticator=None, queue_manager=None, topic_manager=None, protocol=None):
        """
        Extension to C{TCPServer} constructor to provide mechanism for providing implementation classes.

        @param server_address: The (address,port) C{tuple}
        @param RequestHandlerClass: The class to use for handling requests.
        @param timeout: The timeout for the underlying socket.
        @keyword authenticator: The configure L{coilmq.auth.Authenticator} object to use.
        @keyword queue_manager: The configured L{coilmq.queue.QueueManager} object to use.
        @keyword topic_manager: The configured L{coilmq.topic.TopicManager} object to use. 
        """
        self.log = logging.getLogger('%s.%s' % (
            self.__module__, self.__class__.__name__))
        if not RequestHandlerClass:
            RequestHandlerClass = StompRequestHandler
        self.timeout = timeout
        self.authenticator = authenticator
        self.queue_manager = queue_manager
        self.topic_manager = topic_manager
        self.protocol = protocol
        self.frames_queue = Queue.Queue()
        self._serving_event = threading.Event()
        self._shutdown_request_event = threading.Event()
        TCPServer.__init__(self, server_address, RequestHandlerClass)

    def server_close(self):
        """
        Closes the socket server and any associated resources.
        """
        self.log.debug("Closing the socket server connection.")
        TCPServer.server_close(self)
        self.queue_manager.close()
        self.topic_manager.close()
        if hasattr(self.authenticator, 'close'):
            self.authenticator.close()
        self.shutdown()

    def shutdown(self):
        if self._serving_event.is_set():
            self._shutdown_request_event.set()
            self._serving_event.clear()
            TCPServer.shutdown(self)

    def serve_forever(self, poll_interval=0.5):
        """Handle one request at a time until shutdown.

        Polls for shutdown every poll_interval seconds. Ignores
        self.timeout. If you need to do periodic tasks, do them in
        another thread.
        """
        self._serving_event.set()
        self._shutdown_request_event.clear()
        TCPServer.serve_forever(self, poll_interval=poll_interval)


class ThreadedStompServer(ThreadingMixIn, StompServer):
    pass
