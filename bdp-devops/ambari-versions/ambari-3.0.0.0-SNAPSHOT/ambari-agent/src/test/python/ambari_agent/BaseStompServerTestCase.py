#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import json
import ambari_stomp
import os
import sys
import time
import unittest
import logging
import socket
import select
import threading

try:
    from queue import Queue, Empty
except ImportError:
    from Queue import Queue, Empty

from coilmq.util.frames import Frame, FrameBuffer
from coilmq.queue import QueueManager
from coilmq.topic import TopicManager
from coilmq.util import frames
from coilmq.server.socket_server import StompServer, StompRequestHandler, ThreadedStompServer
from coilmq.store.memory import MemoryQueue
from coilmq.scheduler import FavorReliableSubscriberScheduler, RandomQueueScheduler
from coilmq.protocol import STOMP10

logger = logging.getLogger(__name__)

class BaseStompServerTestCase(unittest.TestCase):
  """
  Base class for test cases provides the fixtures for setting up the multi-threaded
  unit test infrastructure.
  We use a combination of C{threading.Event} and C{Queue.Queue} objects to faciliate
  inter-thread communication and lock-stepping the assertions.
  """

  def setUp(self):
    self.clients = []
    self.server = None  # This gets set in the server thread.
    self.server_address = None  # This gets set in the server thread.
    self.ready_event = threading.Event()

    addr_bound = threading.Event()
    self.init_stdout_logger()

    def start_server():
      self.server = TestStompServer(('127.0.0.1', 21613),
                                    ready_event=self.ready_event,
                                    authenticator=None,
                                    queue_manager=self._queuemanager(),
                                    topic_manager=self._topicmanager())
      self.server_address = self.server.socket.getsockname()
      addr_bound.set()
      self.server.serve_forever()

    self.server_thread = threading.Thread(
        target=start_server, name='server')
    self.server_thread.start()
    self.ready_event.wait()
    addr_bound.wait()

  def _queuemanager(self):
    """
    Returns the configured L{QueueManager} instance to use.
    Can be overridden by subclasses that wish to change out any queue mgr parameters.
    @rtype: L{QueueManager}
    """
    return QueueManager(store=MemoryQueue(),
                        subscriber_scheduler=FavorReliableSubscriberScheduler(),
                        queue_scheduler=RandomQueueScheduler())

  def _topicmanager(self):
    """
    Returns the configured L{TopicManager} instance to use.
    Can be overridden by subclasses that wish to change out any topic mgr parameters.
    @rtype: L{TopicManager}
    """
    return TopicManager()

  def tearDown(self):
    for c in self.clients:
      c.close()
    self.server.server_close()
    self.server_thread.join()
    self.ready_event.clear()
    del self.server_thread

  def _new_client(self, connect=True):
    """
    Get a new L{TestStompClient} connected to our test server.
    The client will also be registered for close in the tearDown method.
    @param connect: Whether to issue the CONNECT command.
    @type connect: C{bool}
    @rtype: L{TestStompClient}
    """
    client = TestStompClient(self.server_address)
    self.clients.append(client)
    if connect:
      client.connect()
      res = client.received_frames.get(timeout=1)
      self.assertEqual(res.cmd, frames.CONNECTED)
    return client

  def get_json(self, filename):
    filepath = os.path.join(os.path.abspath(os.path.dirname(__file__)), "dummy_files", "stomp", filename)

    with open(filepath) as fp:
      return fp.read()

  def get_dict_from_file(self, filename):
    filepath = os.path.join(os.path.abspath(os.path.dirname(__file__)), "dummy_files", "stomp", filename)

    with open(filepath) as fp:
      return json.load(fp)

  def init_stdout_logger(self):
    format='%(levelname)s %(asctime)s - %(message)s'

    logger = logging.getLogger()
    logger.setLevel(logging.INFO)
    formatter = logging.Formatter(format)
    chout = logging.StreamHandler(sys.stdout)
    chout.setLevel(logging.INFO)
    chout.setFormatter(formatter)
    cherr = logging.StreamHandler(sys.stderr)
    cherr.setLevel(logging.ERROR)
    cherr.setFormatter(formatter)
    logger.handlers = []
    logger.addHandler(cherr)
    logger.addHandler(chout)

    logging.getLogger('stomp.py').setLevel(logging.WARN)
    logging.getLogger('coilmq').setLevel(logging.INFO)
    logging.getLogger('ambari_agent.apscheduler').setLevel(logging.WARN)
    logging.getLogger('apscheduler').setLevel(logging.WARN)
    logging.getLogger('ambari_agent.alerts').setLevel(logging.WARN)
    logging.getLogger('alerts').setLevel(logging.WARN)
    logging.getLogger('ambari_agent.AlertSchedulerHandler').setLevel(logging.WARN)


  def remove_files(self, filepathes):
    for filepath in filepathes:
      if os.path.isfile(filepath):
        os.remove(filepath)

  def assert_with_retries(self, func, tries, try_sleep):
    # wait for 2 seconds
    for i in range(tries):
      try:
        func()
        break
      except AssertionError:
        time.sleep(try_sleep)
    else:
      func()

  def assertDictEqual(self, d1, d2):
    try:
      super(BaseStompServerTestCase, self).assertDictEqual(d1, d2)
    except AttributeError:
      super(BaseStompServerTestCase, self).assertEqual(d1, d2) # Python 2.6 compatibility



class TestStompServer(ThreadedStompServer):
    """
    A stomp server for functional tests that uses C{threading.Event} objects
    to ensure that it stays in sync with the test suite.
    """

    allow_reuse_address = True

    def __init__(self, server_address,
                 ready_event=None,
                 authenticator=None,
                 queue_manager=None,
                 topic_manager=None):
        self.ready_event = ready_event
        StompServer.__init__(self, server_address, StompRequestHandler,
                             authenticator=authenticator,
                             queue_manager=queue_manager,
                             topic_manager=topic_manager,
                             protocol=STOMP10)

    def server_activate(self):
        self.ready_event.set()
        StompServer.server_activate(self)


class TestStompClient(object):
    """
    A stomp client for use in testing.
    This client spawns a listener thread and pushes anything that comes in onto the
    read_frames queue.
    @ivar received_frames: A queue of Frame instances that have been received.
    @type received_frames: C{Queue.Queue} containing any received C{stompclient.frame.Frame}
    """

    def __init__(self, addr, connect=True):
        """
        @param addr: The (host,port) tuple for connection.
        @type addr: C{tuple}
        @param connect: Whether to connect socket to specified addr.
        @type connect: C{bool}
        """
        self.log = logging.getLogger('%s.%s' % (
            self.__module__, self.__class__.__name__))
        self.sock = None
        self.addr = addr
        self.received_frames = Queue()
        self.read_stopped = threading.Event()
        self.buffer = FrameBuffer()
        if connect:
            self._connect()

    def connect(self, headers=None):
        self.send_frame(Frame(frames.CONNECT, headers=headers))

    def send(self, destination, message, set_content_length=True, extra_headers=None):
        headers = extra_headers or {}
        headers['destination'] = destination
        if set_content_length:
            headers['content-length'] = len(message)
        self.send_frame(Frame('send', headers=headers, body=message))

    def subscribe(self, destination):
        self.send_frame(Frame('subscribe', headers={
                        'destination': destination}))

    def send_frame(self, frame):
        """
        Sends a stomp frame.
        @param frame: The stomp frame to send.
        @type frame: L{stompclient.frame.Frame}
        """
        if not self.connected:
            raise RuntimeError("Not connected")
        self.sock.send(frame.pack())

    def _connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect(self.addr)
        self.connected = True
        self.read_stopped.clear()
        t = threading.Thread(target=self._read_loop,
                             name="client-receiver-%s" % hex(id(self)))
        t.start()

    def _read_loop(self):
        while self.connected:
            r, w, e = select.select([self.sock], [], [], 0.1)
            if r:
                data = self.sock.recv(1024)
                self.buffer.append(data)
                for frame in self.buffer:
                    self.log.debug("Processing frame: %s" % frame)
                    self.received_frames.put(frame)
        self.read_stopped.set()
        # print "Read loop has been quit! for %s" % id(self)

    def disconnect(self):
        self.send_frame(Frame('disconnect'))

    def close(self):
        if not self.connected:
            raise RuntimeError("Not connected")
        self.connected = False
        self.read_stopped.wait(timeout=0.5)
        self.sock.close()

class TestCaseTcpConnection(ambari_stomp.Connection):
  def __init__(self, url):
    self.lock = threading.RLock()
    self.correlation_id = -1
    ambari_stomp.Connection.__init__(self, host_and_ports=[('127.0.0.1', 21613)])

  def send(self, destination, message, content_type=None, headers=None, **keyword_headers):
    with self.lock:
      self.correlation_id += 1

    logger.info("Event to server at {0} (correlation_id={1}): {2}".format(destination, self.correlation_id, message))

    body = json.dumps(message)
    ambari_stomp.Connection.send(self, destination, body, content_type=content_type, headers=headers, correlationId=self.correlation_id, **keyword_headers)
    return self.correlation_id

  def add_listener(self, listener):
    self.set_listener(listener.__class__.__name__, listener)

from ambari_agent import security
security.AmbariStompConnection = TestCaseTcpConnection